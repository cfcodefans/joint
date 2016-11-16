package org.joints.commons;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MiscUtils {
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	public static String toCharset(final String str, final String charset) {
		try {
			return (str == null || StringUtils.isBlank(charset)) ? str : new String(str.getBytes(), charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return str;
		}
	}

	public static String byteCountToDisplaySize(long size) {
		String displaySize;
		if (size / 1073741824L > 0L) {
			displaySize = String.valueOf(size / 1073741824L) + " GB";
		} else {
			if (size / 1048576L > 0L) {
				displaySize = String.valueOf(size / 1048576L) + " MB";
			} else {
				if (size / 1024L > 0L)
					displaySize = String.valueOf(size / 1024L) + " KB";
				else
					displaySize = String.valueOf(size) + " bytes";
			}
		}
		return displaySize;
	}

//	public static String getMD5EncodedText(String plainText) {
//		String encodedPassword = null;
//		try {
//			MessageDigest md = MessageDigest.getInstance("MD5");
//			md.update(plainText.getBytes());
//			encodedPassword = new String(Hex.encodeHex(md.digest()));
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//		return encodedPassword;
//	}

	public static long getProcessId() {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');
		String pidStr = jvmName.substring(0, index);

		if (index < 1 || !NumberUtils.isCreatable(pidStr)) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return 0;
		}

		return Long.parseLong(pidStr);
	}

	public static String invocationInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String invocationInfo(final int i) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Map map(Object... keyAndVals) {
		return MapUtils.putAll(new HashMap(), keyAndVals);
	}

	public static boolean sleep(int i, TimeUnit seconds) {
		try {
			Thread.sleep(seconds.toMillis(i));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Thread.interrupted();
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
			e.printStackTrace();
		}

		return null;
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
			e.printStackTrace();
		}
		return sw.toString();
	}

	private static int next;

	public static long uuid() {
		next = ++next % 1000000;
		return (System.currentTimeMillis() / 1000) * 100000000 + Thread.currentThread().getId() * 1000000 + next;
	}

	public static String invocInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s.%s", StringUtils.substringAfterLast(ste[i].getClassName(), "."), ste[i].getMethodName());
	}

	public static boolean interrupted(String msg) throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException(msg);
		}
		return false;
	}

	public static boolean interrupted() throws InterruptedException {
		return interrupted(Thread.currentThread().getName() + " is interrupted!");
	}

	public static class ExByteArrayInputStream extends ByteArrayInputStream {
		public ExByteArrayInputStream(byte[] buf) {
			super(buf);
		}

		public ExByteArrayInputStream(int bufSize) {
			super(new byte[bufSize]);
		}

		public byte[] getBuf() {
			return super.buf;
		}
	}
}
