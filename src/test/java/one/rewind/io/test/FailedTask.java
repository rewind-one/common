package one.rewind.io.test;

import one.rewind.io.requester.Task;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ProxyException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

public class FailedTask extends Task {


	public FailedTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);
	}

	public FailedTask(String url, String post_data) throws MalformedURLException, URISyntaxException {
		super(url, post_data);
	}

	public FailedTask(String url, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {
		super(url, post_data, cookies, ref);
	}

	public FailedTask(String url, HashMap<String, String> headers, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {
		super(url, headers, post_data, cookies, ref);
	}

	@Override
	public Task validate() throws ProxyException.Failed, AccountException.Failed, AccountException.Frozen {
		throw new UnreachableBrowserException("Failed task.");
		// return super.validate();
	}
}
