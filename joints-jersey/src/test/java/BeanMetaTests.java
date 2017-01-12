import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.ArrayType;
import org.joints.commons.Jsons;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by fan on 2017/1/12.
 */
public class BeanMetaTests {
    public static class POJO {
        private static AtomicLong ID = new AtomicLong(1);
        private int version = 0;
        private long id = ID.getAndIncrement();

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        private Date updatedAt = new Date();

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ServerCfg extends POJO {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ExchangeCfg extends POJO {
        private String name;
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @JsonBackReference
        private Set<QueueCfg> queues = new HashSet<>();

        public Set<QueueCfg> getQueues() {
            return queues;
        }

        public void setQueues(Set<QueueCfg> queues) {
            this.queues = queues;
        }

        private ServerCfg server;

        public ServerCfg getServer() {
            return server;
        }

        public void setServer(ServerCfg server) {
            this.server = server;
        }
    }

    public static class QueueCfg extends POJO {
        private String name;
        private boolean durable = true;

        public boolean isDurable() {
            return durable;
        }

        public void setDurable(boolean durable) {
            this.durable = durable;
        }

        public String getName() {

            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonManagedReference
        private Set<ExchangeCfg> exchanges = new HashSet<>();

        public Set<ExchangeCfg> getExchanges() {
            return exchanges;
        }

        public void setExchanges(Set<ExchangeCfg> exchanges) {
            this.exchanges = exchanges;
        }

        private ServerCfg server;

        public ServerCfg getServer() {
            return server;
        }

        public void setServer(ServerCfg server) {
            this.server = server;
        }
    }

    @Test
    public void testComplexBeanRelation() {
        ServerCfg sc = new ServerCfg();
        sc.setUrl("amqp://snowball:5672");

        ExchangeCfg ec = new ExchangeCfg();
        ec.setName("image_processing_exchange");
        ec.setServer(sc);
        ec.setType("direct");

        QueueCfg qc = new QueueCfg();
        qc.setName("image_processing_queue");
        qc.setServer(sc);
        qc.setDurable(true);

        ec.getQueues().add(qc);

        qc.getExchanges().add(ec);

        System.out.println(Jsons.toString(qc));
    }

    @Test
    public void testBeanDescription() {
        SerializationConfig cfg = Jsons.MAPPER.getSerializationConfig();
        {
            BeanDescription beanDescription = cfg.introspect(cfg.constructType(QueueCfg.class));
            List<BeanPropertyDefinition> propList = beanDescription.findProperties();
//        propList.forEach(System.out::println);

            for (BeanPropertyDefinition bpd : propList) {
                System.out.println(bpd.getGetter().getType().isCollectionLikeType());
            }
        }

        {
            BeanDescription beanDescription = cfg.introspect(cfg.constructType(QueueCfg.class));
            List<BeanPropertyDefinition> propList = beanDescription.findProperties();
//        propList.forEach(System.out::println);

            for (BeanPropertyDefinition bpd : propList) {
                System.out.println(bpd.getGetter().getType().isCollectionLikeType());
            }
        }
    }

    @Test
    public void testBeanInArray() {
        ServerCfg sc = new ServerCfg();
        sc.setUrl("amqp://snowball:5672");

        ServerCfg[] scs = new ServerCfg[] {sc, sc, sc};

        SerializationConfig cfg = Jsons.MAPPER.getSerializationConfig();
        BeanDescription beanDescription = cfg.introspect(cfg.constructType(scs.getClass()));

        if (beanDescription.getType().isArrayType()) {
            ArrayType at = (ArrayType) beanDescription.getType();
            beanDescription = cfg.introspect(at.getContentType());
            List<BeanPropertyDefinition> propList = beanDescription.findProperties();
            for (BeanPropertyDefinition bpd : propList) {
                System.out.println(bpd.getGetter().getType().isCollectionLikeType());
            }
            return;
        }
    }

    @Test
    public void testGenericType() {
        List<ServerCfg> scList = new ArrayList<>();
        SerializationConfig cfg = Jsons.MAPPER.getSerializationConfig();
        JavaType listType = cfg.constructType(scList.getClass());
        System.out.println(listType);

        BeanDescription beanDescription = cfg.introspect(cfg.constructType(QueueCfg.class));

        AnnotatedMethod getExchanges = beanDescription.findMethod("getExchanges", null);
        System.out.println(getExchanges);

        System.out.println(getExchanges.getRawReturnType());
        JavaType setType = getExchanges.getType();
        System.out.println(setType);
    }


}
