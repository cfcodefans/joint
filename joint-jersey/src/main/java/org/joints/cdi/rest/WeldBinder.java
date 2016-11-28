package org.joints.cdi.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

public class WeldBinder extends AbstractBinder {

    private static final Logger log = LogManager.getLogger(WeldBinder.class);

    @Override
    protected void configure() {
        try {
            BeanManager bm = CDI.current().getBeanManager();
            bm.getBeans(Object.class, DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE).forEach(bean -> bindToJerseyContext(bm, bean));
        } catch (Exception e) {
            log.error("failed to bind", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bindToJerseyContext(BeanManager bm, final Bean bean) {
        try {
            Object _ref = bm.getReference(bean, bean.getBeanClass(), bm.createCreationalContext(bean));
            bind(_ref).to(bean.getBeanClass());
        } catch (Exception e) {
            log.warn(String.format("failed to bind: %s \n\t%s", e.getStackTrace()[0], e.getMessage()));
        }
    }

    public static <T> T getBean(Class<T> cls) {
        return CDI.current().select(cls).get();
    }
}
