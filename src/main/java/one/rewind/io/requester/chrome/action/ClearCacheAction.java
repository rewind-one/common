package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;
import org.openqa.selenium.chrome.ChromeDriver;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.json.JSON;

public class ClearCacheAction extends ChromeAction {

	public ClearCacheAction() {}

	public void run() {
		agent.getDriver().get("chrome://settings-frame/clearBrowserData");
		ClickAction ca = new ClickAction("#clear-browser-data-commit");
		ca.setAgent(agent);
		ca.run();
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}