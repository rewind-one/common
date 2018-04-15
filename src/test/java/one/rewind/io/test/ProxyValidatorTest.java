package one.rewind.io.test;

import org.junit.Test;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;

public class ProxyValidatorTest {

	@Test
	public void testProxy() {

		ProxyWrapper pw = new ProxyWrapperImpl("10.0.0.51", 49999, null, null);

	}
}
