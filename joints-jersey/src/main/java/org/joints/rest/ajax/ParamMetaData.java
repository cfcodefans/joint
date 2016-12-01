package org.joints.rest.ajax;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameter.Source;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.lang.reflect.Type;

@XmlRootElement
@SuppressWarnings("rawtypes")
public class ParamMetaData implements Serializable {

	private static final long serialVersionUID = 1L;
	public Source source;
	public String sourceName;
	public Class rawType;
	public Type type;

	public static ParamMetaData build(Parameter param) {
		ParamMetaData pmd = new ParamMetaData();
		
		pmd.source = param.getSource();
		pmd.sourceName = param.getSourceName();
		pmd.rawType = param.getRawType();
		pmd.type = param.getType();
		
		return pmd;
	}

	@Override
	public String toString() {
		return "ParamMetaData [source=" + source + ", sourceName=" + sourceName + ", rawType=" + rawType + ", type=" + type + "]";
	}
	
}
