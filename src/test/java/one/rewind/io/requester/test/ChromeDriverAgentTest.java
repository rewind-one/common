package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.chrome.action.PostAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.ChromeTask;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by karajan on 2017/6/3.
 */
public class ChromeDriverAgentTest {

	@Test
	public void test() throws Exception {

		ChromeTask t = new ChromeTask("https://www.zbj.com/");

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

		BrowserMobProxyServer ps = ChromeDriverDistributor.buildBMProxy(null);

		System.err.println(ps.getPort());

	}

	@Test
	public void loginTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<10; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			agent.start();

			ChromeTask task = new ChromeTask("http://www.zbj.com");
			ChromeAction action = new LoginWithGeetestAction(account);
			task.addAction(action);
			agent.submit(task);

			Thread.sleep(10000);

			agent.stop();
		}

	}

	@Test
	public void requesterFilterTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		final ChromeTask t = new ChromeTask("https://beijing.zbj.com/").setNoFetchImages();
		ChromeTask t2 = new ChromeTask("https://www.baidu.com/s?word=ip").setNoFetchImages();
		/*t.addDoneCallback((task)->{
			System.err.println("Done!");
			System.err.println(task.getResponse().getVar("test"));
		});*/

		/*t.setResponseFilter((response, contents, messageInfo) -> {
			if(messageInfo.getOriginalUrl().contains("tu_329aca4.js")) {
				t.getResponse().setVar("test", contents.getTextContents());
			}
		});*/

		Proxy proxy = new ProxyImpl("10.0.0.56", 49999, null, null);
		ChromeDriverAgent agent = new ChromeDriverAgent(proxy, ChromeDriverAgent.Flag.MITM);
		agent.start();

		agent.addTerminatedCallback((a)->{
			System.err.println("TERMINATED");
		});

		agent.submit(t2);
		//agent.submit(t2);
		//agent.submit(t);
		//agent.submit(t2);
		//agent.submit(t);
		//agent.submit(t2);

		System.err.println("Proxy\tbyte send:" + agent.proxy.bytes_send + "\tbyte rev:" + agent.proxy.bytes_rev);
		Thread.sleep(10000);

		agent.stop();
	}

	@Test
	public void testGetInterval() throws MalformedURLException, URISyntaxException, NoSuchFieldException, IllegalAccessException {

		ChromeTask task = new TestChromeTask.T1("http://www.baidu.com");

		long min_interval = task.getClass().getField("MIN_INTERVAL").getLong(task.getClass());

		System.err.println(min_interval);
	}

	@Test
	public void testPostRequest() throws Exception {

		String url = "https://www.jfh.com/jfhrm/buinfo/showbucaseinfo";
		Map<String, String> data = ImmutableMap.of("uuidSecret", "MTQxODM7NDQ%3D");

		ChromeDriverAgent agent = new ChromeDriverAgent();

		agent.start();

		ChromeTask task = new ChromeTask(url);
		task.addAction(new PostAction(url, data));
		agent.submit(task);
	}
}