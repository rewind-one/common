package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.chrome.action.RedirectAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.ScheduledChromeTask;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;

import static one.rewind.io.requester.chrome.ChromeDriverDistributor.buildBMProxy;
import static one.rewind.io.requester.chrome.ChromeDriverDistributor.logger;

public class ChromeDriverDistributorTest {

	@Before
	public void loadClass() throws Exception {

		Class.forName(TestChromeTask.class.getName());
		Class.forName(TestChromeTask.T1.class.getName());
		Class.forName(TestChromeTask.T2.class.getName());
	}

	/**
	 * 4浏览器 并发请求100个任务
	 * @throws Exception
	 */
	@Test
	public void basicTest() throws Exception {

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		for(int i=0; i<4; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		for(int i=0; i<1000; i++) {

			if(i%2 == 0) {
				ChromeTaskHolder holder = ChromeTask.buildHolder(
						TestChromeTask.T1.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

				Map<String, Object> info = distributor.submit(holder);
			} else {
				ChromeTaskHolder holder = ChromeTask.buildHolder(
						TestChromeTask.T2.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

				Map<String, Object> info = distributor.submit(holder);
			}
		}

		Thread.sleep(60000);

		distributor.close();
	}


	/**
	 * 4浏览器 并发请求100个任务
	 * @throws Exception
	 */
	@Test
	public void scheduleTaskTest() throws Exception {

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		for(int i=0; i<4; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		ChromeTaskHolder holder = ChromeTask.buildHolder(
				TestChromeTask.T1.class, ImmutableMap.of("q", "ip"));

		Map<String, Object> info = ChromeTaskScheduler.getInstance().schedule(new ScheduledChromeTask(holder, "* * * * *"));

		System.err.println(JSON.toPrettyJson(info));

		Thread.sleep(600000);

		distributor.close();
	}

	@Test
	public void proxyTest() throws Exception {

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent1 = new ChromeDriverAgent(proxy);
		distributor.addAgent(agent1);

		proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent2 = new ChromeDriverAgent(proxy);
		distributor.addAgent(agent2);
		proxy = new ProxyImpl("118.190.133.34", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent3 = new ChromeDriverAgent(proxy);
		distributor.addAgent(agent3);

		proxy = new ProxyImpl("118.190.44.184", 59998, "tfelab", "TfeLAB2@15");
		ChromeDriverAgent agent4 = new ChromeDriverAgent(proxy);
		distributor.addAgent(agent4);

		distributor.layout();

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<10000; i++) {

			ChromeTaskHolder holder =
					ChromeTask.buildHolder(TestChromeTask.T1.class, ImmutableMap.of("q", "ip"));

			distributor.submit(holder);
		}

		Thread.sleep(60000);

		distributor.close();
	}

	/**
	 * 手动抛出异常
	 * 查看ChromeDriverAgent重启情况
	 * @throws Exception
	 */
	@Test
	public void ExceptionTest() throws Exception {

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		for(int i=0; i<1; i++) {

			ChromeDriverAgent agent = new ChromeDriverAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		for(int i=0; i<10; i++) {

			ChromeTaskHolder holder = ChromeTask.buildHolder(
					TestFailedChromeTask.class, ImmutableMap.of("q", "ip"));

			distributor.submit(holder);
		}

		Thread.sleep(60000);

		distributor.close();
	}

	@Test
	public void test() throws Exception {

		Class.forName(TestFailedChromeTask.class.getName());

		ChromeTaskHolder holder = ChromeTask.buildHolder(
				TestFailedChromeTask.class, ImmutableMap.of("q", "ip"));

	}

	/**
	 * 账户异常回调
	 * @throws ChromeDriverException.IllegalStatusException
	 * @throws InterruptedException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws ChromeDriverException.NotFoundException
	 * @throws AccountException.NotFound
	 */
	@Test
	public void testAccountFailed() throws Exception {

		Class.forName(TestFailedChromeTask.class.getName());

		int containerNum = 1;

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");
		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent(container.getRemoteAddress(), container);

		distributor.addAgent(agent);
		//ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress);

		AccountImpl account_1 = new AccountImpl("zbj.com", "17600668061", "gcy116149");
		AccountImpl account_2 = new AccountImpl("zbj.com", "15284809626", "123456");

		ChromeTask task = new ChromeTask("http://www.zbj.com")
				.addAction(new LoginWithGeetestAction(account_1));

		//
		agent.submit(task, true);

		agent.addAccountFailedCallback((a, acc) -> {

			try {
				ChromeTask task1 = new ChromeTask("http://www.zbj.com")
						.addAction(new RedirectAction("https://login.zbj.com/login/dologout"))
						.addAction(new LoginWithGeetestAction(account_2));


				a.submit(task1, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		ChromeTaskHolder holder = ChromeTask.buildHolder(
				TestFailedChromeTask.class, ImmutableMap.of("q", "ip"));

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();

	}

	/**
	 * 代理异常回调
	 * @throws Exception
	 */
	@Test
	public void testProxyFailed() throws Exception {

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		Proxy proxy1 = new ProxyImpl("47.106.89.236", 59998, "tfelab", "TfeLAB2@15");

		Proxy proxy2 = new ProxyImpl("39.108.178.54", 59998, "tfelab", "TfeLAB2@15");

		ChromeDriverAgent agent = new ChromeDriverAgent(proxy1);

		agent.addProxyFailedCallback((a, p) -> {

			a.changeProxy(proxy2);
		});

		ChromeTask task = new ChromeTask("https://www.baidu.com/s?wd=ip");

		task.setValidator((a, t) -> {

			logger.info("proxy");
			//throw new UnreachableBrowserException("Test");
			throw new ProxyException.Failed(a.proxy);
			//throw new AccountException.Failed(account);
		});

		ChromeTask task1 = new ChromeTask("https://www.baidu.com/s?wd=ip");

		//
		agent.addNewCallback((a)->{
			try {
				a.submit(task);
			} catch (ChromeDriverException.IllegalStatusException e) {
				e.printStackTrace();
			}
		});

		distributor.addAgent(agent);


		Thread.sleep(6000000);

		distributor.close();
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
