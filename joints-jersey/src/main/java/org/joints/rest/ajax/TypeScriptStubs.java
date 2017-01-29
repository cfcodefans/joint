package org.joints.rest.ajax;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Parameter;
import org.joints.commons.Jsons;
import org.joints.commons.MiscUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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
        classAndTypeSources.put(Character.class, TypeScriptBasics.string.literal);
        classAndTypeSources.put(boolean.class, TypeScriptBasics._boolean.literal);
        classAndTypeSources.put(Boolean.class, TypeScriptBasics._boolean.literal);
        classAndTypeSources.put(Object.class, TypeScriptBasics.any.literal);

        return classAndTypeSources;
    }

    public static class TypeScriptParseContext implements Serializable {
        public final Map<Class, String> classAndTypeNames = getBasicTypesAndTypeSources();
        public final Map<Class, String> classAndTypeSources = new HashMap<>();
        public final Map<String, String> resNameAndTypeSources = new HashMap<>();

        @Override
        public String toString() {
            return StringUtils.join(classAndTypeSources.values(), "\n")
//                + "\n" + StringUtils.join(classAndTypeNames.values(), "\n")
                + "\n" + StringUtils.join(resNameAndTypeSources.values(), "\n");
        }
    }

    public static String classToTypeScriptDef(Class cls, TypeScriptParseContext ctx) {
        if (cls == null) return StringUtils.EMPTY;

        Map<Class, String> classAndTypeSources = ctx.classAndTypeSources;
        Map<Class, String> classAndTypeNames = ctx.classAndTypeNames;
        {
            String typeName = classAndTypeNames.get(cls);
            if (StringUtils.isNotBlank(typeName))
                return typeName;
        }

        if (Number.class.isAssignableFrom(cls)) {
            return classAndTypeNames.computeIfAbsent(cls, (Class key) -> TypeScriptBasics.number.literal);
        }

        if (Map.class.isAssignableFrom(cls)) {
            return classAndTypeNames.computeIfAbsent(cls, (Class key) -> TypeScriptBasics.any.literal);
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
            String sourceType = classToTypeScriptDef(componentType, ctx);
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
                if (ArrayUtils.isEmpty(parameterizedClzz)) {
                    sb.append(String.format("\t%s: any[];\n", propDef.getName()));
                } else {
                    String typeName = classToTypeScriptDef(parameterizedClzz[0], ctx);
                    sb.append(String.format("\t%s: %s[];\n", propDef.getName(), typeName));
                }
            } else if (returnClz.isAnnotation()) {
                continue;
            } else {
                String typeName = classToTypeScriptDef(returnClz, ctx);
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

    public static Map<AjaxResMetadata, String> getResourceAndTypeSources(List<AjaxResMetadata> metadataList, TypeScriptParseContext ctx) {
        Map<AjaxResMetadata, String> reMap = new HashMap<>();
        if (CollectionUtils.isEmpty(metadataList))
            return reMap;

        for (AjaxResMetadata md : metadataList) {
            reMap.put(md, toTypeSource(md, ctx));
        }
        return reMap;
    }

    static String toTypeSource(AjaxResMetadata md, TypeScriptParseContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (md == null) return sb.toString();
        try {
            sb.append(String.format("class %s {\n", md.name));
            sb.append(String.format("\turl: string = '%s';\n", md.getUrl()));

            md.injectedParams.stream()
                .filter(pmd -> pmd.source != Parameter.Source.CONTEXT
                    && pmd.source != Parameter.Source.BEAN_PARAM) //TODO, it would be too complicate to support bean param, and it is purely for java backend's convenience, is it really practical?
                .forEach(pmd -> sb.append(String.format("\t%s: Param = new Param('%s', '%s', '%s');\n", pmd.sourceName, pmd.source, "")));

            sb.append(String.format("\tparams():IParam[] { return [%s]; }\n",
                StringUtils.join(md.injectedParams.stream().map(pmd -> pmd.sourceName).toArray(), " ,")));

            sb.append(StringUtils.join(md.methods.stream()
                .map(rmd -> toTypeSource(rmd, ctx)).toArray(), "\n"));

            List<AjaxResMethodMetaData> subResMethodList = AjaxResMetadata.flatten(md);
            if (CollectionUtils.isNotEmpty(subResMethodList)) {
                sb.append(StringUtils.join(subResMethodList.stream()
                    .map(rmd -> toTypeSource(rmd, ctx)).toArray(), "\n"));
            }

            sb.append("\n}");

            ctx.resNameAndTypeSources.put(md.name, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    static String toTypeSource(ParamMetaData param, TypeScriptParseContext ctx) {
        if (Collection.class.isAssignableFrom(param.rawType)) {
            Class[] parameterizedClzz = MiscUtils.getParameterizedClzz(param.type);
            String typeName = classToTypeScriptDef(parameterizedClzz[0], ctx);
            return String.format("%s[]", typeName);
        }
        return String.format("%s", classToTypeScriptDef(param.rawType, ctx));
    }

    static String toTypeSource(AjaxResMethodMetaData method, TypeScriptParseContext ctx) {
        StringBuilder sb = new StringBuilder().append("\t").append(method.name).append('(');
        String returnTypeName = classToTypeScriptDef(method.returnType, ctx);

        List<String> paramStrList = method.params.stream()
            .filter(pmd -> pmd.source != Parameter.Source.CONTEXT && pmd.source != Parameter.Source.BEAN_PARAM)
            .map(pmd -> {
                String paramType = toTypeSource(pmd, ctx);
                return String.format("%s: %s", pmd.sourceName, paramType);
            }).collect(Collectors.toList());

        sb.append(StringUtils.join(paramStrList, " ,"));
        sb.append(String.format("): IRestInvocation<%s> {\n", returnTypeName));
        method.params.stream()
            .filter(pmd -> pmd.source != Parameter.Source.CONTEXT
                && pmd.source != Parameter.Source.BEAN_PARAM) //TODO, it would be too complicated to support bean param, and it is purely for java backend's convenience, is it really practical?
            .forEach(pmd -> sb.append(String.format("\tlet _%s: Param = new Param('%s', '%s', Source.%s);\n", pmd.sourceName, pmd.sourceName, pmd.sourceName, pmd.source)));

        sb.append(String.format("\tlet _params:IParam[] = [%s];\n",
            StringUtils.join(method.params.stream().map(pmd -> '_' + pmd.sourceName).toArray(), ", ")));

        sb.append(String.format("\treturn <IRestInvocation<%s>> { \n\t\tparams:_params, " +
                "\n\t\tmethod:HttpMethod.%s, " +
                "\n\t\tresultType: '%s', " +
                "\n\t\tpath: '%s', " +
                "\n\t\tname: '%s'," +
                "\n\t\tproduceMediaTypes: [%s], " +
                "\n\t\tconsumedMediaTypes: [%s]};\n}\n",
            returnTypeName,
            method.httpMethod,
            returnTypeName,
            method.getPath(),
            method.name,
            StringUtils.join(method.produceMediaTypes.stream().map(type -> String.format("'%s'", type)).toArray(), ","),
            StringUtils.join(method.consumedMediaTypes.stream().map(type -> String.format("'%s'", type)).toArray(), ",")));

        return sb.toString();
    }
}
