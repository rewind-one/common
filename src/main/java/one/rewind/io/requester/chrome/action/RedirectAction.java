package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;

public class RedirectAction extends Action {

	String url;

	public RedirectAction(String url) {
		this.url = url;
	}

	public boolean run(ChromeAgent agent) {

		agent.getDriver().navigate().to(url);

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}
}