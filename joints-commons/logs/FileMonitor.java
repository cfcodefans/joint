package org.joints.commons;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by fan on 2016/11/25.
 */
public class FileMonitor extends Observable implements Runnable, AutoCloseable {
    private static final Logger log = LogManager.getLogger(FileMonitor.class);
    protected WatchService ws = null;
    protected Map<WatchKey, Path> keys;
    protected String start = null;
    protected FileFilter filter = null;

    public FileMonitor(String _start, FileFilter _filter) {
        keys = new HashMap<WatchKey, Path>();
        start = _start;
        filter = _filter;
        try {
            ws = FileSystems.getDefault().newWatchService();
            regForWatch(Paths.get(start));
        } catch (IOException e) {
            log.error(String.format("failed to monitor path at %s", start), e);
        }
    }

    public void regAllForWatch(Path _start) throws IOException {
        Files.walkFileTree(_start, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                regForWatch(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void regForWatch(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            log.warn("can not watch a file: {}", dir);
            return;
        }
        WatchKey regKey = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(regKey, dir);
    }

    private Map<WatchEvent.Kind, Set<Path>> wrap(WatchKey wk, List<WatchEvent<?>> watchEvents) {
        Path dir = keys.get(wk);
        if (dir == null) {
            log.error("WatchKey not recognized! " + wk.watchable());
            return Collections.emptyMap();
        }
        return MiscUtils$.map(StandardWatchEventKinds.ENTRY_DELETE,
            watchEvents.stream().filter(we -> ENTRY_DELETE.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()),
            StandardWatchEventKinds.ENTRY_CREATE,
            watchEvents.stream().filter(we -> ENTRY_CREATE.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()),
            StandardWatchEventKinds.ENTRY_MODIFY,
            watchEvents.stream().filter(we -> ENTRY_MODIFY.equals(we.kind())).map(we -> (Path) we.context()).map(dir::resolve).collect(Collectors.toSet()));
    }

    public static Map<WatchEvent.Kind, Set<Path>> castEvent(Object event) {
        return (Map<WatchEvent.Kind, Set<Path>>) event;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            // wait for key to be signalled
            WatchKey wk;
            try {
                wk = ws.take();
                log.info(wk);
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(wk);
            if (dir == null) {
                log.error("WatchKey not recognized! " + wk.watchable());
                continue;
            }

            List<WatchEvent<?>> watchEvents = wk.pollEvents();
            updateListeners(wk, watchEvents);

            for (WatchEvent<?> event : watchEvents) {
                WatchEvent.Kind kind = event.kind();
                //TBD - provide example of how  OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                //print out event
                log.info("{}: {}\n", event.kind().name(), child);

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            regAllForWatch(child);
                        } else {
                            regForWatch(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }
            if (!wk.reset()) {
                keys.remove(wk);
                if (keys.isEmpty()) {
                    return;
                }
            }
        }
    }

    protected void updateListeners(WatchKey wk, List<WatchEvent<?>> _watchEvents) {
        Path dir = keys.get(wk);
        if (dir == null || CollectionUtils.isEmpty(_watchEvents)) {
            return;
        }

        List<WatchEvent<?>> watchEvents = _watchEvents.stream()
            .map(_we -> {
                WatchEvent<Path> ev = cast(_we);
                Path name = ev.context();
                Path child = dir.resolve(name);
                if (filter.accept(child.toFile())) {
                    return ev;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(watchEvents)) {
            return;
        }

        super.setChanged();
        super.notifyObservers(wrap(wk, watchEvents));
    }

    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    @Override
    public void close() throws Exception {
        if (ws != null) ws.close();
    }
}
