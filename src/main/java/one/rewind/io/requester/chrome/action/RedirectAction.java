package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;

public class RedirectAction extends ChromeAction {

	String url;

	public RedirectAction(String url) {
		this.url = url;
	}

	public void run() {
		agent.getDriver().navigate().to(url);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}