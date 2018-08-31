package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.action.*;
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
	public void testTianyancha() throws Exception {

		ChromeTask t = new ChromeTask("https://www.tianyancha.com/");

		Proxy proxy = new ProxyImpl("sdyk.red", 60202, "tfelab", "TfeLAB2@15");
		//ProxyImpl proxy1 = new ProxyImpl( "sdyk.red", 60202, "tfelab", "TfeLAB2@15");
		//Proxy proxy = new ProxyImpl("tpda.cc", 60202, "sdyk", "sdyk");

		ChromeDriverAgent agent = new ChromeDriverAgent(proxy, ChromeDriverAgent.Flag.MITM);

		agent.start();

		agent.addTerminatedCallback((a)->{
			System.err.println("TERMINATED");
		});

		agent.submit(t);

		Thread.sleep(1000000);

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

			ChromeAction action = new LoginWithGeetestAction().setAccount(account);

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

	@Test
	public void testResponseFilter() throws Exception {

		String url = "https://www.mihuashi.com/users/Nianless?role=employer";

		//Proxy proxy = new ProxyImpl("10.0.0.56", 49999, null, null);
		ChromeDriverAgent agent = new ChromeDriverAgent(ChromeDriverAgent.Flag.MITM);

		agent.start();

		ChromeTask task = new ChromeTask(url);

		task.addAction(new ClickAction("#users-show > div.container-fluid > div.profile__container > main > header > ul > li:nth-child(2) > a", 1000));

		task.addAction(new LoadMoreContentAction("#vue-comments-app > div:nth-child(2) > a > span:nth-child(1)"));

		task.setResponseFilter((response, contents, messageInfo) -> {

			if(messageInfo.getOriginalUrl().matches(".*?/users/Nianless/comments\\?role=employer&per=\\d+&page=\\d+")) {
				task.getResponse().setVar("content",
						task.getResponse().getVar("content") == null?
								contents.getTextContents() :
								task.getResponse().getVar("content") + "\n" + contents.getTextContents());
			}
		});

		agent.submit(task);

		System.err.println(task.getResponse().getVar("content"));

		Thread.sleep(10000000);
	}
}