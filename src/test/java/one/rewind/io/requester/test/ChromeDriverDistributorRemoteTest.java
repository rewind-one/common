package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChromeDriverDistributorRemoteTest {

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
	public void simpleTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException, ChromeDriverException.NotFoundException, AccountException.NotFound {

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");

		ChromeDriverDockerContainer container =
				new ChromeDriverDockerContainer(host, "ChromeContainer-10.0.0.50-1", 31001, 32001);

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent(container.getRemoteAddress(), container);

		distributor.addAgent(agent);

		for(int i=0; i<100; i++) {

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

		Thread.sleep(1000000);

	}


	@Test
	public void loginTest() throws Exception {

		int containerNum = 1;

		DockerHost host = new DockerHost("10.0.0.50", 22, "root");
		host.delAllDockerContainers();

		//
		List<ChromeDriverDockerContainer> containers = new ArrayList<>();
		for(int i=0; i<containerNum; i++) {
			containers.add(host.createChromeDriverDockerContainer());
		}

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		final Proxy proxy = new ProxyImpl("10.0.0.51", 49999, null, null);

		//
		for(ChromeDriverDockerContainer container : containers) {

			final URL remoteAddress = container.getRemoteAddress();

			try {

				proxy.validate();

				ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container, proxy);
				//ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress);

				ChromeTask task = new ChromeTask("http://zbj.com");

				AccountImpl account = new AccountImpl("zbj.com", "17600668061", "gcy116149");

				task.addAction(new LoginWithGeetestAction(account));

				//
				agent.addNewCallback((a)->{
					try {
						a.submit(task);
					} catch (ChromeDriverException.IllegalStatusException e) {
						e.printStackTrace();
					}
				});

				distributor.addAgent(agent);

			} catch (ChromeDriverException.IllegalStatusException | URISyntaxException | InterruptedException | MalformedURLException e) {
				e.printStackTrace();
			}
		}

		/*for(int i=0; i<100; i++) {

			// Task task = new Task("https://www.google.com.sg/search?q=1" + (1050 + i));

			Task task = new Task("https://zbj.com");
			distributor.submit(task);
		}*/

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

		Thread.sleep(1000000);
	}
}
