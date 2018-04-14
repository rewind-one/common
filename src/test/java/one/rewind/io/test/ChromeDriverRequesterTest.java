package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import org.junit.Test;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.proxy.ProxyAuthenticator;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;

import java.net.*;

import static one.rewind.io.requester.chrome.ChromeDriverRequester.buildBMProxy;

public class ChromeDriverRequesterTest {

	@Test
	public void basicTest() throws MalformedURLException, URISyntaxException, InterruptedException {

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		for(int i=0; i<4; i++) {
			requester.addChromeDriverAgent(new ChromeDriverAgent(null));
		}

		requester.layout();

		for(int i=0; i<100; i++) {

			Task task = new Task("http://www.baidu.com/s?word=" + (1950 + i));
			requester.submit(task);
		}

		Thread.sleep(60000);

		requester.close();
	}

	@Test
	public void proxyTest() throws MalformedURLException, URISyntaxException, InterruptedException {

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		ProxyWrapper proxy = new ProxyWrapperImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		requester.addChromeDriverAgent(new ChromeDriverAgent(proxy));

		proxy = new ProxyWrapperImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");
		requester.addChromeDriverAgent(new ChromeDriverAgent(proxy));

		proxy = new ProxyWrapperImpl("118.190.133.34", 59998, "tfelab", "TfeLAB2@15");
		requester.addChromeDriverAgent(new ChromeDriverAgent(proxy));

		proxy = new ProxyWrapperImpl("118.190.44.184", 59998, "tfelab", "TfeLAB2@15");
		requester.addChromeDriverAgent(new ChromeDriverAgent(proxy));

		requester.layout();

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<100; i++) {

			Task task = new Task("http://www.baidu.com/s?word=ip");
			requester.submit(task);
		}

		Thread.sleep(60000);

		requester.close();
	}

	@Test
	public void testBuildProxyServer() throws InterruptedException, UnknownHostException {

		ProxyWrapper proxy = new ProxyWrapperImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		BrowserMobProxyServer ps = buildBMProxy(proxy);
		System.err.println(ps.getClientBindAddress());
		System.err.println(ps.getPort());
		Thread.sleep(100000);
	}
}
