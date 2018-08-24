package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.chrome.action.RedirectAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskFactory;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChromeDriverDistributorRemoteTest {

	@Before
	public void loadClass() throws Exception {

		Class.forName(TestChromeTask.class.getName());

		/*TaskHolder holder = new TaskHolder(

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


	@Test
	public void batchTest() throws Exception {
		for(int i=0; i<20; i++) {
			loginTest();
		}
	}

	/**
	 * 远程服务器手动创建容器后
	 * 执行简单调用测试
	 *
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws ChromeDriverException.IllegalStatusException
	 * @throws InterruptedException
	 * @throws ChromeDriverException.NotFoundException
	 * @throws AccountException.NotFound
	 */
	@Test
	public void simpleTest() throws Exception {

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");

		ChromeDriverDockerContainer container =
				new ChromeDriverDockerContainer(host, "ChromeContainer-10.0.0.50-1", 31001, 32001);

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent(container.getRemoteAddress(), container);

		distributor.addAgent(agent);

		for(int i=0; i<100; i++) {

			TaskHolder holder = ChromeTaskFactory.getInstance().newHolder(TestChromeTask.T1.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

			Map<String, Object> info = distributor.submit(holder);

			System.err.println(JSON.toPrettyJson(info));
		}

		Thread.sleep(1000000);
	}


	@Test
	public void loginTest() throws Exception {

		int containerNum = 1;

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");
		host.delAllDockerContainers();

		//
		List<ChromeDriverDockerContainer> containers = new ArrayList<>();
		for(int i=0; i<containerNum; i++) {
			containers.add(host.createChromeDriverDockerContainer());
		}

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		//final Proxy proxy = new ProxyImpl("10.0.0.51", 49999, null, null);

		//CountDownLatch downLatch = new CountDownLatch(containerNum);

		for(ChromeDriverDockerContainer container : containers) {

			new Thread(()->{

				try {

					final URL remoteAddress = container.getRemoteAddress();

					//proxy.validate();

					/*CountDownLatch latch = new CountDownLatch(1);*/

					ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container/*, proxy*/);
					//ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress);

					ChromeTask task = new ChromeTask("https://login.zbj.com/login");

					AccountImpl account = new AccountImpl("zbj.com", "17600668061", "gcy116149");

					task.addAction(new LoginWithGeetestAction().setAccount(account));

					// TODO 如果在NewCallback中调用了agent.submit方法，任务执行成功后会调用IdleCallbacks，而不是继续执行后续的NewCallback
					agent.addNewCallback((a) -> {
						a.submit(task);
					});

					distributor.addAgent(agent);

					//downLatch.countDown();

				} catch (ChromeDriverException.IllegalStatusException | URISyntaxException | InterruptedException | MalformedURLException e) {
					e.printStackTrace();
				}

			}).start();
		}

		//downLatch.await();

		ChromeTask task = new ChromeTask("https://www.baidu.com");

		Thread.sleep(600000);

		//distributor.close();
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void restartContainer() throws Exception {

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");

		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		// final Proxy proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");

		final URL remoteAddress = container.getRemoteAddress();

		ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container);

		distributor.addAgent(agent);

		for(int i=0; i<10; i++) {

			TaskHolder holder = ChromeTaskFactory.getInstance().newHolder(TestFailedChromeTask.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

			distributor.submit(holder);
		}

		Thread.sleep(1000000);
	}

	/**
	 * docker 代理更换测试
	 * @throws Exception
	 */
	@Test
	public void RremoteProxyFiledTest() throws Exception {

		DockerHost host = new DockerHost("***", 22, "***");

		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		Proxy proxy1 = new ProxyImpl("***", 59998, "***", "***@15");

		proxy1.validate();

		Proxy proxy2 = new ProxyImpl("***", 59998, "***", "***@15");

		proxy2.validate();

		final URL remoteAddress = container.getRemoteAddress();

		ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container, proxy1);

		agent.addProxyFailedCallback((a, p, t) -> {

			try {
				a.changeProxy(proxy2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		distributor.addAgent(agent);

		TaskHolder holder = ChromeTaskFactory.getInstance().newHolder(TestProxyFailedChromeTask.class, ImmutableMap.of("q", "ip"));

		//
		/*agent.addIdleCallback((a)->{
			try {
				a.submit(task);
			} catch (ChromeDriverException.IllegalStatusException e) {
				e.printStackTrace();
			}
		});*/

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();

	}

	/**
	 * docker Account更换测试
	 */
	@Test
	public void RemoteAccountFiledTest() throws Exception {

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

		TaskHolder holder = ChromeTaskFactory.getInstance().newHolder(TestFailedChromeTask.class, "17600668061", ImmutableMap.of("q", ""));

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();
	}
}
