package org.joints.commons.persistence.jpa;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.commons.MiscUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ApplicationScoped
public class JpaModule {

	private static final String EXTERNAL_JPA_PROPERTIES = "external_jpa_properties";

	static EntityManagerFactory emf = null;
	final static String UN = "mqueue-dispatcher";
	protected static final Logger log = LogManager.getLogger(JpaModule.class);
	private static JpaModule instance;

	protected static EntityManager initialValue() {
		log.info(MiscUtils.invocationInfo());
		initEmf();
		return emf.createEntityManager();
	};

	private static ThreadLocal<EntityManager> ems = ThreadLocal.withInitial(JpaModule::initialValue);

	private static synchronized void initEmf() {
		if (emf == null || !emf.isOpen()) {
			log.info("loading EntityManagerFactory......\n");
			emf = Persistence.createEntityManagerFactory(UN, getExternalPersistenceCfgs());
			log.info("\nEntityManagerFactory is loaded......\n");
		}
	}

	@PostConstruct
	public void init() {
		log.info(MiscUtils.invocationInfo());
		initEmf();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map getExternalPersistenceCfgs() {
		final String pathStr = System.getProperty(EXTERNAL_JPA_PROPERTIES);
		if (StringUtils.isBlank(pathStr)) {
			return Collections.EMPTY_MAP;
		}

		if (Files.isReadable(Paths.get(pathStr))) {
			log.warn(String.format("can't read %s", EXTERNAL_JPA_PROPERTIES));
			return Collections.EMPTY_MAP;
		}

		try {
			final Properties p = new Properties();
			p.load(new FileInputStream(pathStr));

			final HashMap settings = new HashMap(p);
			log.info("\nhave loaded data source\n" + settings + "\n\n");
			return settings;
		} catch (Exception e) {
			log.error("failed to load EXTERNAL_JPA_PROPERTIES: " + pathStr, e);
		}

		return Collections.EMPTY_MAP;
	}

	@Produces
	@PersistenceContext
	public static EntityManager getEntityManager() {
		final EntityManager em = ems.get();
		if (em != null && em.isOpen()) {
			return em;
		}

		log.info("creating EntityManager for Thread: " + Thread.currentThread().getName());
		ems.remove();

		return ems.get();
	}

	@PreDestroy
	public void destory() {
		log.info(MiscUtils.invocationInfo());
		log.info("\n\nclosing EntityManagerFactory......\n");
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
		emf = null;
		log.info("\n\nEntityManagerFactory is closed......\n");
	}

	@Produces
	public static JpaModule instance() {
		if (instance == null) {
			instance = new JpaModule();
			instance.init();
		}
		return instance;
	}
}
