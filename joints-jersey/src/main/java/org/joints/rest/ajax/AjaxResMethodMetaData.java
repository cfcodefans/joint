package org.joints.rest.ajax;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.research.ws.wadl.HTTPMethods;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethod.JaxrsType;
import org.joints.commons.MiscUtils;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@XmlRootElement
public class AjaxResMethodMetaData implements Serializable {

    private static final long serialVersionUID = 1L;

    public JaxrsType jaxrsType;
    public String name;
    @SuppressWarnings("rawtypes")
    public Class returnType;
    public HTTPMethods httpMethod;
    public List<ParamMetaData> params = new ArrayList<ParamMetaData>();
    public boolean isArray = false;

    public final List<String> produceMediaTypes = new ArrayList<String>();
    public final List<String> consumedMediaTypes = new ArrayList<String>();

    @JsonIgnore
    @XmlTransient
    public AjaxResMetadata parent;

    private String path = "";

    public synchronized String getPath() {
        if (StringUtils.isBlank(path)) {
            for (AjaxResMetadata _parent = this.parent; _parent != null; _parent = _parent.parent) {
                path = StringUtils.stripEnd(_parent.path, "/")
                    + (StringUtils.isNotBlank(path) ? "/" : "")
                    + StringUtils.stripStart(path, "/");
            }
        }
        return path;
    }

    public static AjaxResMethodMetaData build(ResourceMethod resMd) {
        if (resMd == null) {
            return null;
        }

        AjaxResMethodMetaData aResMd = new AjaxResMethodMetaData();

        String httpMethodStr = resMd.getHttpMethod().toUpperCase();
        if (!EnumUtils.isValidEnum(HTTPMethods.class, httpMethodStr)) {
            return null;
        }

        aResMd.httpMethod = HTTPMethods.valueOf(httpMethodStr);
        aResMd.name = resMd.getInvocable().getHandlingMethod().getName();
        aResMd.returnType = resMd.getInvocable().getRawResponseType();
        if (aResMd.returnType.isArray()) {
            aResMd.isArray = true;
            aResMd.returnType = aResMd.returnType.getComponentType();
        } else if (Collection.class.isAssignableFrom(aResMd.returnType)) {
            aResMd.isArray = true;
            Type genericReturnType = resMd.getInvocable().getDefinitionMethod().getGenericReturnType();
            Class[] parameterizedClzz = MiscUtils.getParameterizedClzz(genericReturnType);
            aResMd.returnType = parameterizedClzz[0];
        }

        aResMd.jaxrsType = resMd.getType();

        resMd.getProducedTypes().stream().map(MediaType::toString).forEach(aResMd.produceMediaTypes::add);
        resMd.getConsumedTypes().stream().map(MediaType::toString).forEach(aResMd.consumedMediaTypes::add);
        resMd.getInvocable().getParameters().stream().map(ParamMetaData::build).forEach(aResMd.params::add);

        return aResMd;
    }
}
