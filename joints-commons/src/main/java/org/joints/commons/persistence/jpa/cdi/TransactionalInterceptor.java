package org.joints.commons.persistence.jpa.cdi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.commons.persistence.jpa.JpaModule;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.Serializable;
import java.util.function.BiFunction;

@Transactional
@Interceptor
public class TransactionalInterceptor {

    private static final Logger log = LogManager.getLogger(TransactionalInterceptor.class);

    public static <P, R> Object withTransaction(BiFunction<EntityManager, P, R> bif, P param) throws Throwable {
        EntityManager em = JpaModule.getEntityManager();
        if (em == null) {
            log.error("failed to get EntityManager");
            return null;
        }

        if (!em.isOpen()) {
            log.error("not EntityManager Opened! \n\t");
            return null;
        }

        if (em.isJoinedToTransaction()) {
            return bif.apply(em, param);
        }

        final EntityTransaction transaction = em.getTransaction();
        if (!transaction.isActive()) {
            transaction.begin();
        }
        R returnValue = null;
        try {
            returnValue = bif.apply(em, param);
            // em.flush();
            transaction.commit();
            log.info("transaction committed");
        } catch (Throwable t) {
            try {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    log.warn("Rolled back transaction");
                }
            } catch (Exception e1) {
                log.warn("Rollback of transaction failed -> " + e1);
            }
            throw t;
        }
        return returnValue;
    }

    @AroundInvoke
    public Object withTransaction(InvocationContext ctx) throws Throwable {
        EntityManager em = JpaModule.getEntityManager();

        if (em == null) {
            log.warn("not EntityManager found! \n\t" + ctx.getTarget() + "." + ctx.getMethod());
            return ctx.proceed();
        }

        if (!em.isOpen()) {
            log.warn("not EntityManager Opened! \n\t" + ctx.getTarget() + "." + ctx.getMethod());
            return ctx.proceed();
        }

        if (em.isJoinedToTransaction()) {
            return ctx.proceed();
        }

        final EntityTransaction transaction = em.getTransaction();

        if (!transaction.isActive()) {
            transaction.begin();
        }
        Object returnValue = null;
        try {
            returnValue = ctx.proceed();
            // em.flush();
            transaction.commit();
            log.info("transaction committed");
        } catch (Throwable t) {
            try {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                    log.warn("Rolled back transaction");
                }
            } catch (Exception e1) {
                log.warn("Rollback of transaction failed -> " + e1);
            }
            throw t;
        }

        return returnValue;
    }
}
