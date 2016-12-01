package org.joints.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.event.EventUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MiscUtils {
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	public static long getPropertyNumber(String name, long defaultValule) {
		String str = System.getProperty(name);
		if (StringUtils.isNumeric(str)) {
			return Long.parseLong(str);
		}
		return defaultValule;
	}

	public static String invocationInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String invocInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), StringUtils.substringAfterLast(ste[i].getClassName(), "."), ste[i].getMethodName());
	}

	public static String invocationInfo(final int i) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String byteCountToDisplaySize(long size) {
		if (size / 1073741824L > 0L) {
			return String.valueOf(size / 1073741824L) + " GB";
		}

		if (size / 1048576L > 0L) {
			return String.valueOf(size / 1048576L) + " MB";
		}

		if (size / 1024L > 0L)
			return String.valueOf(size / 1024L) + " KB";

		return String.valueOf(size) + " bytes";
	}

	public static long getProcessId() {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');
		String pidStr = jvmName.substring(0, index);

		if (index < 1 || !NumberUtils.isNumber(pidStr)) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return 0;
		}

		return Long.parseLong(pidStr);
	}

	public static class LoopingArrayIterator<E> extends ObjectArrayIterator<E> {
		@SafeVarargs
		public LoopingArrayIterator(final E... array) {
			super(array, 0, array.length);
		}

		public LoopingArrayIterator(final E array[], final int start) {
			super(array, start, array.length);
		}

		public E loop() {
			final E[] array = this.getArray();
			loopIdx.compareAndSet(array.length, 0);
			return array[loopIdx.getAndIncrement() % array.length];
		}

		private AtomicInteger loopIdx = new AtomicInteger();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Map map(Object... keyAndVals) {
		return MapUtils.putAll(new HashMap(), keyAndVals);
	}

	public static long HOST_HASH = System.currentTimeMillis();

	static {
		try {
			HOST_HASH = InetAddress.getLocalHost().getHostAddress().hashCode();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	// private static long IDX = 0;

	public static long uniqueLong() {
		return Math.abs(UUID.randomUUID().hashCode());
	}

	public static ThreadFactory namedThreadFactory(final String name) {
		return new BasicThreadFactory.Builder().namingPattern(name + "_%d").build();
	}

	public static String lineNumber(final String str) {
		if (str == null) {
			return null;
		}

		final StringReader sr = new StringReader(str);
		final BufferedReader br = new BufferedReader(sr);

		final StringBuilder sb = new StringBuilder(0);
		AtomicLong lineNumber = new AtomicLong(0);
		br.lines().forEach(line -> sb.append(lineNumber.incrementAndGet()).append("\t").append(line).append('\n'));

		return sb.toString();
	}

	private static final Log log = LogFactory.getLog(EventUtils.class.getName());

	public static NameValuePair[] getParamPairs(Map<String, ?> paramMap) {
		return paramMap.entrySet().stream().map(en -> new BasicNameValuePair(en.getKey(), String.valueOf(en.getValue()))).toArray(NameValuePair[]::new);
	}

	public static String mapToJson(Map<String, String> paramMap) {
		if (MapUtils.isEmpty(paramMap)) {
			return "{}";
		}

		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return paramMap.toString();
		}
	}

	public static String toXML(final Object bean) {
		final StringWriter sw = new StringWriter();

		try {
			JAXBContext jc = JAXBContext.newInstance(bean.getClass());
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FRAGMENT, true);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			// marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			m.marshal(bean, sw);
		} catch (Exception e) {
			log.error(bean, e);
		}
		return sw.toString();
	}

	public static <T> T toObj(final String xmlStr, final Class<T> cls) {
		if (StringUtils.isBlank(xmlStr) || cls == null) {
			return null;
		}

		try {
			JAXBContext jc = JAXBContext.newInstance(cls);
			Unmarshaller um = jc.createUnmarshaller();
			return um.unmarshal(new StreamSource(new StringReader(xmlStr)), cls).getValue();
		} catch (JAXBException e) {
			log.error(xmlStr, e);
		}

		return null;
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

	public static Map<String, String> extractParams(Map<String, String[]> params) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		params.keySet().forEach(key -> {
			final String[] vals = params.get(key);
			paramsMap.put(key, ArrayUtils.isEmpty(vals) ? null : vals[0]);
		});
		return paramsMap;
	}

	public static String generate(final String text) {
		final StringBuffer sb = new StringBuffer();
		try {
			final byte[] intext = text.getBytes();
			final MessageDigest md5 = MessageDigest.getInstance("MD5");
			final byte[] md5rslt = md5.digest(intext);
			for (int i = 0; i < md5rslt.length; i++) {
				final int val = 0xff & md5rslt[i];
				if (val < 16) {
					sb.append("0");
				}
				sb.append(Integer.toHexString(val));
			}
		} catch (final Exception e) {
			log.error(e, e);
		}
		return sb.toString();
	}

	public static String loadResAsString(final Class<?> cls, final String fileName) {
		if (cls == null || StringUtils.isBlank(fileName)) {
			return StringUtils.EMPTY;
		}

		try {
			return IOUtils.toString(cls.getResourceAsStream(fileName));
		} catch (IOException e) {
			log.error("", e);
		}
		return StringUtils.EMPTY;
	}

	public static String getEncodedText(String plainText) {
		String encodedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText.getBytes());
			encodedPassword = new String(Hex.encodeHex(md.digest()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return encodedPassword;
	}

	public static void writeToMappedFile(File file, byte[] data) throws IOException {
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
			mbb.put(data);
			mbb.force();
		}
	}

	public static byte[] readFromMappedFile(File file) throws IOException {
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			long length = file.length();
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, length);
			if (!mbb.load().isLoaded()) {
				throw new IllegalStateException(String.format("%s can not be loaded", file.getAbsolutePath()));
			}

			byte[] data = new byte[(int) length];
			mbb.get(data);
			return data;
		}
	}
}
