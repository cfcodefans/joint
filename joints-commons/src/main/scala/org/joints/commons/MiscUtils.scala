package org.joints.commons

import java.io._
import java.lang.management.ManagementFactory
import java.lang.reflect.{ParameterizedType, Type}
import java.net.{InetAddress, UnknownHostException}
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util
import java.util.UUID
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong
import javax.xml.bind.{JAXBContext, JAXBException, Marshaller, Unmarshaller}
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.binary.Hex
import org.apache.commons.collections4.MapUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.apache.commons.lang3.event.EventUtils
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.logging.{Log, LogFactory}
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.message.BasicNameValuePair

import scala.collection.JavaConverters._

object MiscUtils {
    val AVAILABLE_PROCESSORS: Int = Runtime.getRuntime.availableProcessors

    def getPropertyNumber(name: String, defaultValue: Long): Long = {
        val str: String = System.getProperty(name)
        if (StringUtils.isNumeric(str)) return str.toLong
        defaultValue
    }

    def invocationInfo: String = {
        val stes: Array[StackTraceElement] = Thread.currentThread.getStackTrace
        val ste: StackTraceElement = stes(2)
        s"${ste.getFileName}\t${ste.getClassName}.${ste.getMethodName}"
    }

    def invocInfo: String = {
        val stes: Array[StackTraceElement] = Thread.currentThread.getStackTrace
        val ste: StackTraceElement = stes(2)
        s"${ste.getFileName}\t${StringUtils.substringAfterLast(ste.getClassName, ".")}.${ste.getMethodName}"
    }

    def invocationInfo(i: Int): String = {
        val stes: Array[StackTraceElement] = Thread.currentThread.getStackTrace
        val ste: StackTraceElement = stes(i)
        s"${ste.getFileName}\t${ste.getClassName}.${ste.getMethodName}"
    }

    def byteCountToDisplaySize(size: Long): String = {
        if (size / 1073741824L > 0L) return String.valueOf(size / 1073741824L) + " GB"
        if (size / 1048576L > 0L) return String.valueOf(size / 1048576L) + " MB"
        if (size / 1024L > 0L) return String.valueOf(size / 1024L) + " KB"
        String.valueOf(size) + " bytes"
    }

    def getProcessId: Long = { // Note: may fail in some JVM implementations
        // therefore fallback has to be provided
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        val jvmName: String = ManagementFactory.getRuntimeMXBean.getName
        val index: Int = jvmName.indexOf('@')
        val pidStr: String = jvmName.substring(0, index)
        if (index < 1 || !NumberUtils.isNumber(pidStr)) { // part before '@' empty (index = 0) / '@' not found (index = -1)
            return 0
        }
        pidStr.toLong
    }

    def map(keyAndVals: Object*): util.Map[_, _] = MapUtils.putAll(new util.HashMap[_, _], keyAndVals.toArray)

    var HOST_HASH: Long = System.currentTimeMillis

    // private static long IDX = 0;
    def uniqueLong: Long = Math.abs(UUID.randomUUID.hashCode)

    def namedThreadFactory(name: String): ThreadFactory = new BasicThreadFactory.Builder().namingPattern(name + "_%d").build

    def lineNumber(str: String): String = {
        if (str == null) return null
        val sr: StringReader = new StringReader(str)
        val br: BufferedReader = new BufferedReader(sr)
        val sb: StringBuilder = new StringBuilder(0)
        val lineNumber: AtomicLong = new AtomicLong(0)
        br.lines.forEach((line: String) => sb.append(lineNumber.incrementAndGet).append("\t").append(line).append('\n'))
        sb.toString
    }

    private val log: Log = LogFactory.getLog(classOf[EventUtils].getName)

    def getParamPairs(paramMap: util.Map[String, _]): Array[NameValuePair] =
        paramMap.entrySet.asScala.map((en: util.Map.Entry[String, _]) => new BasicNameValuePair(en.getKey, String.valueOf(en.getValue))).toArray

    def mapToJson(paramMap: util.Map[String, String]): String = {
        if (MapUtils.isEmpty(paramMap)) return "{}"
        val mapper: ObjectMapper = new ObjectMapper
        try mapper.writeValueAsString(paramMap)
        catch {
            case e: Exception =>
                log.error(e.getMessage, e)
                paramMap.toString
        }
    }

    def toXML(bean: Any): String = {
        val sw: StringWriter = new StringWriter
        try {
            val jc: JAXBContext = JAXBContext.newInstance(bean.getClass)
            val m: Marshaller = jc.createMarshaller
            m.setProperty(Marshaller.JAXB_FRAGMENT, true)
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            // marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
            m.marshal(bean, sw)
        } catch {
            case e: Exception =>
                log.error(bean, e)
        }
        sw.toString
    }

    def toObj[T](xmlStr: String, cls: Class[T]): T = {
        if (StringUtils.isBlank(xmlStr) || cls == null) return null.asInstanceOf
        try {
            val jc: JAXBContext = JAXBContext.newInstance(cls)
            val um: Unmarshaller = jc.createUnmarshaller
            return um.unmarshal(new StreamSource(new StringReader(xmlStr)), cls).getValue
        } catch {
            case e: JAXBException =>
                log.error(xmlStr, e)
        }
        null.asInstanceOf
    }

    //    public static Map<String, String> extractParams(MultivaluedMap<String, String> params) {
    //        Map<String, String> paramsMap = new HashMap<String, String>();
    //        params.keySet().forEach(key -> paramsMap.put(key, params.getFirst(key)));
    //        return paramsMap;
    //    }
    //
    //    public static Map<String, String[]> toParamMap(MultivaluedMap<String, String> params) {
    //        Map<String, String[]> paramsMap = new HashMap<String, String[]>();
    //        params.keySet().forEach(key -> paramsMap.put(key, params.get(key).toArray(new String[0])));
    //        return paramsMap;
    //    }
    def extractParams(params: util.Map[String, Array[String]]): util.Map[String, String] = {
        params.asScala.mapValues(vals => if (vals.isEmpty) null else vals(0)).asJava
    }

    def generate(text: String): String = {
        val sb: StringBuffer = new StringBuffer
        try {
            val intext: Array[Byte] = text.getBytes
            val md5: MessageDigest = MessageDigest.getInstance("MD5")
            val md5rslt: Array[Byte] = md5.digest(intext)

            for (_b <- md5rslt) {
                val _val: Int = 0xff & _b
                if (_val < 16) sb.append("0")
                sb.append(Integer.toHexString(_val))
            }
        } catch {
            case e: Exception =>
                log.error(e, e)
        }
        sb.toString
    }

    def loadResAsString(cls: Class[_], fileName: String): String = {
        if (cls == null || StringUtils.isBlank(fileName)) return StringUtils.EMPTY
        try return IOUtils.toString(cls.getResourceAsStream(fileName))
        catch {
            case e: IOException => log.error("", e)
        }
        StringUtils.EMPTY
    }

    def getEncodedText(plainText: String): String = {
        var encodedPassword: String = null
        try {
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            md.update(plainText.getBytes)
            encodedPassword = new String(Hex.encodeHex(md.digest))
        } catch {
            case e: NoSuchAlgorithmException =>
                e.printStackTrace()
        }
        encodedPassword
    }

    @throws[IOException]
    def writeToMappedFile(file: File, data: Array[Byte]): Unit = try {
        val fc: FileChannel = FileChannel.open(file.toPath, StandardOpenOption.WRITE, StandardOpenOption.READ)
        try {
            val mbb: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, data.length)
            mbb.put(data)
            mbb.force
        } finally if (fc != null) fc.close()
    }

    @throws[IOException]
    def readFromMappedFile(file: File): Array[Byte] = try {
        val fc: FileChannel = FileChannel.open(file.toPath, StandardOpenOption.READ)
        try {
            val length: Long = file.length
            val mbb: MappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, length)
            if (!mbb.load.isLoaded) throw new IllegalStateException(s"${file.getAbsolutePath} can not be loaded")
            val data: Array[Byte] = new Array[Byte](length.toInt)
            mbb.get(data)
            data
        } finally if (fc != null) fc.close()
    }

    def getParameterizedClzz(_type: Type): Array[Class[_]] = {
        if (_type == null) return null
        if (_type.isInstanceOf[Class[_]]) {
            val aClass: Class[_] = _type.asInstanceOf[Class[_]]
            return Array[Class[_]](aClass)
        }
        if (_type.isInstanceOf[ParameterizedType]) {
            val pt: ParameterizedType = _type.asInstanceOf[ParameterizedType]
            pt.getActualTypeArguments.filter(_.isInstanceOf[Class])
        }
        null
    }

    try {
        HOST_HASH = InetAddress.getLocalHost.getHostAddress.hashCode
    } catch {
        case e: UnknownHostException => e.printStackTrace()
    }
}