package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;

public class ClearCacheAction extends BasicAction {

	public ClearCacheAction() {}

	public boolean run(ChromeDriverAgent agent) {
		agent.getDriver().get("chrome://settings-frame/clearBrowserData");
		ClickAction ca = new ClickAction("#clear-browser-data-commit");
		ca.run(agent);
		return true;
	}
}