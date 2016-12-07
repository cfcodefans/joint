package org.joints.commons.persistence.jpa;

import org.joints.commons.persistence.jpa.cdi.Transactional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


/**
 * To avoid the conflict between cdi and ejb, this class could be used in non-ejb container
 * 
 * @author fan
 *
 * @param <T>
 */
public abstract class CdiBaseDao<T> extends BaseDao<T> {

	private static final long	serialVersionUID	= 1L;

	public CdiBaseDao() {

	}

	@Inject
	@PersistenceContext
	public void setEm(EntityManager em) {
		this.em = em;
	}

	public EntityManager getEm() {
		return em;
	}

	@Transactional
	public T create(final T entity) {
		return super.create(entity);
	}

	@Transactional
	public T edit(final T entity) {
		return super.edit(entity);
	}

	@Transactional
	public T destroy(final T entity) {
		return super.destroy(entity);
	}

	public CdiBaseDao( final EntityManager _em) {
		super(_em);
	}
}
