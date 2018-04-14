package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;
import org.openqa.selenium.chrome.ChromeDriver;
import one.rewind.json.JSON;

/**
 * 滚轮事件
 */
public class ScrollAction extends ChromeAction {

	public String value;

	public ScrollAction() {}

	public ScrollAction(String value) {
		this.value = value;
	}

	public void run() {
		try {
			String setscroll = "document.documentElement.scrollTop=" + value;
			agent.trigger(setscroll);
			success = true;
		} catch (Exception e) {
			logger.error(e);
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}