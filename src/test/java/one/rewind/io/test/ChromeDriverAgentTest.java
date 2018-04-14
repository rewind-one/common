package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;
import org.junit.Test;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by karajan on 2017/6/3.
 */
public class ChromeDriverAgentTest {

	@Test
	public void test() throws Exception {

		Task t = new Task("https://www.google.com/");

		ProxyWrapper proxy = new ProxyWrapperImpl("scisaga.net", 60103, null, null);

		ChromeDriverAgent agent = new ChromeDriverAgent(proxy, ChromeDriverAgent.Flag.MITM);

/*		agent.setIdleCallback(()->{
			System.err.println("IDLE");
		});*/

		agent.setTerminatedCallback(()->{
			System.err.println("TERMINATED");
		});

		agent.submit(t);

		agent.stop();

	}

	@Test
	public void testBuildProxy() {

		BrowserMobProxyServer ps = ChromeDriverRequester.buildBMProxy(null);

		System.err.println(ps.getPort());

	}

	@Test
	public void loginTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException {

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<1; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent(null);
			Task task = new Task("http://www.zbj.com");
			ChromeAction action = new LoginWithGeetestAction(account);
			task.addAction(action);
			agent.submit(task);

			agent.stop();
		}

	}
}