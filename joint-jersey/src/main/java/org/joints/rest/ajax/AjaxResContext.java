package org.joints.rest.ajax;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.model.MethodList;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by fan on 2016/11/25.
 */
public class AjaxResContext {
    private static final Logger log = LogManager.getLogger(AjaxResContext.class);
    private static final ConcurrentMap<String, AjaxResContext> cache = new ConcurrentHashMap<String, AjaxResContext>();

    private AjaxResContext() {
    }

    public static AjaxResContext getInstance(String name) {
        return cache.computeIfAbsent(StringUtils.defaultIfBlank(name, ""), (n) -> new AjaxResContext());
    }

    List<AjaxResMetaData> proxyList = new ArrayList<AjaxResMetaData>();

    public List<AjaxResMetaData> getProxyList() {
        return proxyList;
    }

    public void build(final ResourceModel _resModel, final Application _app) {
        build(_resModel.getResources(), _app);
    }

    public void build(final Collection<Resource> resources, final Application _app) {
        if (CollectionUtils.isEmpty(resources)) {
            return;
        }
        List<AjaxResMetaData> resMetaDataList = getAjaxResMetaDatas(_app, resources);
        proxyList.addAll(resMetaDataList);
    }

    public void build(final Collection<Resource> resources) {
        proxyList.addAll(resources.parallelStream().map(res -> {
            AjaxResMetaData resMd = AjaxResMetaData.build(res);
            Collection<Class<?>> _clss = res.getHandlerClasses();

            _clss.stream().filter(_cls -> !isSingleton(_cls)).forEach(_cls -> {
                getClassSetters(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
                getClassFields(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
            });

            log.info("\n injected Params: \n\t" + StringUtils.join(resMd.injectedParams, "\n\t"));
            resMd.appendInjectedParams(resMd.injectedParams);
            return resMd;
        }).collect(Collectors.toList()));
    }

    public static List<AjaxResMetaData> getAjaxResMetaDatas(Application _app, Collection<Resource> resources) {
        Set<Class<?>> resClass = _app.getClasses();
        log.info("\nfound resources classes: \n" + StringUtils.join(resClass, '\n'));
        log.info("\nfound singleton instances: \n" + StringUtils.join(_app.getSingletons(), '\n'));

        return resources.parallelStream().map(res -> {
            AjaxResMetaData resMd = AjaxResMetaData.build(res);
            Collection<Class<?>> _clss = CollectionUtils.intersection(resClass, res.getHandlerClasses());

            _clss.stream().filter(_cls -> !isSingleton(_cls)).forEach(_cls -> {
                getClassSetters(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
                getClassFields(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
            });

            log.info("\n injected Params: \n\t" + StringUtils.join(resMd.injectedParams, "\n\t"));
            resMd.appendInjectedParams(resMd.injectedParams);
            return resMd;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    private static List<Parameter> getClassSetters(final boolean encodedFlag, final Class handlerClass) {
        MethodList methodList = new MethodList(handlerClass, true);

        methodList = methodList.withoutMetaAnnotation(HttpMethod.class).
            withoutAnnotation(Path.class).
            withoutAnnotation(Context.class).
            hasNumParams(1).
            hasReturnType(void.class).
            nameStartsWith("set");

        if (!methodList.iterator().hasNext())
            return Collections.emptyList();

        //AnnotatedMethod
        return Stream.generate(methodList.iterator()::next).map(method -> Parameter.create(
            handlerClass,
            method.getMethod().getDeclaringClass(),
            encodedFlag || method.isAnnotationPresent(Encoded.class),
            method.getParameterTypes()[0],
            method.getGenericParameterTypes()[0],
            method.getAnnotations())).filter(Objects::nonNull).collect(Collectors.toList());

    }

    @SuppressWarnings("rawtypes")
    private static List<Parameter> getClassFields(final boolean encodedFlag, Class handlerClass) {
        return Stream.of(AccessController.doPrivileged(ReflectionHelper.getDeclaredFieldsPA(handlerClass)))
            .filter(field -> (field.getDeclaredAnnotations().length > 0
                && (field.isAnnotationPresent(QueryParam.class)
                || field.isAnnotationPresent(PathParam.class)
                || field.isAnnotationPresent(CookieParam.class)
                || field.isAnnotationPresent(MatrixParam.class)
                || field.isAnnotationPresent(FormParam.class)
                || field.isAnnotationPresent(HeaderParam.class))))
            .map(field -> Parameter.create(
                handlerClass,
                field.getDeclaringClass(),
                encodedFlag || field.isAnnotationPresent(Encoded.class),
                field.getType(),
                field.getGenericType(),
                field.getAnnotations()))
            .collect(Collectors.toList());
    }

    /**
     * Check if the resource class is declared to be a singleton.
     *
     * @param resourceClass resource class.
     * @return {@code true} if the resource class is a singleton, {@code false} otherwise.
     */
    private static boolean isSingleton(Class<?> resourceClass) {
        return resourceClass.isAnnotationPresent(Singleton.class)
            || (Providers.isProvider(resourceClass) && !resourceClass.isAnnotationPresent(PerLookup.class));
    }
}
