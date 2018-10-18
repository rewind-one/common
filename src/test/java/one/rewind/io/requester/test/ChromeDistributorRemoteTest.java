package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.chrome.action.RedirectAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.parser.TemplateManager;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.chrome.ChromeTask;
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

/**
 * 远程的ChromeDriver测试
 */
public class ChromeDistributorRemoteTest {

	@Before
	public void loadClass() throws Exception {
		Class.forName(TestChromeTask.class.getName());
	}

	@Test
	public void batchTest() throws Exception {
		for(int i=0; i<20; i++) {
			loginTest();
		}
	}

	/**
	 * 远程服务器手动创建容器后执行简单调用测试
	 *
	 * @throws Exception
	 */
	@Test
	public void simpleTest() throws Exception {

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");

		ChromeDriverDockerContainer container =
				new ChromeDriverDockerContainer(host, "ChromeContainer-10.0.0.50-1", 31001, 32001);

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		ChromeAgent agent = new ChromeAgent(container.getRemoteAddress(), container);

		distributor.addAgent(agent);

		for(int i=0; i<100; i++) {

			TaskHolder holder = ChromeTask.at(TestChromeTask.T1.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

			System.err.println(JSON.toPrettyJson(distributor.submit(holder)));
		}

		Thread.sleep(1000000);
	}

	/**
	 * 登陆测试
	 * @throws Exception
	 */
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

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		//final Proxy proxy = new ProxyImpl("10.0.0.51", 49999, null, null);

		//CountDownLatch downLatch = new CountDownLatch(containerNum);

		for(ChromeDriverDockerContainer container : containers) {

			new Thread(()->{

				try {

					final URL remoteAddress = container.getRemoteAddress();

					//proxy.validate();

					/*CountDownLatch latch = new CountDownLatch(1);*/

					ChromeAgent agent = new ChromeAgent(remoteAddress, container/*, proxy*/);
					//ChromeAgent agent = new ChromeAgent(remoteAddress);

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
	 * 重启容器测试
	 * @throws Exception
	 */
	@Test
	public void restartContainer() throws Exception {

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");

		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		// final Proxy proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");

		final URL remoteAddress = container.getRemoteAddress();

		ChromeAgent agent = new ChromeAgent(remoteAddress, container);

		distributor.addAgent(agent);

		for(int i=0; i<10; i++) {

			TaskHolder holder = ChromeTask.at(TestChromeTask.TF.class, ImmutableMap.of("q", String.valueOf(1950 + i)));

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

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		Proxy proxy1 = new ProxyImpl("***", 59998, "***", "***@15");
		proxy1.validate();

		Proxy proxy2 = new ProxyImpl("***", 59998, "***", "***@15");
		proxy2.validate();

		final URL remoteAddress = container.getRemoteAddress();

		ChromeAgent agent = new ChromeAgent(remoteAddress, container, proxy1);

		agent.addProxyFailedCallback((a, p, t) -> {

			try {
				a.changeProxy(proxy2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		distributor.addAgent(agent);

		TaskHolder holder = ChromeTask.at(TestChromeTask.TPF.class, ImmutableMap.of("q", "ip"));

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
	 * docker Account 更换测试
	 */
	@Test
	public void RemoteAccountFiledTest() throws Exception {

		Class.forName(TestChromeTask.TF.class.getName());

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

		TaskHolder holder = ChromeTask.at(TestChromeTask.TF.class, ImmutableMap.of("q", ""), "17600668061");

		distributor.submit(holder);

		Thread.sleep(6000000);

		distributor.close();
	}
}
