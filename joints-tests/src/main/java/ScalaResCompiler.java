import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.model.Resource;
import org.joints.commons.Lambdas;
import org.joints.commons.MiscUtils;
import org.joints.commons.ScriptUtils;
import org.joints.rest.script.ScriptResLoader;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.mutable.Buffer;
import scala.reflect.internal.util.AbstractFileClassLoader;
import scala.reflect.internal.util.BatchSourceFile;
import scala.reflect.internal.util.SourceFile;
import scala.reflect.io.Path;
import scala.reflect.io.PlainFile;
import scala.tools.nsc.Global;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.Scripted;

import javax.script.ScriptEngine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by fan on 2016/11/29.
 */
public class ScalaResCompiler extends ScriptResLoader {

    private static final Logger log = LogManager.getLogger(ScalaResCompiler.class);

    public FileFilter getFileFilter() {
        return new OrFileFilter(Arrays.asList(new WildcardFileFilter("*.scala")));
    }

    protected Set<Resource> executeScriptFile(File file) {
        log.info("loading rest jersey resource from {}", file);
        String resClzName = FilenameUtils.getBaseName(file.getName());
        if (StringUtils.isBlank(resClzName)) {
            log.error("scala file name is invalid {}", file);
            return Collections.emptySet();
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String scriptStr = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
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
            Scripted sed = (Scripted) se;// Scripted.apply(Scripted.apply$default$1(), Scripted.apply$default$2(), new PrintWriter(baos));
            IMain iMain = sed.intp();

            iMain.resetClassLoader();
            if (cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) cl;
                List<URL> clzPathList = Arrays.asList(ucl.getURLs()).stream()
                    .filter(Lambdas.wpf(url -> Paths.get(url.toURI()).toFile().isFile()))
                    .collect(Collectors.toList());
                iMain.addUrlsToClassPath(JavaConverters.<URL>asScalaBuffer(clzPathList));
            }

            List<SourceFile> batchSourceFiles = Arrays.asList(new BatchSourceFile(new PlainFile(new Path(file)), scriptStr.toCharArray()));
            Buffer<SourceFile> batchSourceFileBuf = JavaConverters.<SourceFile>asScalaBuffer(batchSourceFiles);
            Tuple2<Object, Global.Run> compileResult = iMain.compileSourcesKeepingRun(batchSourceFileBuf.toSeq());
            Global.Run run = compileResult._2();
            String compileInfo = baos.toString(Charset.defaultCharset().name());
            log.info(compileInfo);

            if (Boolean.FALSE.equals(compileResult._1())) {
                log.error("failed to compile {} for \n {} code: \n {}", file, compileInfo, scriptStr);
                return Collections.emptySet();
            }

            AbstractFileClassLoader scalaCompilerClassLoader = iMain.classLoader();
            Class<?> resCls = scalaCompilerClassLoader.loadClass(resClzName);

            return castToResources(resCls);
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
}
