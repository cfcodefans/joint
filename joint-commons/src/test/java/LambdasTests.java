import org.joints.commons.Lambdas;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Created by fan on 2016/11/16.
 */
public class LambdasTests {

	public void iex(Integer i) throws Exception {
		if (i > 35) throw new Exception("too old");
	}

	@Test
	public void testLambda() {
		IntStream.range(0, 40).mapToObj(Integer::valueOf).forEach(Lambdas.wc(this::iex));
	}
}
