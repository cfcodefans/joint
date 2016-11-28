import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.server.model.Resource;
import org.joints.commons.MiscUtils;
import org.joints.rest.script.ScriptResLoader;
import org.junit.Test;

import javax.script.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

;

/**
 * Created by fan on 2016/11/28.
 */
public class ScriptTests {
    @Test
    public void testScalaRes() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("scala");
        se.getContext().setWriter(new PrintWriter(System.out));

        String loadResAsString = FileUtils.readFileToString(new File("src/test/resources/rest-tests.scala.code"), Charset.defaultCharset());
        System.out.println(MiscUtils.lineNumber(loadResAsString));

        Set<Resource> resourceSet = tryEval(se, loadResAsString);
        System.out.println(resourceSet);
    }

    public static Set<Resource> tryEval(ScriptEngine se, String scriptStr) throws ScriptException {
        Object evaluated = null;
        if (se instanceof Compilable) {
            Compilable cpl = (Compilable) se;
            evaluated = cpl.compile(scriptStr).eval();
        } else {
            evaluated = se.eval(scriptStr);
        }

        if (evaluated instanceof Class) {
            Class clz = (Class)evaluated;
            Resource res = Resource.from(clz);
            return new HashSet<Resource>(Arrays.asList(res));
        }

        if (evaluated instanceof Resource) {
            Resource res = (Resource) evaluated;
            return new HashSet<Resource>(Arrays.asList(res));
        }

        if (evaluated instanceof Set) {
            Set set = (Set) evaluated;
            if (CollectionUtils.isEmpty(set)) {
                return Collections.emptySet();
            }

            return (Set<Resource>) set.stream().filter(Objects::nonNull).map(obj -> {
                if (obj instanceof Resource) return (Resource) obj;
                if (obj instanceof Class) return Resource.from((Class) obj);
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }

        if (evaluated instanceof ScriptResLoader.IResourceGenerator) {
            ScriptResLoader.IResourceGenerator resGen = (ScriptResLoader.IResourceGenerator) evaluated;
            return resGen.apply(null);
        }

        if (!(se instanceof Invocable)) {
            return Collections.emptySet();
        }

        Invocable inv = (Invocable) se;
        ScriptResLoader.IResourceGenerator resGen = inv.getInterface(ScriptResLoader.IResourceGenerator.class);
        if (resGen == null) {
            return Collections.emptySet();
        }
        return resGen.apply(null);
    }
}
