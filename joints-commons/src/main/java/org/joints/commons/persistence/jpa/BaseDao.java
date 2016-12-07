package org.joints.commons.persistence.jpa;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.util.reflection.Reflections;
import org.joints.commons.MiscUtils;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

//import org.jboss.weld.util.reflection.Reflections;

/**
 * abstraction for most of JPA operations
 *
 * @param <T>
 * @author fan
 */

@SuppressWarnings({"rawtypes", "unchecked"})
@TransactionManagement(TransactionManagementType.CONTAINER)
public class BaseDao<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(BaseDao.class);

    @PersistenceContext
    protected EntityManager em;

    public BaseDao() {

    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEm() {
        return em;
    }

    public BaseDao(final EntityManager _em) {
        this.em = _em;
    }

    protected Class<T> getEntityClass() {
        return (Class<T>) Reflections.getActualTypeArguments(this.getClass())[0];
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public T create(final T entity) {
        em.persist(entity);
        return entity;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public T edit(final T entity) {
        T n = em.merge(entity);
        em.flush();
        return n;// em.merge(entity);
    }

    public T refresh(final T entity) {
        em.refresh(entity);
        return entity;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public T destroy(final T entity) {
        em.remove(em.merge(entity));
        return entity;
    }

    public T find(Object pk) {
        if (pk == null)
            return null;
        return getEntityClass().cast(em.find(getEntityClass(), pk));
    }

    public <E> E findEntity(Class<E> cls, Object pk) {
        if (pk == null || cls == null) {
            return null;
        }
        return cls.cast(em.find(cls, pk));
    }

    public List<T> findAll() {
        return em.createQuery(String.format("select object(o) from %s as o", getEntityClass().getSimpleName())).getResultList();
    }

	/*
     * (non-Javadoc)
	 * 
	 * @see com.netcircle.paymentsystem.dao.impl.IBaseDao#queryCount(java.lang.String , java.lang.Object)
	 */

    public Object findOneEntity(String hql, Object... params) {
        final List result = queryEntityPage(hql, 0, 1, params);
        return CollectionUtils.isEmpty(result) ? null : result.get(0);
    }

    public T findOne(String hql, Object... params) {
        final List<?> result = queryEntityPage(hql, 0, 1, params);
        return CollectionUtils.isEmpty(result) ? null : getEntityClass().cast(result.get(0));
    }

    public void beginTransaction() {
        beginTransaction(em);
    }

    public static EntityTransaction beginTransaction(EntityManager em) {
        final EntityTransaction transaction = em.getTransaction();
        if (transaction.isActive()) {
            return transaction;
        }

        transaction.begin();
        return transaction;
    }

    public static EntityTransaction endTransaction(EntityManager em) {
        final EntityTransaction transaction = em.getTransaction();
        if (!transaction.isActive()) {
            log.error("\nInactive Tranaction, No Commit");
            return transaction;
        }

        if (transaction.getRollbackOnly()) {
            transaction.rollback();
            return transaction;
        }

        transaction.commit();
        return transaction;
    }

    public void endTransaction() {
        endTransaction(em);
    }

    public int executeUpdateQuery(String hql, Object... params) {
        return SimpleQueryBuilder.byHQL(hql, em).build().executeUpdate();
    }

	/*
     * (non-Javadoc)
	 * 
	 * @see com.netcircle.paymentsystem.dao.impl.IBaseDao#queryCount(java.lang.String , java.lang.Object)
	 */

    public List<T> query(String hql, Object... params) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).withPositionedParams(params).doQuery();
    }

    protected long getCountFromResult(final List<?> resultList) {
        if (CollectionUtils.isEmpty(resultList)) {
            return 0;
        }
        if (resultList.size() == 1) {
            Object first = resultList.get(0);
            if (first instanceof Long) {
                return (Long) resultList.get(0);
            }
            if (first.getClass().isArray()) {
                return (Long) ((Object[]) first)[0];
            }
        }
        return resultList.size();
    }

    public long queryCount(String hql, Object... params) {
        if (StringUtils.isBlank(hql)) {
            return 0;
        }
        final List<?> resultList = query(hql, params);
        return getCountFromResult(resultList);
    }

    public long queryCountByNativeSql(String sql, Object... params) {
        if (StringUtils.isBlank(sql)) {
            return 0;
        }
        return getCountFromResult(SimpleQueryBuilder.byNativeSQL(sql, em).withPositionedParams(params).doQuery());
    }

    public long countByNativeSqlWithIndexedParams(String sql, Map<Integer, Object> namedParams) {
        if (StringUtils.isBlank(sql)) {
            return 0;
        }
        return getCountFromResult(SimpleQueryBuilder.byNativeSQL(sql, em).withPositionedParams(namedParams).doQuery());
    }

    public long countByNativeSqlWithIndexedParams(String sql, Object... params) {
        if (StringUtils.isBlank(sql)) {
            return 0;
        }
        return getCountFromResult(SimpleQueryBuilder.byNativeSQL(sql, em).withPositionedParams(params).doQuery());
    }

    public long queryCountWithNamedParams(String hql, Map<String, Object> namedParams) {
        if (StringUtils.isBlank(hql)) {
            return 0;
        }
        final List<?> resultList = queryPageByNamedParams(hql, -1, -1, namedParams);
        return getCountFromResult(resultList);
    }

    public long queryCountWithPositionalParams(String hql, Map<Integer, Object> positionalParams) {
        if (StringUtils.isBlank(hql)) {
            return 0;
        }
        return getCountFromResult(SimpleQueryBuilder.byHQL(hql, em).withPositionedParams(positionalParams).doQuery());
    }

    public List queryEntity(String hql, Object... params) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).withPositionedParams(params).doQuery();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.netcircle.paymentsystem.dao.impl.IBaseDao#queryPage(java.lang.String, int, int, java.lang.Object)
	 */

    public List queryEntityPage(String hql, int pageIdx, int pageSize, Object... params) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withPositionedParams(params).doQuery();
    }

    public List queryEntityPageByNamedParams(String hql, int pageIdx, int pageSize, Map<String, Object> namedParams) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withNamedParams(namedParams).doQuery();
    }

    public List queryEntityByNamedParams(String hql, Map<String, Object> namedParams) {
        return SimpleQueryBuilder.byHQL(hql, em).withNamedParams(namedParams).doQuery();
    }

    public List queryEntityPageByPositionalParams(String hql, int pageIdx, int pageSize, Map<Integer, Object> positionalParams) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withPositionedParams(positionalParams).doQuery();
    }

    public List queryEntityByPositionalParams(String hql, Map<Integer, Object> positionalParams) {
        return SimpleQueryBuilder.byHQL(hql, em).withPositionedParams(positionalParams).doQuery();
    }

    public List<T> queryPage(String hql, int pageIdx, int pageSize, Object... params) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withPositionedParams(params).doQuery();
    }

    public List<T> queryPageByNamedParams(String hql, int pageIdx, int pageSize, Map<String, Object> namedParams) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withNamedParams(namedParams).doQuery();
    }

    public List<T> queryPageByPositionalParams(String hql, int pageIdx, int pageSize, Map<Integer, Object> positionalParams) {
        if (StringUtils.isBlank(hql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byHQL(hql, em).page(pageIdx, pageSize).withPositionedParams(positionalParams).doQuery();
    }

    public List queryByNativeSqlWithIndexedParams(String sql, int pageIdx, int pageSize, Object... params) {
        if (StringUtils.isBlank(sql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byNativeSQL(sql, em).withPositionedParams(params).page(pageIdx, pageSize).doQuery();
    }

    public List queryByNativeSqlWithIndexedParams(String sql, int pageIdx, int pageSize, Map<Integer, Object> positionalParams) {
        if (StringUtils.isBlank(sql)) {
            return Collections.EMPTY_LIST;
        }
        return SimpleQueryBuilder.byNativeSQL(sql, em).withPositionedParams(positionalParams).page(pageIdx, pageSize).doQuery();
    }

    public static class SimpleQueryBuilder {
        private final Query q;

        public SimpleQueryBuilder(Query q) {
            super();
            if (q == null)
                throw new IllegalArgumentException("Query is null!");
            this.q = q;
        }

        public static SimpleQueryBuilder byHQL(final String hql, final EntityManager em) {
            if (StringUtils.isBlank(hql))
                throw new IllegalArgumentException("hql is null!");
            if (em == null)
                throw new IllegalArgumentException("EntityManager em is null!");
            return new SimpleQueryBuilder(em.createQuery(hql));
        }

        public static SimpleQueryBuilder byNativeSQL(final String sql, final EntityManager em) {
            if (StringUtils.isBlank(sql))
                throw new IllegalArgumentException("sql is null!");
            if (em == null)
                throw new IllegalArgumentException("EntityManager em is null!");
            return new SimpleQueryBuilder(em.createNativeQuery(sql));
        }

        public Query build() {
            return q;
        }

        public SimpleQueryBuilder withNamedParams(Map<String, Object> namedParams) {
            if (MapUtils.isEmpty(namedParams))
                return this;
            namedParams.forEach((key, value) -> q.setParameter(key, value));
            return this;
        }

        public SimpleQueryBuilder withPositionedParams(Map<Integer, Object> positionedParams) {
            Object[] params = new Object[positionedParams.size()];
            IntStream.range(0, positionedParams.size()).forEach((i) -> params[i] = positionedParams.get(Integer.valueOf(i)));
            return withPositionedParams(params);
        }

        public SimpleQueryBuilder withPositionedParams(Object... params) {
            if (ArrayUtils.isNotEmpty(params)) {
                IntStream.range(0, params.length).forEachOrdered((i) -> q.setParameter(1 + i, params[i]));
            }
            return this;
        }

        public SimpleQueryBuilder page(int pageIdx, int pageSize) {
            if (pageIdx >= 0 && pageSize > 0) {
                q.setFirstResult(pageIdx * pageSize);
                q.setMaxResults(pageSize);
            }
            return this;
        }

        public List doQuery() {
            if (q == null)
                return Collections.EMPTY_LIST;
            return ObjectUtils.defaultIfNull(q.getResultList(), Collections.EMPTY_LIST);
        }
    }

    public void close() {
        log.info(MiscUtils.invocationInfo());
        try {
            if (em != null && em.isOpen()) {
                final EntityTransaction transaction = em.getTransaction();
                if (transaction != null && transaction.isActive()) {
                    em.flush();
                }
                em.close();
            }
        } catch (Exception e) {
            log.error("failed to close em: " + e.getMessage());
        }
    }
}
