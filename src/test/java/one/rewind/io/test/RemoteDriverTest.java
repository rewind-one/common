package one.rewind.io.test;

import one.rewind.io.SshManager;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import one.rewind.io.SshManager;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.io.requester.proxy.ProxyWrapperImpl;
import one.rewind.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class RemoteDriverTest {

	@Test
	public void createDockerContainers() throws Exception {

		SshManager.Host host = new SshManager.Host("10.0.0.62", 22, "root", "sdyk315pr");
		host.connect();

		for(int i=0; i<10; i++){

			String cmd = "docker run -d --name ChromeContainer-"+i+" -p "+(31000 + i)+":4444 -p "+(32000 + i)+":5900 -e SCREEN_WIDTH=\"1360\" -e SCREEN_HEIGHT=\"768\" -e SCREEN_DEPTH=\"24\" selenium/standalone-chrome-debug";

			String output = host.exec(cmd);
			System.err.println(output);

		}
	}

	@Test
	public void delAllDockerContainers() throws Exception {

		SshManager.Host host = new SshManager.Host("10.0.0.62", 22, "root", "sdyk315pr");
		host.connect();

		String cmd = "docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q)\n";

		String output = host.exec(cmd);

		System.err.println(output);
	}


	@Test
	public void remoteTest() throws Exception {

		delAllDockerContainers();

		createDockerContainers();

		ChromeDriverRequester requester = ChromeDriverRequester.getInstance();

		for(int i=0; i<10; i++) {

			final ProxyWrapper proxy = new ProxyWrapperImpl("114.215.70.14", 59998, "tfelab", "TfeLAB2@15");
			final URL remoteAddress = new URL("http://10.0.0.62:" + (31000 + i) + "/wd/hub");

			new Thread(() -> {
				requester.addChromeDriverAgent(new ChromeDriverAgent(remoteAddress, proxy));
			}).start();

		}

		for(int i=0; i<1000; i++) {

			Task task = new Task("http://www.baidu.com/s?word=ip" /*+ (1050 + i)*/);
			requester.submit(task);
		}

		Thread.sleep(3000000);

		requester.close();

		delAllDockerContainers();
	}

}
