package org.joints.commons

import java.io.{FileFilter, IOException}
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.{Collections, Objects, Observable, List => JList, Map => JMap, Set => JSet}

import org.apache.commons.collections4.CollectionUtils
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.JavaConverters._

/**
  * Created by fan on 2016/11/25.
  */
object FileMonitor {
    private val log: Logger = LogManager.getLogger(classOf[FileMonitor])

    def castEvent(event: Object): JMap[WatchEvent.Kind[_], JSet[Path]] = event.asInstanceOf[JMap[WatchEvent.Kind[_], JSet[Path]]]

    private[commons] def cast[T](event: WatchEvent[_]): WatchEvent[T] = event.asInstanceOf[WatchEvent[T]]
}

class FileMonitor(val _start: String, val _filter: FileFilter) extends Observable with Runnable with AutoCloseable {
    protected var ws: WatchService = null
    protected var keys: JMap[WatchKey, Path] = null
    protected var start: String = null
    protected var filter: FileFilter = null

    keys = new java.util.HashMap[WatchKey, Path]
    start = _start
    filter = _filter
    try {
        ws = FileSystems.getDefault.newWatchService
        regForWatch(Paths.get(start))
    } catch {
        case e: IOException =>
            FileMonitor.log.error(String.format("failed to monitor path at %s", start), e)
    }

    @throws[IOException]
    def regAllForWatch(_start: Path): Unit = Files.walkFileTree(_start, new SimpleFileVisitor[Path]() {
        @throws[IOException]
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
            regForWatch(dir)
            FileVisitResult.CONTINUE
        }
    })

    @throws[IOException]
    def regForWatch(dir: Path): Unit = {
        if (!Files.isDirectory(dir)) {
            FileMonitor.log.warn("can not watch a file: {}", dir)
            return
        }
        val regKey: WatchKey = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        keys.put(regKey, dir)
    }

    private def wrap(wk: WatchKey, watchEvents: JList[WatchEvent[Path]]): JMap[WatchEvent.Kind[Path], JSet[Path]] = {
        val dir: Path = keys.get(wk)
        if (dir == null) {
            FileMonitor.log.error("WatchKey not recognized! " + wk.watchable)
            return Collections.emptyMap[WatchEvent.Kind[Path], JSet[Path]]
        }

        return watchEvents.asScala
            .groupBy(we => we.kind())
            .mapValues(wes => wes.map(_.context()).toSet.asJava)
            .asJava
    }

    override def run(): Unit = while (!Thread.interrupted) {
        // wait for key to be signalled
        var wk: WatchKey = null
        try {
            wk = ws.take
            FileMonitor.log.info(wk)
        } catch {
            case x: InterruptedException =>
                return
        }

        val dir: Path = keys.get(wk)
        if (dir != null) {
            val watchEvents: JList[WatchEvent[_]] = wk.pollEvents
            updateListeners(wk, watchEvents)
            for (event <- watchEvents.asScala.filterNot(_.kind() eq OVERFLOW)) {
                val kind: WatchEvent.Kind[_] = event.kind
                //TBD - provide example of how  OVERFLOW event is handled
                // Context for directory entry event is the file name of entry
                val ev: WatchEvent[Path] = FileMonitor.cast(event)
                val name: Path = ev.context
                val child: Path = dir.resolve(name)
                //print out event
                FileMonitor.log.info("{}: {}\n", event.kind.name, child)

                if (kind eq ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS))
                            regAllForWatch(child)
                        else regForWatch(child)
                    } catch {
                        case x: IOException =>
                        // ignore to keep sample readbale
                    }
                }
            }
            if (!wk.reset) {
                keys.remove(wk)
                if (keys.isEmpty) return
            }
        } else
            FileMonitor.log.error("WatchKey not recognized! " + wk.watchable)
    }

    protected def updateListeners(wk: WatchKey, _watchEvents: JList[WatchEvent[_]]): Unit = {
        val dir: Path = keys.get(wk)
        if (dir == null || CollectionUtils.isEmpty(_watchEvents)) return
        val watchEvents: JList[WatchEvent[Path]] = _watchEvents.asScala.map((_we: WatchEvent[_]) => {
            val ev: WatchEvent[Path] = FileMonitor.cast(_we)
            val name: Path = ev.context
            val child: Path = dir.resolve(name)
            if (filter.accept(child.toFile)) ev else null
        }).filter(Objects.nonNull(_)).asJava

        if (CollectionUtils.isEmpty(watchEvents)) return

        super.setChanged()
        super.notifyObservers(wrap(wk, watchEvents))
    }

    @throws[Exception]
    override def close(): Unit = if (ws != null) ws.close()
}