# some basic utility classes

# org.joints.commons.Lambdas
## A wrapper for Java 8 lambda expression as default lambda functional interface doesn't define exception clause

```java
public class LambdasTests {

	public void iex(Integer i) throws Exception {
		if (i > 35) throw new Exception("too old");
	}

	@Test
	public void testLambda() {
		IntStream.range(0, 40).mapToObj(Integer::valueOf).forEach(Lambdas.wc(this::iex));
	}
}
```

# org.joints.commons.ProcTrace 
## A tool class for tracing code execution and log time for each step

The class will record the time and position of each "ongoing" method call.
<table>
<tr>
<td>
   <pre lang="java">
       @Test
    public void testNormalCase() {
        ProcTrace.start();
        {
            ProcTrace.start("begin some lengthy process");
            doSomething(500);
            ProcTrace.ongoing("step 1");
            {
                ProcTrace.start("doing something else");
                doSomething(400);
                ProcTrace.ongoing("step 1.1");
                doSomething(300);
                ProcTrace.ongoing("step 1.2");
                doSomething(200);
                ProcTrace.ongoing("step 1.3");
                doSomething(100);
                ProcTrace.end();
            }
            ProcTrace.end();
        }
        ProcTrace.end();
        log.info(ProcTrace.flush());
    }
   </pre>
</td>
<td>
  <pre>
          1504 ms:	begin some lengthy process
            500 ms:	step 1
            
            1002 ms:	doing something else
                400 ms:	step 1.1
                300 ms:	step 1.2
                200 ms:	step 1.3
                102 end
  </pre>
</td>
</tr>
<tr>
<td>
   <pre lang="java">
           ProcTrace.start("Start to end 1");
        doSomething(300);
        {
            ProcTrace.start("Start to end 2");
            doSomething(200);
            {
                ProcTrace.start("Start to end 3");
                doSomething(100);
                ProcTrace.end();
            }
            ProcTrace.end();
        }
        ProcTrace.end();
        log.info(ProcTrace.flush());
   </pre>
   </td>
   <td>
  <pre>
616 ms:	Start to end 1
        
        305 ms:	Start to end 2
            
            102 ms:	Start to end 3
                102 end
  </pre>
</td>
</tr>
<tr>
<td>
   <pre lang="java">
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
   </pre>
   </td>
   <td>
  <pre>
4 ms:	layer 1
        4 ms:	layer 2
            4 ms:	layer 3
                4 ms:	layer 4
                    4 ms:	layer 5
                        2 ms:	layer 6
                            0 ms:	something 1
                            0 ms:	something 2
                            0 ms:	something 3
                            2 end
                    0 ms:	Exception: what happened?
                0 ms:	after exception
  </pre>
</td>
</tr>
</table>

