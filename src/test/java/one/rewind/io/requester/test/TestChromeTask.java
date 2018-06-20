package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.task.ChromeTask;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TestChromeTask extends ChromeTask {

	static {
		// init_map_class
		init_map_class = ImmutableMap.of("q", String.class);
		// init_map_defaults
		init_map_defaults = ImmutableMap.of("q", "ip");
		// url_template
		url_template = "http://www.b" +
				"  aidu.com/s?word={{q}}";
	}

	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);
		this.addDoneCallback((t) -> {
			System.err.println(t.getResponse().getText().length());
		});
	}
}
