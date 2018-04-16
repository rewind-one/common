package one.rewind.io.test;

import net.lightbody.bmp.BrowserMobProxyServer;
import one.rewind.io.SshManager;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import org.junit.Test;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import static one.rewind.io.requester.chrome.ChromeDriverRequester.buildBMProxy;

public class RemoteDriverTest {

	@Test
	public void createDockerContainers() throws Exception {

		SshManager.Host host = new SshManager.Host("10.0.0.56", 22, "root", "sdyk315pr");
		host.connect();

		CountDownLatch done = new CountDownLatch(10);

		for(int i_=0; i_<10; i_++){

			final int i = i_;

			new Thread(() -> {

				String cmd = "docker run -d --name ChromeContainer-"+i+" -p "+(31000 + i)+":4444 -p "+(32000 + i)+":5900 -e SCREEN_WIDTH=\"1360\" -e SCREEN_HEIGHT=\"768\" -e SCREEN_DEPTH=\"24\" selenium/standalone-chrome-debug";

				String output = null;
				try {
					output = host.exec(cmd);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.err.println(output);

				done.countDown();

			}).start();
		}

		done.await();
	}

	@Test
	public void delAllDockerContainers() throws Exception {

		SshManager.Host host = new SshManager.Host("10.0.0.56", 22, "root", "sdyk315pr");
		host.connect();

		String cmd = "docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q)\n";

		String output = host.exec(cmd);

		System.err.println(output);
	}

	@Test
	public void simpleTest() throws Exception {

		final Proxy proxy = new ProxyImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");
		final URL remoteAddress = new URL("http://10.0.0.56:4444/wd/hub");
		ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, proxy);
		agent.start();

		System.err.println(ChromeDriverRequester.REQUESTER_LOCAL_IP + ":" + agent.bmProxy_port);

		Task task = new Task("http://ddns.oray.com/checkip");
		agent.submit(task);

		System.err.println(task.getResponse().getText());

		Thread.sleep(1000000);
	}


	@Test
	public void remoteTest() throws Exception {

		delAllDockerContainers();

		createDockerContainers();

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		for(int i=0; i<10; i++) {

			final Proxy proxy = new ProxyImpl("scisaga.net", 60103, "tfelab", "TfeLAB2@15");
			final URL remoteAddress = new URL("http://10.0.0.56:" + (31000 + i) + "/wd/hub");

			new Thread(() -> {
				try {

					ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress, proxy);
					//ChromeDriverAgent agent = new ChromeDriverAgent(remoteAddress);

					requester.addAgent(agent);

					agent.start();

				} catch (ChromeDriverException.IllegalStatusException e) {
					e.printStackTrace();
				}
			}).start();

		}

		for(int i=0; i<100; i++) {

			Task task = new Task("https://www.google.com.sg/search?q=1" + (1050 + i));
			requester.submit(task);
		}

		Thread.sleep(3000000);

		requester.close();

		delAllDockerContainers();
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
