package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.ScheduledChromeTask;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Map;

import static one.rewind.io.requester.chrome.ChromeDriverDistributor.buildBMProxy;

public class ChromeDriverDistributorTest {

	@Before
	public void loadClass() throws Exception {

		Class.forName(TestChromeTask.class.getName());

		/*ChromeTaskHolder holder = new ChromeTaskHolder(

				TestChromeTask.class.getName(),
				TestChromeTask.domain(),
				TestChromeTask.need_login,
				null,
				ImmutableMap.of("q", "ip"),
				0,
				TestChromeTask.base_priority
		);

		ChromeTask task = holder.build();*/
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

			ChromeTaskHolder holder = new ChromeTaskHolder(
				TestChromeTask.class.getName(),
				TestChromeTask.domain(),
				TestChromeTask.need_login,
				null,
				ImmutableMap.of("q", String.valueOf(1950 + i)),
				0,
				TestChromeTask.base_priority
			);

			Map<String, Object> info = distributor.submit(holder);
			System.err.println(JSON.toPrettyJson(info));
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

		ChromeTaskHolder holder = new ChromeTaskHolder(
				TestChromeTask.class.getName(),
				TestChromeTask.domain(),
				TestChromeTask.need_login,
				null,
				ImmutableMap.of("q", "ip"),
				0,
				TestChromeTask.base_priority
		);

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

			ChromeTaskHolder holder = new ChromeTaskHolder(
					TestChromeTask.class.getName(),
					TestChromeTask.domain(),
					TestChromeTask.need_login,
					null,
					ImmutableMap.of("q", "ip"),
					0,
					TestChromeTask.base_priority
			);

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

			ChromeTaskHolder holder = new ChromeTaskHolder(
					TestFailedChromeTask.class.getName(),
					TestFailedChromeTask.domain(),
					TestFailedChromeTask.need_login,
					null,
					ImmutableMap.of("q", String.valueOf(1950 + i)),
					0,
					TestFailedChromeTask.base_priority
			);

			distributor.submit(holder);
		}

		Thread.sleep(60000);

		distributor.close();
	}

	@Test
	public void testProxyFailed() {

	}

	@Test
	public void testAccountFailed() {

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
