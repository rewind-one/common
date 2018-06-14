package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static one.rewind.io.requester.chrome.ChromeDriverRequester.buildBMProxy;

public class ChromeDriverRequesterTest {

	@Test
	public void basicTest() throws MalformedURLException, URISyntaxException, InterruptedException, ChromeDriverException.IllegalStatusException {

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		for(int i=0; i<4; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			requester.addAgent(agent);
		}

		requester.layout();

		for(int i=0; i<100; i++) {

			Task task = new Task("http://www.baidu.com/s?word=" + (1950 + i));
			task.addDoneCallback((t) -> {
				System.err.println(t.getUrl() + " -- " + t.getResponse().getSrc().length);
			});
			requester.submit(task);
		}

		Thread.sleep(60000);

		requester.close();
	}

	@Test
	public void proxyTest() throws MalformedURLException, URISyntaxException, InterruptedException, ChromeDriverException.IllegalStatusException {

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent1 = new ChromeDriverAgent(proxy);
		requester.addAgent(agent1);

		proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent2 = new ChromeDriverAgent(proxy);
		requester.addAgent(agent2);
		proxy = new ProxyImpl("118.190.133.34", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent3 = new ChromeDriverAgent(proxy);
		requester.addAgent(agent3);

		proxy = new ProxyImpl("118.190.44.184", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent4 = new ChromeDriverAgent(proxy);
		requester.addAgent(agent4);

		requester.layout();

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<10000; i++) {

			Task task = new Task("http://www.baidu.com/s?word=ip");
			requester.submit(task);
		}

		Thread.sleep(60000);

		requester.close();
	}

	@Test
	public void ExceptionTest() throws MalformedURLException, URISyntaxException, InterruptedException, ChromeDriverException.IllegalStatusException {

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		for(int i=0; i<1; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			requester.addAgent(agent);
		}

		requester.layout();

		for(int i=0; i<1; i++) {

			Task task = new Task("http://www.baidu.com/s?word=" + (1950 + i));

			task.addDoneCallback((t) -> {
				System.err.println(t.getUrl() + " -- " + t.getResponse().getSrc().length);
			});

			requester.submit(task);
		}

		Thread.sleep(60000);

		requester.close();
	}

	@Test
	public void testBuildProxyServer() throws InterruptedException, UnknownHostException {

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		BrowserMobProxyServer ps = buildBMProxy(proxy);
		System.err.println(ps.getClientBindAddress());
		System.err.println(ps.getPort());
		Thread.sleep(100000);
	}
}
