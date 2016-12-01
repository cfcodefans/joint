package org.joints.web.mvc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

public class ControllerHelper {

	private static final String JOINT_JERSEY_MVC_BASE	= "joint.jersey.mvc.base";
	private static final String JERSEY_CONTROLLER	= "JerseyController";

	public static String getBasePath(final HttpServletRequest _req) {
		final String basePathParam = _req.getServletContext().getServletRegistration(JERSEY_CONTROLLER).getInitParameter(JOINT_JERSEY_MVC_BASE);
		return StringUtils.defaultIfBlank(basePathParam, "/");
	}

	@Deprecated
	public static String joinPaths(final String... paths) {
		if (ArrayUtils.isEmpty(paths)) {
			return StringUtils.EMPTY;
		}

		final StringBuilder sb = new StringBuilder(0);

		Stream.of(paths).filter(StringUtils::isNotBlank).forEach(path -> {
			path = path.trim();
			path = StringUtils.strip(path, "/");
			path = StringUtils.stripEnd(path, "/");
			sb.append('/');
			sb.append(path);
		});

		sb.deleteCharAt(0);

		return sb.toString();
	}
}
