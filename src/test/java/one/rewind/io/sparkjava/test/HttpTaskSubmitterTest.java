package one.rewind.io.sparkjava.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.HttpTaskSubmitter;
import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.test.TestChromeTask;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static spark.Spark.port;

public class HttpTaskSubmitterTest {

	@Before
	public void init() throws ChromeDriverException.IllegalStatusException, InterruptedException, ClassNotFoundException, URISyntaxException, UnsupportedEncodingException, MalformedURLException {

		port(80);

		Class.forName(TestChromeTask.class.getName());
		Class.forName(TestChromeTask.T1.class.getName());
		Class.forName(TestChromeTask.T2.class.getName());
		Class.forName(TestChromeTask.T3.class.getName());
		Class.forName(TestChromeTask.T4.class.getName());
		Class.forName(TestChromeTask.T5.class.getName());

		ChromeDistributor distributor = ChromeDistributor.getInstance();

		for(int i=0; i<1; i++) {

			ChromeAgent agent = new ChromeAgent();
			distributor.addAgent(agent);
		}
		HttpTaskSubmitter.getInstance().submit(TestChromeTask.T1.class.getName(), JSON.toJson(ImmutableMap.of("q", String.valueOf(1950))));
	}

	@Test
	public void submitterTest() throws ClassNotFoundException, MalformedURLException, UnsupportedEncodingException, URISyntaxException, InterruptedException {


		Msg msg = HttpTaskSubmitter.getInstance().submit(TestChromeTask.T4.class.getName(), "", JSON.toJson(ImmutableMap.of("q", String.valueOf(1950))), 0);

		System.err.println(JSON.toPrettyJson(msg));

		Thread.sleep(1000000);
	}
}
