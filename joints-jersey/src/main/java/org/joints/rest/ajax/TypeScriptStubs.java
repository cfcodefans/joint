package org.joints.rest.ajax;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Parameter;
import org.joints.commons.Jsons;
import org.joints.commons.MiscUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by fan on 2017/1/24.
 */
public class TypeScriptStubs {
    public static Map<Class, String> getBasicTypesAndTypeSources() {
        Map<Class, String> classAndTypeSources = new HashMap<>();

        String numberLiteral = TypeScriptBasics.number.literal;
        classAndTypeSources.put(int.class, numberLiteral);
        classAndTypeSources.put(short.class, numberLiteral);
        classAndTypeSources.put(byte.class, numberLiteral);
        classAndTypeSources.put(long.class, numberLiteral);
        classAndTypeSources.put(float.class, numberLiteral);
        classAndTypeSources.put(double.class, numberLiteral);
        classAndTypeSources.put(Date.class, numberLiteral);

        classAndTypeSources.put(char.class, TypeScriptBasics.string.literal);
        classAndTypeSources.put(boolean.class, TypeScriptBasics._boolean.literal);
        classAndTypeSources.put(Boolean.class, TypeScriptBasics._boolean.literal);

        return classAndTypeSources;
    }

    private static Map<Class, String> basicTypesAndTypeSources = getBasicTypesAndTypeSources();

    public static String classToTypeScriptDef(Class cls, Map<Class, String> classAndTypeSources, Map<Class, String> classAndTypeNames) {
        if (cls == null) return StringUtils.EMPTY;

        if (classAndTypeSources == null) {
            classAndTypeSources = new HashMap<>();
        }

        if (classAndTypeNames == null) {
            classAndTypeNames = getBasicTypesAndTypeSources();
        }

        {
            String typeName = classAndTypeNames.get(cls);
            if (StringUtils.isNotBlank(typeName))
                return typeName;
        }

        if (Number.class.isAssignableFrom(cls)) {
            return classAndTypeNames.computeIfAbsent(cls, (Class key) -> TypeScriptBasics.number.literal);
        }

        if (CharSequence.class.isAssignableFrom(cls)) {
            return classAndTypeNames.computeIfAbsent(cls, (Class key) -> TypeScriptBasics.string.literal);
        }

        if (cls.isEnum()) {
            Map<Class, String> finalClassAndTypeNames = classAndTypeNames;
            classAndTypeSources.computeIfAbsent(cls, (Class key) -> {
                Map enumMap = EnumUtils.getEnumMap(key);
                finalClassAndTypeNames.put(key, key.getSimpleName());
                return String.format("\nenum %s {%s};\n", key.getSimpleName(), StringUtils.join(enumMap.keySet(), ", "));
            });
            return classAndTypeNames.get(cls);
        }

        if (cls.isArray()) {
            Class componentType = cls.getComponentType();
            String sourceType = classToTypeScriptDef(componentType, classAndTypeSources, classAndTypeNames);
            if (StringUtils.isNotBlank(sourceType)) {
                return String.format("%s[]", sourceType);
            } else {
                return "Any[]";
            }
        }

        if (Collection.class.isAssignableFrom(cls)) {
            return "Any[]";
        }

        classAndTypeNames.putIfAbsent(cls, cls.getSimpleName());

        SerializationConfig cfg = Jsons.MAPPER.getSerializationConfig();
        JavaType jt = cfg.constructType(cls);
        BeanDescription bd = cfg.introspect(jt);
        List<BeanPropertyDefinition> propList = bd.findProperties();

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("interface %s {\n", cls.getSimpleName()));

        for (BeanPropertyDefinition propDef : propList) {
            if (propDef instanceof POJOPropertyBuilder) {
                POJOPropertyBuilder def = (POJOPropertyBuilder) propDef;
                if (def.anyIgnorals() || !def.couldSerialize()) continue;
            } else if (propDef instanceof SimpleBeanPropertyDefinition) {
                SimpleBeanPropertyDefinition def = (SimpleBeanPropertyDefinition) propDef;
                if (!def.couldSerialize()) continue;
            }

            Method md = propDef.getGetter().getMember();
            Class<?> returnClz = md.getReturnType();

            if (Collection.class.isAssignableFrom(returnClz)) {
                Class[] parameterizedClzz = MiscUtils.getParameterizedClzz(md.getGenericReturnType());
                String typeName = classToTypeScriptDef(parameterizedClzz[0], classAndTypeSources, classAndTypeNames);
                sb.append(String.format("\t%s: %s[];\n", propDef.getName(), typeName));
            } else if (returnClz.isAnnotation()) {
                continue;
            } else {
                String typeName = classToTypeScriptDef(returnClz, classAndTypeSources, classAndTypeNames);
                sb.append(String.format("\t%s: %s;\n", propDef.getName(), typeName));
            }
        }

        sb.append("}");
        classAndTypeSources.put(cls, sb.toString());

        return cls.getSimpleName();
    }

    enum TypeScriptBasics {
        _boolean("boolean"), number("number"), string("string"), any("any");
        public String literal;

        TypeScriptBasics(String _literal) {
            this.literal = _literal;
        }
    }

    public static Map<AjaxResMetadata, String> getResourceAndTypeSources(List<AjaxResMetadata> metadataList, Map<Class, String> classAndTypeNames) {
        Map<AjaxResMetadata, String> reMap = new HashMap<>();
        if (CollectionUtils.isEmpty(metadataList))
            return reMap;

        for (AjaxResMetadata md : metadataList) {

        }
        return reMap;
    }

    static String toTypeSource(AjaxResMetadata md, Map<Class, String> classAndTypeNames) {
        StringBuilder sb = new StringBuilder();
        if (md == null) return sb.toString();

        sb.append(String.format("class %s extends IRestInvocation {\n", md.name));
        sb.append(String.format("\turl: string = '%s';\n", md.getUrl()));

        md.injectedParams.stream()
            .filter(pmd->pmd.source != Parameter.Source.CONTEXT)
            .forEach(pmd->sb.append(String.format("\t%s: Param = new Param('%s', '%s');\n", pmd.sourceName, pmd.sourceName, pmd.source)));

        sb.append(String.format("\tparams():IParam[] {return new Array[IParam]{%s};}\n",
            StringUtils.join(md.injectedParams.stream().map(pmd -> pmd.sourceName).toArray(), " ,")));

        sb.append(StringUtils.join(md.methods.stream()
            .map(rmd->toTypeSource(rmd, classAndTypeNames)).toArray(), "\n"));

        sb.append("\n}");
        return sb.toString();
    }

    static String toTypeSource(ParamMetaData param, Map<Class, String> classAndTypeNames) {

    }

    static String toTypeSource(AjaxResMethodMetaData method, Map<Class, String> classAndTypeNames) {

    }
}
