package org.joints.rest.ajax;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEvent.Type;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.joints.commons.MiscUtils;
import org.joints.commons.ProcTrace;

public class AjaxResLoader extends ResourceConfig {
    private static final Logger log = LogManager.getLogger(AjaxResLoader.class);

    public AjaxResLoader() {
        ProcTrace.start(MiscUtils.invocationInfo());
        ProcTrace.ongoing("set packages scan");

        register(JacksonFeature.class);
        register(EncodingFilter.class);
        register(GZipEncoder.class);
        register(DeflateEncoder.class);
        register(new ResLoaderListener());

        ProcTrace.end();
        log.info(ProcTrace.flush());

        instance = this;

    }

    private static AjaxResLoader instance = null;

    public static AjaxResLoader instance() {
        return instance;
    }

    public static class ResLoaderListener implements ContainerLifecycleListener, ApplicationEventListener {
        @Override
        public void onStartup(Container c) {
            log.info(MiscUtils.invocationInfo() + "\n\n\t");
        }

        @Override
        public void onReload(Container c) {
            log.info(MiscUtils.invocationInfo());
            ResourceConfig resCfg = c.getConfiguration();
            AjaxResContext ajaxResCtx = AjaxResContext.getInstance(resCfg.getApplicationName());
            ajaxResCtx.proxyList.clear();
            ajaxResCtx.build(resCfg.getResources(), resCfg.getApplication());
        }

        @Override
        public void onShutdown(Container c) {
            log.info(MiscUtils.invocationInfo());
        }

        @Override
        public void onEvent(ApplicationEvent ev) {
            log.info(MiscUtils.invocationInfo() + "\n\n\t");
            if (Type.INITIALIZATION_APP_FINISHED == ev.getType()) {
                AjaxResContext.getInstance(ev.getResourceConfig().getApplicationName())
                    .build(ev.getResourceModel(), ev.getResourceConfig());
            }
            log.info(MiscUtils.invocationInfo() + " finished loading restful apis.....");
        }

        @Override
        public RequestEventListener onRequest(RequestEvent reqEv) {
            return null;
        }
    }
}
