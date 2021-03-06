package org.joints.rest.script;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.joints.commons.FileMonitor;
import org.joints.commons.MiscUtils;
import org.joints.commons.ProcTrace;
import org.joints.commons.ScriptUtils;
import org.joints.rest.ajax.AjaxResContext;
import org.joints.web.mvc.ResCacheMgr;

import javax.script.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;


public class ScriptResLoader extends ResourceConfig {

    public static final String SCRIPT_PATH = "script-path";
    private static final Logger log = LogManager.getLogger(ScriptResLoader.class);
    private static ScriptResLoader instance = null;
    protected Container container = null;
    private ResourceConfig resHolder = this;
    private AtomicBoolean startedWatch = new AtomicBoolean(false);
    private ExecutorService watchThread = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("script-folder-watcher"));
    private Map<Path, Set<Resource>> scriptPathAndResources = new HashMap<>();

    public ScriptResLoader() {
        ProcTrace.start(MiscUtils.invocationInfo());
        ProcTrace.ongoing("set packages scan");

        register(JacksonFeature.class);
        register(EncodingFilter.class);
        register(GZipEncoder.class);
        register(DeflateEncoder.class);
        register(new ContainerListener());

        ProcTrace.end();
        log.info(ProcTrace.flush());

        instance = this;

        String[] pathStrs = getScriptPaths();
        final String realResPath = ResCacheMgr.getRealResPath("WEB-INF/" + pathStrs[0]);
        Stream.of(pathStrs)
            .map(pathStr -> realResPath)
            .map(Paths::get)
            .filter(Files::exists)
            .map(Path::toFile)
            .filter(File::isDirectory)
            .map(dir -> dir.listFiles(getFileFilter()))
            .flatMap(Stream::of)
            .forEach(file -> scriptPathAndResources.put(file.toPath(), executeScriptFile(file)));

        Set<Resource> resSet = scriptPathAndResources.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

        prepareResourceConfig(resSet, this);

        startWatchScriptFolder(realResPath);
    }

    protected Object tryEvalForResources(ScriptEngine se, String scriptStr) throws ScriptException {
        Object evaluated = null;
        if (se instanceof Compilable) {
            Compilable cpl = (Compilable) se;
            evaluated = cpl.compile(scriptStr).eval();
            if (evaluated != null)
                return evaluated;
        }

        if (se instanceof Invocable) {
            Invocable inv = (Invocable) se;
            evaluated = inv.getInterface(ScriptResLoader.IResourceGenerator.class);
            if (evaluated != null)
                return evaluated;
        }
        evaluated = se.eval(scriptStr);
        return evaluated;
    }

    protected Set<Resource> castToResources(Object evaluated) {
        if (evaluated instanceof Class) {
            Class clz = (Class) evaluated;
            Resource res = Resource.from(clz);
            return new HashSet<Resource>(Arrays.asList(res));
        }

        if (evaluated instanceof Resource) {
            Resource res = (Resource) evaluated;
            return new HashSet<Resource>(Arrays.asList(res));
        }

        if (evaluated instanceof Set) {
            Set set = (Set) evaluated;
            if (CollectionUtils.isEmpty(set)) {
                return Collections.emptySet();
            }

            return (Set<Resource>) set.stream().filter(Objects::nonNull).map(obj -> {
                if (obj instanceof Resource) return (Resource) obj;
                if (obj instanceof Class) return Resource.from((Class) obj);
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }

        if (evaluated instanceof IResourceGenerator) {
            IResourceGenerator resGen = (IResourceGenerator) evaluated;
            return resGen.apply(null);
        }
        return Collections.emptySet();
    }

    public FileFilter getFileFilter() {
        return new OrFileFilter(Arrays.asList(
            new WildcardFileFilter("*.js")
//            ,new WildcardFileFilter("*.scala")
        ));
    }

    public void generateAjaxMetadata(Set<Resource> resSet) {
        AjaxResContext ajaxResContext = AjaxResContext.getInstance(this.getApplicationName());
        ajaxResContext.getProxyList().clear();
        ajaxResContext.build(resSet);
    }

    private void startWatchScriptFolder(final String startPath) {
        if (!startedWatch.compareAndSet(false, true)) return;
        watchThread.submit(() -> {
            try (FileMonitor fm = new FileMonitor(startPath, getFileFilter())) {
                fm.addObserver(ScriptResLoader.this::onFileChange);
                fm.run();
            } catch (Exception e) {
                log.error("something wrong with file watcher", e);
            }
        });
    }

    public String[] getScriptPaths() {
        String pathProperty = Objects.toString(this.getConfiguration().getProperty(SCRIPT_PATH), "scripts").trim();
        return Stream.of(StringUtils.split(pathProperty, ",")).map(String::trim).toArray(String[]::new);
    }

    protected Set<Resource> executeScriptFile(File file) {
        log.info("loading rest jersey resource from {}", file);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        String scriptStr = null;
        try {
            scriptStr = FileUtils.readFileToString(file, Charset.defaultCharset());
            if (StringUtils.isBlank(scriptStr)) {
                log.error("scriptStr: {} is blank", scriptStr);
                return Collections.emptySet();
            }

            String extStr = FilenameUtils.getExtension(file.getName());
            ScriptEngine se = ScriptUtils.getScriptEngineByMimeType(extStr);
            if (se == null) {
                log.error("invalid script file extension: {}", file);
                return Collections.emptySet();
            }
            se.getBindings(ScriptContext.ENGINE_SCOPE).put("current_path", file.getParentFile().getAbsolutePath());
            return castToResources(tryEvalForResources(se, scriptStr));
        } catch (IOException e) {
            log.error("fail to execute script file: ", e);
        } catch (Exception e) {
            log.info(MiscUtils.lineNumber(scriptStr));
            log.error(String.format("failed to execute script: \n\t %s \n\t", MiscUtils.lineNumber(scriptStr)), e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        return Collections.emptySet();
    }

    private void onFileChange(Observable fm, Object _watchEvents) {
        Map<WatchEvent.Kind, Set<Path>> eventAndPaths = FileMonitor.castEvent(_watchEvents);
        eventAndPaths.get(ENTRY_DELETE).stream()
            .map(scriptPathAndResources::remove)
            .flatMap(Set::stream).collect(Collectors.toSet());

        Set<Path> createdPaths = eventAndPaths.get(ENTRY_CREATE);
        Set<Path> modifiedPaths = eventAndPaths.get(ENTRY_MODIFY);
        modifiedPaths.addAll(createdPaths);

        for (Path path : modifiedPaths) {
            Set<Resource> newResources = executeScriptFile(path.toFile());
            if (CollectionUtils.isNotEmpty(newResources))
                scriptPathAndResources.put(path, newResources);
            else
                scriptPathAndResources.remove(path);
        }

        Set<Resource> resSet = scriptPathAndResources.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        this.resHolder = new ResourceConfig();
        prepareResourceConfig(resSet, resHolder);

        container.reload(resHolder);
    }

    protected void prepareResourceConfig(Set<Resource> resSet, ResourceConfig resourceConfig) {
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(EncodingFilter.class);
        resourceConfig.register(GZipEncoder.class);
        resourceConfig.register(DeflateEncoder.class);

        resourceConfig.setApplicationName(this.getApplicationName());
        resourceConfig.setClassLoader(this.getClassLoader());

        resourceConfig.registerResources(resSet);
        generateAjaxMetadata(resSet);
    }

    public interface IResourceGenerator {
        Set<Resource> apply(Application app);
    }

    private static class ContainerListener extends AbstractContainerLifecycleListener {
        @Override
        public void onStartup(Container container) {
            log.info("ScriptResLoader.container = {}", container);
            instance.container = container;
            instance.container.getConfiguration().getProperties().get(SCRIPT_PATH);
        }

        @Override
        public void onReload(Container container) {
        }

        @Override
        public void onShutdown(Container container) {
        }
    }

    private static class AppHandlerListener implements ApplicationEventListener {
        @Override
        public void onEvent(ApplicationEvent event) {
        }

        @Override
        public RequestEventListener onRequest(RequestEvent requestEvent) {
            return null;
        }
    }

    private static class ResDelegator implements Inflector<ContainerRequestContext, Response> {
        @Override
        public Response apply(ContainerRequestContext containerRequestContext) {
            return null;
        }
    }
}
