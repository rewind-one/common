package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;

public class RedirectAction extends Action {

	String url;

	public RedirectAction(String url) {
		this.url = url;
	}

	public boolean run(ChromeDriverAgent agent) {

		agent.getDriver().navigate().to(url);

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}
}