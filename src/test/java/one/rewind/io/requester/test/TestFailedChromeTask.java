package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
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
		url_template = "http://{{q}}";
	}

	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestFailedChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);

		this.setValidator((a, t) -> {

			//throw new UnreachableBrowserException("Test");
			//throw new ProxyException();
			Account account1 = a.accounts.get(t.getDomain());
			throw new AccountException.Failed(account1);
			//throw new AccountException.Failed(a.accounts.get(t.getDomain()));

		});

		this.addDoneCallback((t) -> {
			System.err.println(t.getResponse().getText().length());
		});

		this.addAction(new LoginWithGeetestAction());
	}
}
