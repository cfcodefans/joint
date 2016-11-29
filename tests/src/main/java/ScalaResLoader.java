import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.glassfish.jersey.server.model.Resource;
import org.joints.rest.script.ScriptResLoader;
import scala.collection.JavaConverters;
import scala.reflect.internal.util.ScalaClassLoader;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.Scripted;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.FileFilter;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by fan on 2016/11/29.
 */
public class ScalaResLoader extends ScriptResLoader {

    public FileFilter getFileFilter() {
        return new OrFileFilter(Arrays.asList(
            new WildcardFileFilter("*.js")
            ,new WildcardFileFilter("*.scala")
        ));
    }

    protected static class ScriptClassLoader extends ClassLoader implements ScalaClassLoader {
        public ScriptClassLoader(ClassLoader cl) {
            super(cl);
        }
    }

    protected Set<Resource> tryEvalForResources(ScriptEngine se, String scriptStr) throws ScriptException {
        if (!(se instanceof Scripted)) {
            return super.tryEvalForResources(se, scriptStr);
        }

        Scripted sed = (Scripted) se;
        Thread t = Thread.currentThread();
        ClassLoader _cl = t.getContextClassLoader();

        IMain intp = sed.intp();
        try {
            ScriptClassLoader scl = new ScriptClassLoader(_cl);
            t.setContextClassLoader(scl);

            if (_cl instanceof URLClassLoader) {
                URLClassLoader ucl = (ScalaClassLoader.URLClassLoader) _cl;
                intp.addUrlsToClassPath(JavaConverters.asScalaBuffer(Arrays.asList(ucl.getURLs())));
            }
            intp.settings().embeddedDefaults(scl);
//            MutableSettings.BooleanSetting useJavaClassPath = (MutableSettings.BooleanSetting) intp.settings().usejavacp();
//            useJavaClassPath.v_$eq(true);

            return super.tryEvalForResources(se, scriptStr);
        } finally {
            t.setContextClassLoader(_cl);
            intp.close();
        }
    }
}
