import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.glassfish.jersey.server.model.Resource;
import org.joints.rest.script.ScriptResLoader;
import scala.collection.JavaConverters;
import scala.reflect.internal.util.ScalaClassLoader;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.Scripted;
import scala.tools.nsc.settings.MutableSettings;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.FileFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                URLClassLoader ucl = (URLClassLoader) _cl;
                List<URL> clzPathList = Arrays.asList(ucl.getURLs()).stream()
                    .filter((URL url) -> {
                        try {
                            return Paths.get(url.toURI()).toFile().isFile();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
                intp.addUrlsToClassPath(JavaConverters.asScalaBuffer(clzPathList));
            }
            intp.settings().embeddedDefaults(scl);
            MutableSettings.BooleanSetting useJavaClassPath = (MutableSettings.BooleanSetting) intp.settings().usejavacp();
            useJavaClassPath.v_$eq(true);

            return super.tryEvalForResources(se, scriptStr);
        } finally {
            t.setContextClassLoader(_cl);
            intp.close();
        }
    }
}
