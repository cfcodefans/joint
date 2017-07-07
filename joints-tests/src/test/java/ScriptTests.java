import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.server.model.Resource;
import org.joints.rest.script.ScriptResLoader;
import org.junit.Assert;
import org.junit.Test;
import scala.collection.JavaConverters;
import scala.reflect.internal.util.BatchSourceFile;
import scala.reflect.internal.util.ScalaClassLoader;
import scala.reflect.internal.util.SourceFile;
import scala.reflect.io.VirtualFile;
import scala.tools.nsc.Global;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.Scripted;
import scala.tools.nsc.settings.MutableSettings;

import javax.script.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

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
//        System.out.println(MiscUtils.lineNumber(loadResAsString));

        ScriptContext sc = new SimpleScriptContext();
        sc.setAttribute("CLASS_LOADER", Thread.currentThread().getContextClassLoader(), ScriptContext.GLOBAL_SCOPE);
        sc.setWriter(new PrintWriter(System.out));
        se.setContext(sc);

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
            Class clz = (Class) evaluated;
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

    @Test
    public void testScalaScript() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("scala");

//        SimpleScriptContext _sc = new SimpleScriptContext();
//        _sc.getBindings(ScriptContext.ENGINE_SCOPE).put("app", this);
//        se.setContext(_sc);

        se.put("app", this);
        se.put("now", this.now);

        se.getContext().setWriter(new PrintWriter(System.out));

        String loadResAsString = FileUtils.readFileToString(new File("src/test/resources/script.scala.code"), Charset.defaultCharset());

//        System.out.println(se.eval(loadResAsString));
        System.out.println(se.eval("now.toString"));
    }

    static class ScriptClassLoader extends ClassLoader implements ScalaClassLoader {
        public ScriptClassLoader(ClassLoader cl) {
            super(cl);
        }
    }

    @Test
    public void testScalaResWithSpecialClassLoader() throws Exception {

        Thread t = Thread.currentThread();
        ClassLoader _cl = t.getContextClassLoader();

        try {
            ScriptClassLoader scl = new ScriptClassLoader(_cl);
            t.setContextClassLoader(scl);

            ScriptEngineManager sem = new ScriptEngineManager();
            ScriptEngine se = sem.getEngineByExtension("scala");
            Scripted sed = (Scripted) se;
            IMain intp = sed.intp();

            intp.settings().embeddedDefaults(scl);
            System.out.println(intp.settings().usejavacp());

//            intp.settings().usejavacp().
//            intp.isettings().allSettings().
            String loadResAsString = FileUtils.readFileToString(new File("src/test/resources/rest-tests.scala"), Charset.defaultCharset());
//        System.out.println(MiscUtils.lineNumber(loadResAsString));

            Set<Resource> resourceSet = tryEval(se, loadResAsString);
            System.out.println(resourceSet);
            intp.close();
        } finally {
            t.setContextClassLoader(_cl);
        }
    }

    public Date now = new Date();

    @Test
    public void testScalaResUsingJavaClassLoader() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("scala");
        Scripted sed = (Scripted) se;
        IMain intp = sed.intp();

        MutableSettings.BooleanSetting usejavacp = (MutableSettings.BooleanSetting) intp.settings().usejavacp();
        usejavacp.v_$eq(true);
        System.out.println(usejavacp);

//            intp.settings().usejavacp().
//            intp.isettings().allSettings().
        String loadResAsString = FileUtils.readFileToString(new File("src/test/resources/rest-tests.scala.code"), Charset.defaultCharset());
//        System.out.println(MiscUtils.lineNumber(loadResAsString));

        Set<Resource> resourceSet = tryEval(se, loadResAsString);
        System.out.println(resourceSet);
        intp.close();
    }

    @Test
    public void testNashorn() throws ScriptException {
        NashornScriptEngineFactory nsef = new NashornScriptEngineFactory();
        NashornScriptEngine nse = (NashornScriptEngine) nsef.getScriptEngine();

        nse.getContext().setWriter(new PrintWriter(System.out));
        nse.put("now", new Date());
        nse.eval("print(now);");
    }

    @Test
    public void testCompiler() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByExtension("scala");
        Scripted sed = (Scripted) se;
        IMain iMain = new IMain(sed.intp().settings(), new PrintWriter(System.out)); //((Scripted) se).intp();
        String loadResAsString = FileUtils.readFileToString(new File("src/test/resources/ScalaRes.scala"), Charset.defaultCharset());

        Global.Run run = iMain.compileSourcesKeepingRun(
            JavaConverters.<SourceFile>asScalaBuffer(Arrays.<SourceFile>asList(
                new BatchSourceFile(
                    new VirtualFile("src/main/webapp/WEB-INF/scripts/ScalaRes.scala", "./"), loadResAsString.toCharArray())))
        )._2();
        Assert.assertNotNull(run);

        Class<?> scalaRes = iMain.classLoader().findClass("ScalaRes");
        Assert.assertNotNull(scalaRes);

        System.out.println(iMain.classLoader().classNameToPath("ScalaRes"));
        Assert.assertNotNull(Class.forName("ScalaRes"));
    }
}
