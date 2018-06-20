package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.task.ChromeTask;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TestFailedChromeTask extends ChromeTask {

	static {
		// init_map_class
		init_map_class = ImmutableMap.of("q", String.class);
		// init_map_defaults
		init_map_defaults = ImmutableMap.of("q", "ip");
		// url_template
		url_template = "http://www.baidu.com/s?word={{q}}";
	}

	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestFailedChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);

		this.setValidator((t) -> {
			throw new UnreachableBrowserException("Test");
		});

		this.addDoneCallback((t) -> {
			System.err.println(t.getResponse().getText().length());
		});
	}
}
