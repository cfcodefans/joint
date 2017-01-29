package org.joints.rest.ajax;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.functors.EqualPredicate;
import org.apache.commons.collections4.functors.NotPredicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Resource;
import org.joints.commons.MiscUtils;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@XmlRootElement
public class AjaxResMetadata implements Serializable {

    public enum DataType {
        text, html, script, json, jsonp, xml;

        @SuppressWarnings("unchecked")
        public static final Map<MediaType, DataType> internalMap = MiscUtils.map(
            MediaType.APPLICATION_JSON_TYPE, json,
            MediaType.TEXT_PLAIN_TYPE, text,
            MediaType.APPLICATION_XML_TYPE, xml,
            MediaType.TEXT_XML_TYPE, xml,
            MediaType.TEXT_HTML, html,
            MediaType.APPLICATION_XHTML_XML_TYPE, html,
            MediaType.valueOf("text/event-stream"), text);

        public static List<DataType> convert(List<MediaType> mediaTypes) {
            final List<DataType> dataTypeList = mediaTypes.stream().map(internalMap::get).filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dataTypeList)) {
                return Arrays.asList(text);
            }
            return dataTypeList;
        }
    }

    private static final long serialVersionUID = 1L;

    @XmlTransient
    @JsonIgnore
    public String path;
    public String name;

    @XmlTransient
    @JsonIgnore
    public String baseUrl;

    public String getUrl() {
        return baseUrl + (StringUtils.endsWith(baseUrl, "/") || StringUtils.startsWith(path, "/") ? "" : "/") + path;
    }

    public List<AjaxResMetadata> children = new ArrayList<AjaxResMetadata>();

    @XmlTransient
    @JsonIgnore
    public List<ParamMetaData> injectedParams = new ArrayList<ParamMetaData>();

    @XmlTransient
    @JsonIgnore
    public AjaxResMetadata parent;

    @XmlTransient
    @JsonIgnore
    public transient Resource resource;

    public List<AjaxResMethodMetaData> methods = new ArrayList<AjaxResMethodMetaData>();

    public static AjaxResMetadata build(Resource res) {
        if (res == null) {
            return null;
        }

        AjaxResMetadata resMD = new AjaxResMetadata();

        resMD.name = CollectionUtils.find(res.getNames(), NotPredicate.notPredicate(EqualPredicate.equalPredicate("[unnamed]")));
        resMD.name = StringUtils.substringAfterLast(resMD.name, ".");
        resMD.path = res.getPath();

        resMD.resource = res;

        res.getChildResources().stream()
            .map(AjaxResMetadata::build)
            .filter(Objects::nonNull)
            .forEach(subResMD -> {
                subResMD.parent = resMD;
                resMD.children.add(subResMD);
            });

        res.getResourceMethods().stream()
            .map(AjaxResMethodMetaData::build)
            .filter(Objects::nonNull)
            .forEach(resMD.methods::add);

        resMD.methods.forEach(methodMeta -> methodMeta.parent = resMD);

        resMD.baseUrl = res.getPath();

        return resMD;
    }

    public void setBaseUrl(String _baseUrl) {
        this.baseUrl = _baseUrl;
        children.forEach(armd -> armd.setBaseUrl(_baseUrl + (StringUtils.endsWith(_baseUrl, "/") ? "" : "/") + path));
    }

    public void appendInjectedParams(List<ParamMetaData> _params) {
        children.forEach(armd -> armd.appendInjectedParams(_params));
        methods.forEach(md -> md.params.addAll(_params));
    }

    public static List<AjaxResMethodMetaData> flatten(List<AjaxResMetadata> resMetadataList, List<AjaxResMethodMetaData> methodMetaDataList) {
        if (CollectionUtils.isEmpty(resMetadataList)) return methodMetaDataList;
        for (AjaxResMetadata ajaxResMetadata : resMetadataList) {
            methodMetaDataList.addAll(ajaxResMetadata.methods);
            flatten(ajaxResMetadata.children, methodMetaDataList);
        }
        return methodMetaDataList;
    }

    public static List<AjaxResMethodMetaData> flatten(AjaxResMetadata...resMetadatas) {
        if (ArrayUtils.isEmpty(resMetadatas)) return Collections.emptyList();
        return flatten(Arrays.asList(resMetadatas), new ArrayList<AjaxResMethodMetaData>());
    }
}
