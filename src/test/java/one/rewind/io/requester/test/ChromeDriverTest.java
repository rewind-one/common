package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.chrome.action.*;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.chrome.ChromeTask;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;

import static one.rewind.io.requester.chrome.ChromeDistributor.buildBMProxy;

/**
 * ChromeDriverAgent 测试
 * Created by karajan on 2017/6/3.
 */
public class ChromeDriverTest {

	@Before
	public void setup() {

		// https://stackoverflow.com/questions/6909581/java-library-path-setting-programmatically
		try {
			System.setProperty("java.library.path", "C:\\App\\opencv\\build\\java\\x64;C:\\App\\opencv\\build\\x64\\vc15\\bin");
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 使用代理 + 设置Agent 回调
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {

		ChromeTask t = new ChromeTask("https://www.zbj.com/");

		Proxy proxy = new ProxyImpl("uml.ink", 60201, null, null);

		ChromeAgent agent = new ChromeAgent(proxy, ChromeAgent.Flag.MITM);

		agent.start();

		/*agent.setIdleCallback(()->{
			System.err.println("IDLE");
		});*/

		agent.addTerminatedCallback((a)->{
			System.err.println("TERMINATED");
		});

		agent.submit(t);

		agent.stop();

	}

	/**
	 * 登陆测试
	 * TODO 应该使用微博或百度作为例子
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws ChromeDriverException.IllegalStatusException
	 * @throws InterruptedException
	 */
	@Test
	public void loginTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");
		ChromeAgent agent = new ChromeAgent();
		agent.start();

		for(int i=0; i<10; i++) {

			ChromeTask task = new ChromeTask("http://www.zbj.com");

			ChromeAction action1 = new ClickAction("#headerTopWarpV1 > div > div > ul > li.item.J_user-login-status > div > span > a.J_header-top-fromurl.header-top-login");
			task.addAction(action1);

			ChromeAction action = new LoginWithGeetestAction().setAccount(account);
			task.addAction(action);

			agent.submit(task);

			Thread.sleep(10000);
		}

		agent.stop();

	}

	/**
	 * ChromeDriver也可以提交Post任务
	 * @throws Exception
	 */
	@Test
	public void testPostRequest() throws Exception {

		String url = "https://www.jfh.com/jfhrm/buinfo/showbucaseinfo";
		Map<String, String> data = ImmutableMap.of("uuidSecret", "MTQxODM7NDQ%3D");

		ChromeAgent agent = new ChromeAgent();
		agent.start();

		ChromeTask task = new ChromeTask(url);
		task.addAction(new PostAction(url, data));
		agent.submit(task);
	}

	/**
	 * 测试增加请求过滤器
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws ChromeDriverException.IllegalStatusException
	 * @throws InterruptedException
	 */
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
		ChromeAgent agent = new ChromeAgent(proxy, ChromeAgent.Flag.MITM);
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

	/**
	 * 测试返回过滤器
	 * @throws Exception
	 */
	@Test
	public void testResponseFilter() throws Exception {

		String url = "https://www.mihuashi.com/users/Nianless?role=employer";

		//Proxy proxy = new ProxyImpl("10.0.0.56", 49999, null, null);
		ChromeAgent agent = new ChromeAgent(ChromeAgent.Flag.MITM);

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

	/**
	 * 测试创建代理服务器
	 * @throws InterruptedException
	 * @throws UnknownHostException
	 */
	@Test
	public void testBuildProxyServer() throws InterruptedException, UnknownHostException {

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		BrowserMobProxyServer ps = buildBMProxy(proxy);
		System.err.println(ps.getClientBindAddress());
		System.err.println(ps.getPort());
		Thread.sleep(100000);
	}
}