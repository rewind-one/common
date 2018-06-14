package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Test;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static one.rewind.io.requester.chrome.ChromeDriverRequester.buildBMProxy;

public class RemoteDriverTest {

	@Test
	public void test() {
		System.err.println(System.nanoTime());
		System.err.println(System.currentTimeMillis());
	}

	@Test
	public void batchTest() throws Exception {
		for(int i=0; i<20; i++) {
			remoteTest();
		}
	}

	@Test
	public void simpleTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");

		ChromeDriverDockerContainer container =
				new ChromeDriverDockerContainer(host, "ChromeContainer-10.0.0.62-2", 31002, 32002);


		ChromeDriverAgent agent = new ChromeDriverAgent(container.getRemoteAddress(), container);

		agent.start();

		agent.submit(new Task("http://www.baidu.com"));

	}


	@Test
	public void remoteTest() throws Exception {

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");
		host.delAllDockerContainers();

		//
		List<ChromeDriverDockerContainer> containers = new ArrayList<>();
		for(int i=0; i<1; i++) {
			containers.add(host.createChromeDriverDockerContainer());
		}

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		//
		for(ChromeDriverDockerContainer container : containers) {

			final Proxy proxy = new ProxyImpl("10.0.0.51", 59998, "tfelab", "TfeLAB2@15");
			final URL remoteAddress = container.getRemoteAddress();

			new Thread(() -> {
				try {

					proxy.validate();

					ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container, proxy);
					//ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress);

					Task task = new Task("http://zbj.com");
					AccountImpl account = new AccountImpl("zbj.com", "17600668061", "gcy116149");
					task.addAction(new LoginWithGeetestAction(account));

					agent.addNewCallback((a)->{
						try {
							a.submit(task);
						} catch (ChromeDriverException.IllegalStatusException e) {
							e.printStackTrace();
						}
					});

					requester.addAgent(agent);

					agent.start();

				} catch (ChromeDriverException.IllegalStatusException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}).start();

		}

		/*for(int i=0; i<100; i++) {

			// Task task = new Task("https://www.google.com.sg/search?q=1" + (1050 + i));

			Task task = new Task("https://zbj.com");
			requester.submit(task);
		}*/

		Thread.sleep(600000);

		//requester.close();
	}

	@Test
	public void testBuildProxyServer() throws InterruptedException, UnknownHostException {

		Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
		BrowserMobProxyServer ps = buildBMProxy(proxy);
		System.err.println(ps.getClientBindAddress());
		System.err.println(ps.getPort());
		Thread.sleep(100000);
	}

	@Test
	public void testGetLocalAddress() throws UnknownHostException {
		System.err.println(InetAddress.getLocalHost());
	}

	@Test
	public void ContainerTest() throws Exception {

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");
		host.delAllDockerContainers();
		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();
		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();
		final Proxy proxy = new ProxyImpl("10.0.0.51", 59998, "tfelab", "TfeLAB2@15");
		final URL remoteAddress = container.getRemoteAddress();

		ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container, proxy);
		requester.addAgent(agent);

		agent.start();

		while (true) {
			Task task = new Task("http://zbj.com");
			agent.submit(task);
		}

	}

	@Test
	public void RestartContainer() throws Exception {

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");
		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		// final Proxy proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");

		final URL remoteAddress = container.getRemoteAddress();
		ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, container);
		requester.addAgent(agent);

		agent.start();

		for(int i=0; i<10; i++) {

			Task task = new Task("http://www.baidu.com/s?wd=ip");
			task.setValidator((t)->{
				throw new UnreachableBrowserException("Failed task.");
			});
			requester.submit(task);
		}

		Thread.sleep(1000000);
	}
}
