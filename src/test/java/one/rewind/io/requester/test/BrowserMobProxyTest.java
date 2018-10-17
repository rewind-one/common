package one.rewind.io.requester.test;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class BrowserMobProxyTest {

	private static final Logger logger = LogManager.getLogger(BrowserMobProxyTest.class.getName());

	BrowserMobProxy bmProxy;
	int proxyPort;

	@Before
	public void buildProxy() {


		CertificateAndKeySource source =
				new PemFileCertificateSource(new File("ca.crt"), new File("pk.crt"), "sdyk");

		// tell the MitmManager to use the root certificate we just generated
		ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
				.rootCertificateSource(source)
				.build();

		bmProxy = new BrowserMobProxyServer();
		bmProxy.setTrustAllServers(true);
		bmProxy.setMitmManager(mitmManager);
		bmProxy.start(0);
		proxyPort = bmProxy.getPort();

		logger.info("Browser Mob Proxy started @port {}", proxyPort);

		/*RequestFilter filter = (request, contents, messageInfo) -> {

			logger.info(messageInfo.getOriginalUrl());
			logger.info(contents.getTextContents());

			return null;
		};

		bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));

		bmProxy.addResponseFilter((response, contents, messageInfo) -> {

			logger.info(messageInfo.getOriginalUrl());
			logger.info(contents.getTextContents());
		});*/
	}

	@Test
	public void test() throws MalformedURLException, URISyntaxException {

		Task t = new Task("http://www.baidu.com");

		t.setProxy(new ProxyImpl("127.0.0.1", proxyPort, "", ""));

		BasicRequester.getInstance().submit(t);
	}
}
