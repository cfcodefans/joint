import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.commons.ProcTrace;
import org.junit.Test;

/**
 * Created by fan on 2016/12/4.
 */
public class ProcTraceTests {

    private static final Logger log = LogManager.getLogger(ProcTraceTests.class);

    public static void easySleep(long i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNormalCase() {
        ProcTrace.start();

        {
            ProcTrace.start("begin some lengthy process");
            easySleep(500);
            ProcTrace.ongoing("step 1");
            {
                ProcTrace.start("doing something else");
                easySleep(400);
                ProcTrace.ongoing("step 1.1");
                easySleep(300);
                ProcTrace.ongoing("step 1.2");
                easySleep(200);
                ProcTrace.ongoing("step 1.3");
                easySleep(100);
                ProcTrace.end();
            }
            ProcTrace.end();
        }
        ProcTrace.end();

        log.info(ProcTrace.flush());
    }

    @Test
    public void testStartToEnd() {
        ProcTrace.start("Start to end");
        easySleep(300);
        ProcTrace.end();
        log.info(ProcTrace.flush());
    }

    @Test
    public void testNestedStartToEnd() {
        ProcTrace.start("Start to end 1");
        easySleep(300);
        {
            ProcTrace.start("Start to end 2");
            easySleep(200);
            {
                ProcTrace.start("Start to end 3");
                easySleep(100);
                ProcTrace.end();
            }
            ProcTrace.end();
        }
        ProcTrace.end();
        log.info(ProcTrace.flush());
    }

    @Test
    public void testNestedStartToMissingEnd() {
        ProcTrace.start("Start to end 1");
        easySleep(300);
        {
            ProcTrace.start("Start to end 2");
            easySleep(200);
            {
                ProcTrace.start("Start to end 3");
                easySleep(100);
                ProcTrace.ongoing("doing something");
                easySleep(50);
                ProcTrace.ongoing("doing something");
            }
        }
//        ProcTrace.end();
//        log.info(ProcTrace.flush());
        log.info(ProcTrace.endAndFlush());
    }

    @Test
    public void testThrownException() {
        ProcTrace.TraceEntry start = null;
        ProcTrace.start("everything alright");
        easySleep(200);
        ProcTrace.ongoing("step 1");
        try {
            {start = ProcTrace.start("something alright");
                {easySleep(200);
                    ProcTrace.ongoing("step 1.1");
                    ProcTrace.start("nothing alright");
                    {
                        easySleep(200);
                        ProcTrace.ongoing("step 1.1.1");
                        if (true)throw new Exception("what happened?");
                    }
                    ProcTrace.end();
                }
                ProcTrace.end();
            }
            ProcTrace.end();
        } catch (Exception e) {
            ProcTrace.end(start, e);
        }
        ProcTrace.ongoing("finished");
        ProcTrace.end();

        log.info(ProcTrace.flush());
    }

    @Test
    public void testNestedException() {
        ProcTrace.TraceEntry start = null;

        ProcTrace.start("layer 1"); {
            ProcTrace.start("layer 2"); {
                ProcTrace.start("layer 3"); {
                    start = ProcTrace.start("layer 4"); try {
                        ProcTrace.start("layer 5"); {
                            ProcTrace.start("layer 6"); {
                                ProcTrace.ongoing("something 1");
                                ProcTrace.ongoing("something 2");
                                ProcTrace.ongoing("something 3");
                                if (true)throw new Exception("what happened?");
                                ProcTrace.ongoing("something 4");
                                ProcTrace.ongoing("something 5");
                                ProcTrace.ongoing("something 6");
                            } ProcTrace.end();
                        } ProcTrace.end();
                    } catch (Exception e) {ProcTrace.end(start, e);}
                    ProcTrace.ongoing("after exception");
                } ProcTrace.end();
            } ProcTrace.end();
        }
        ProcTrace.end();
        log.info("\n" + ProcTrace.flush());
    }
}
