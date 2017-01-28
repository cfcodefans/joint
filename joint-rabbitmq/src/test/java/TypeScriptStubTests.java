import entity.QueueCfg;
import org.joints.rest.ajax.TypeScriptStubs;
import org.junit.Test;

import java.util.Map;

/**
 * Created by fan on 2017/1/24.
 */
public class TypeScriptStubTests {
    @Test
    public void testQueueCfg() {

        Map<Class, String> classAndTypeSources = TypeScriptStubs.getBasicTypesAndTypeSources();
        Map<Class, String> classAndTypeNames = TypeScriptStubs.getBasicTypesAndTypeSources();
        String src = TypeScriptStubs.classToTypeScriptDef(QueueCfg.class, classAndTypeSources, classAndTypeNames);
        classAndTypeSources.entrySet().stream()
            .forEach((Map.Entry<Class, String> en) -> System.out.printf("\n%s -> %s\n", en.getKey(), en.getValue()));

    }
}
