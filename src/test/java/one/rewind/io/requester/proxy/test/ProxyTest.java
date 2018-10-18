package one.rewind.io.requester.proxy.test;

import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ProxyTest {

	List<Proxy> proxies = new ArrayList<>();

	@Before
	public void setup() {

		proxies.add(new ProxyImpl("uml.ink", 60201, "tfelab", "TfeLAB2@15"));
		proxies.add(new ProxyImpl("uml.ink", 60204, "tfelab", "TfeLAB2@15"));
		proxies.add(new ProxyImpl("uml.ink", 60205, "tfelab", "TfeLAB2@15"));
		proxies.add(new ProxyImpl("uml.ink", 60206, "tfelab", "TfeLAB2@15"));

	}

	@Test
	public void validate() {

		proxies.stream().forEach(p -> {
			p.validate();
		});

	}
}
