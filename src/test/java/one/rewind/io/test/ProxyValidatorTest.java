package one.rewind.io.test;

import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Test;

public class ProxyValidatorTest {

	@Test
	public void testProxy() {

		Proxy pw = new ProxyImpl("10.0.0.51", 49999, null, null);

	}
}
