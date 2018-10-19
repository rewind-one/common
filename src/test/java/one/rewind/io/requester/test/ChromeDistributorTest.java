package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.Distributor;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.scheduler.TaskScheduler;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.chrome.action.RedirectAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.chrome.ChromeTask;
import one.rewind.io.requester.scheduler.ScheduledTask;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

import static one.rewind.io.requester.chrome.ChromeDistributor.buildBMProxy;

public class ChromeDistributorTest {

	/**
	 * 4浏览器 并发请求100个任务
	 * @throws Exception
	 */
	@Test
	public void basicTest1() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<4; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		for(int i=0; i<1000; i++) {

			if(i%2 == 0) {

				TaskHolder holder = ChromeTask.at(TestChromeTask.T1.class, ImmutableMap.of("q", String.valueOf(1950 + i)));
				distributor.submit(holder);

			} else {

				TaskHolder holder = ChromeTask.at(TestChromeTask.T2.class, ImmutableMap.of("k", String.valueOf(1950 + i)));
				distributor.submit(holder);

			}
		}

		Thread.sleep(60000);

		distributor.close();
	}

	@Test
	public void basicTest2() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<4; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);

		}

		distributor.layout();

		for(int i=0; i<10; i++) {

			TaskHolder holder = ChromeTask.at(TestChromeTask.T3.class, ImmutableMap.of("k", String.valueOf(1950 + i)));
			distributor.submit(holder);

		}

		Thread.sleep(60000);

		distributor.close();
	}

	/**
	 * 调度任务
	 * @throws Exception
	 */
	@Test
	public void scheduleTaskTest() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<4; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		TaskHolder holder = ChromeTask.at(TestChromeTask.T3.class, ImmutableMap.of("k", String.valueOf(1950)));

		Distributor.SubmitInfo info = ChromeDistributor.getInstance().schedule(holder, Arrays.asList("* * * * *", "*/2 * * * *", "*/4 * * * *"));

		System.err.println(JSON.toPrettyJson(info));

		Thread.sleep(600000);

		distributor.close();
	}

	@Test
	public void proxyTest() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		ChromeAgent agent1 = new ChromeAgent(new ProxyImpl("uml.ink", 60201, "tfelab", "TfeLAB2@15"));
		distributor.addAgent(agent1);

		ChromeAgent agent2 = new ChromeAgent(new ProxyImpl("uml.ink", 60202, "tfelab", "TfeLAB2@15"));
		distributor.addAgent(agent2);

		ChromeAgent agent3 = new ChromeAgent(new ProxyImpl("uml.ink", 60204, "tfelab", "TfeLAB2@15"));
		distributor.addAgent(agent3);

		ChromeAgent agent4 = new ChromeAgent(new ProxyImpl("uml.ink", 60205, "tfelab", "TfeLAB2@15"));
		distributor.addAgent(agent4);

		distributor.layout();

		Account account = new AccountImpl("zbj.com", "15284812411", "123456");

		for(int i=0; i<10000; i++) {

			TaskHolder holder = ChromeTask.at(TestChromeTask.T1.class, ImmutableMap.of("q", "ip"));

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
	public void throwExceptionTest() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<1; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}

		distributor.layout();

		for(int i=0; i<10; i++) {

			TaskHolder holder = ChromeTask.at( TestChromeTask.TF.class, ImmutableMap.of("q", "ip") );

			distributor.submit(holder);
		}

		Thread.sleep(6000000);

		distributor.close();
	}

	/**
	 * 失败任务测试
	 * @throws Exception
	 */
	@Test
	public void failedTaskTest() throws Exception {


		TaskHolder holder = ChromeTask.at( TestChromeTask.TF.class, ImmutableMap.of("q", "ip") );

	}

	/**
	 * 账户异常回调
	 */
	@Test
	public void testAccountFailed() throws Exception {


		int containerNum = 1;

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");
		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		ChromeAgent agent = new ChromeAgent(container.getRemoteAddress(), container);

		distributor.addAgent(agent);
		//ChromeAgent agent = new ChromeAgent(remoteAddress);

		AccountImpl account_1 = new AccountImpl("zbj.com", "17600668061", "gcy116149");
		AccountImpl account_2 = new AccountImpl("zbj.com", "15284809626", "123456");

		ChromeTask task = new ChromeTask("http://www.zbj.com")
				.addAction(new LoginWithGeetestAction().setAccount(account_1));

		//
		agent.submit(task, true);

		agent.addAccountFailedCallback((a, acc) -> {

			try {
				ChromeTask task1 = new ChromeTask("http://www.zbj.com")
						.addAction(new RedirectAction("https://login.zbj.com/login/dologout"))
						.addAction(new LoginWithGeetestAction().setAccount(account_2));


				a.submit(task1, true);

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		TaskHolder holder = ChromeTask.at( TestChromeTask.TF.class, ImmutableMap.of("q", "ip") );

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();

	}

	/**
	 * 代理异常回调
	 * @throws Exception
	 */
	@Test
	public void proxyFailedTest() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		Proxy proxy1 = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");

		Proxy proxy2 = new ProxyImpl("118.190.133.34", 59998, "tfelab", "TfeLAB2@15");

		ChromeAgent agent = new ChromeAgent(proxy1);

		agent.addProxyFailedCallback((a, p, t) -> {

			try {
				a.changeProxy(proxy2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ChromeDriverException.IllegalStatusException e) {
				e.printStackTrace();
			}
		});

		/*ChromeTask task = new ChromeTask("https://www.baidu.com/s?wd=ip");

		task.setValidator((a, t) -> {

			logger.holder("proxy");
			//throw new UnreachableBrowserException("Test");
			throw new ProxyException.Failed(a.proxy);
			//throw new AccountException.Failed(account);
		});

		ChromeTask task1 = new ChromeTask("https://www.baidu.com/s?wd=ip");

		//
		agent.addNewCallback((a)->{
			try {
				a.submit(task1);
			} catch (ChromeDriverException.IllegalStatusException e) {
				e.printStackTrace();
			}
		});*/

		distributor.addAgent(agent);

		TaskHolder holder = ChromeTask.at( TestChromeTask.TF.class, ImmutableMap.of("q", "ip") );

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();
	}

	/**
	 * 测试带有递减周期监控任务
	 * @throws Exception
	 */
	@Test
	public void testScheduledTask() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<1; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}

		// distributor.layout();

		TaskHolder holder = ChromeTask.at(TestChromeTask.T4.class, ImmutableMap.of("q", String.valueOf(1950)));

		distributor.submit(holder);

		Thread.sleep(60000000);

	}

	@Test
	public void scanTaskTest() throws Exception {

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<1; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}

		// distributor.layout();

		TaskHolder holder = ChromeTask.at(TestChromeTask.T5.class, ImmutableMap.of("q", String.valueOf(1950), "max_page", 60));

		TaskHolder holder1 = ChromeTask.at(TestChromeTask.T5.class, ImmutableMap.of("q", String.valueOf(1989), "max_page", 60));

		distributor.submit(holder);
		distributor.submit(holder1);

		Thread.sleep(600000);

	}
}
