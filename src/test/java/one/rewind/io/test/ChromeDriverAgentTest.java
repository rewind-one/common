package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * Created by karajan on 2017/6/3.
 */
public class ChromeDriverAgentTest {

	@Test
	public void test() throws Exception {

		Task t = new Task("https://www.zbj.com/");

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, null, null);
		//Proxy proxy = new ProxyImpl("tpda.cc", 60202, "sdyk", "sdyk");

		ChromeDriverAgent agent = new ChromeDriverAgent(proxy, ChromeDriverAgent.Flag.MITM);

		agent.start();

/*		agent.setIdleCallback(()->{
			System.err.println("IDLE");
		});*/

		agent.addTerminatedCallback((a)->{
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
	public void loginTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<10; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			agent.start();

			Task task = new Task("http://www.zbj.com");
			ChromeAction action = new LoginWithGeetestAction(account);
			task.addAction(action);
			agent.submit(task);

			Thread.sleep(10000);

			agent.stop();
		}

	}

	@Test
	public void requesterFilterTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		final Task t = new Task("https://www.baidu.com/");
		t.addDoneCallback((task)->{
			System.err.println("Done!");
			System.err.println(task.getResponse().getVar("test"));
		});

		t.setResponseFilter((response, contents, messageInfo) -> {
			if(messageInfo.getOriginalUrl().contains("tu_329aca4.js")) {
				t.getResponse().setVar("test", contents.getTextContents());
			}
		});

		// Proxy proxy = new ProxyImpl("10.0.0.51", 49999, null, null);
		ChromeDriverAgent agent = new ChromeDriverAgent(ChromeDriverAgent.Flag.MITM);
		agent.start();

		/*agent.setIdleCallback(()->{
			System.err.println("IDLE");
		});*/

		agent.addTerminatedCallback((a)->{
			System.err.println("TERMINATED");
		});

		agent.submit(t);

		/*Task t1 = new Task("https://www.jd.com/");
		t1.setResponseFilter((response, contents, messageInfo) -> {
			System.out.println("R2");
		});

		agent.submit(t);*/

		Thread.sleep(10000);

		agent.stop();
	}
}