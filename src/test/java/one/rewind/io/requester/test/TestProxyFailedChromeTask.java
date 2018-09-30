package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.chrome.ChromeTask;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TestProxyFailedChromeTask extends ChromeTask {

	static {

		registerBuilder(
				TestProxyFailedChromeTask.class,
				"http://www.baidu.com/s?word={{q}}",
				ImmutableMap.of("q", String.class),
				ImmutableMap.of("q", "ip")
		);
	}
	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestProxyFailedChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);

		this.setValidator((a, t) -> {
			throw new ProxyException.Failed(a.proxy);
			//throw new ProxyException();

			//throw new AccountException.Failed();
		});

		this.addDoneCallback((t) -> {
			System.err.println(t.getResponse().getText().length());
		});
	}
}
