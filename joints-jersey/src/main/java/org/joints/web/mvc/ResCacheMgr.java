package org.joints.web.mvc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@WebListener
public class ResCacheMgr implements ServletContextListener, Runnable {

    public static class CachedEntry<E> {
        public final Date catchedDate;
        public final E content;

        public CachedEntry(E _entry) {
            catchedDate = new Date();
            this.content = _entry;
        }

        @Override
        public String toString() {
            return "CachedEntry [catchedDate=" + catchedDate + ", content=" + content + "]";
        }
    }

    public static final Cache<String, CachedEntry<?>> cache = CacheBuilder.newBuilder().build();

    private static final Logger log = LogManager.getLogger(ResCacheMgr.class);

    private String realPathStr = null;

    private static ServletContext servletCtx = null;

    private ScheduledExecutorService worker = null;

    @Override
    public void contextDestroyed(ServletContextEvent ce) {
        worker.shutdownNow();
        cache.cleanUp();
    }

    @Override
    public void contextInitialized(ServletContextEvent ce) {
        servletCtx = ce.getServletContext();
        realPathStr = servletCtx.getRealPath(".");
        log.info("getRealPath(): " + realPathStr);

        log.info("start a thread to validate the cache entry");

        worker = Executors.newScheduledThreadPool(1);
        worker.scheduleAtFixedRate(this, 10, 5, TimeUnit.SECONDS);
    }

    private boolean validate(final String pathStr) {
        if (StringUtils.isBlank(pathStr)) {
            return false;
        }

        String absoluteResPathStr = realPathStr + pathStr;

        try {
            URL resUrl = servletCtx.getResource(pathStr);
            if (resUrl == null) {
                log.warn(String.format("resource is invalid at path: %s", absoluteResPathStr));
                return false;
            }
        } catch (MalformedURLException e) {
            log.error("invalid resource path", e);
            return false;
        }

        Path path = Paths.get(absoluteResPathStr);
        if (!Files.isReadable(path)) {
            log.warn(String.format("resource is not readable at path: %s", absoluteResPathStr));
            return false;
        }

        CachedEntry<?> entry = cache.getIfPresent(pathStr);
        if (entry == null) {
            log.warn(String.format("resource is null at path: %s", absoluteResPathStr));
            return false;
        }

        try {
            if (Files.getLastModifiedTime(path).toMillis() > entry.catchedDate.getTime()) {
                return false;
            }
        } catch (IOException e) {
            log.error(String.format("failed to get last modified time of resource at path: %s", absoluteResPathStr), e);
            return false;
        }

        return true;
    }

    public void run() {
        // valid the cache entry
        log.debug("validate the cache entry");
        if (StringUtils.isBlank(realPathStr)) {
            log.error("path of web application is blank");
            return;
        }

        if (cache.size() == 0) {
            log.debug("cacheMap is empty now!");
            return;
        }

        final List<String> invalidPathList = cache.asMap().keySet().stream().filter(pathStr -> !(Thread.interrupted() || validate(pathStr))).collect(Collectors.toList());

        invalidPathList.forEach(invalidPathStr -> {
            cache.invalidate(invalidPathStr);
            log.warn(String.format("resource path: %s is invalid, evicted from %s", invalidPathStr, ResCacheMgr.class.getSimpleName()));
        });
    }

    public static String getTextResource(final String refPath, final String path) {
        return getTextResource(servletCtx, refPath, path);
    }

    public static String getTextResource(final ServletContext rootCtx, final String refPath, final String path) {
        if (StringUtils.isBlank(path)) {
            return StringUtils.EMPTY;
        }

        if (URI.create(path).isAbsolute()) {
            log.error("not running external scripts on server side!\n\t" + path);
            return StringUtils.EMPTY;
        }

        String resPath = getAbsoluteResPath(rootCtx, refPath, path);

        log.info("load Script: " + resPath);
        final InputStream resIS = rootCtx.getResourceAsStream(resPath);
        try {
            return (resIS != null) ? IOUtils.toString(resIS) : StringUtils.EMPTY;
        } catch (final IOException e) {
            log.error("failed to load script from path: " + path, e);
        }
        return StringUtils.EMPTY;
    }

    public static String getAbsoluteResPath(final String refPath, final String path) {
        return getAbsoluteResPath(servletCtx, refPath, path);
    }

    public static String getAbsoluteResPath(final ServletContext rootCtx, final String refPath, final String path) {
        final String rootPath = rootCtx.getContextPath();
        String resPath = path.trim();
        if (resPath.startsWith(rootPath)) {
            return StringUtils.substringAfter(resPath, rootPath);
        }
        return FilenameUtils.separatorsToUnix(FilenameUtils.concat(FilenameUtils.getPath(refPath), path));
    }

    public static String getRealResPath(final String path) {
        return servletCtx.getRealPath(path);
    }
}
