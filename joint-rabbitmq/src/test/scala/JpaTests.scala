
import javax.persistence.{EntityManager, EntityManagerFactory, Persistence}

import org.apache.logging.log4j.{LogManager, Logger}
import org.junit.{After, AfterClass, Before, Test}

/**
  * Created by fan on 2017/1/30.
  */
object JpaTests {
    val log: Logger = LogManager.getLogger(classOf[JpaTests])
    val UN: String = "mqueue-dispatcher"
    lazy val emf: EntityManagerFactory = Persistence.createEntityManagerFactory(UN)
    lazy val em: EntityManager = emf.createEntityManager()

    @AfterClass
    def tearDown(): Unit = {
        if (em != null) em.close()
        if (emf != null) emf.close()
    }
}

class JpaTests {

    import JpaTests._

    @Before
    def startTransaction() {
        val tx = em.getTransaction
        if (tx.isActive) return
        tx.begin()
    }

    @After
    def commit() {
        val tx = em.getTransaction
        if (!tx.isActive) return
        tx.commit()
    }

    @Test
    def validateEntities() {
        log.info(String.format("sc %s", em.createQuery("select count(id) from ServerCfg sc").getSingleResult))
        log.info(String.format("exc %s", em.createQuery("select count(id) from ExchangeCfg ec").getSingleResult))
        log.info(String.format("qc %s", em.createQuery("select count(id) from QueueCfg qc").getSingleResult))
        log.info(String.format("mc %s", em.createQuery("select count(mc) from MessageContext mc").getResultList))
    }
}
