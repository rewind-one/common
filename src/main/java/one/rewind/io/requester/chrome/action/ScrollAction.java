package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;

/**
 * 滚轮事件
 */
public class ScrollAction extends Action {

	public String value;

	public ScrollAction() {}

	public ScrollAction(String value) {
		this.value = value;
	}

	public boolean run(ChromeDriverAgent agent) {
		try {
			String setscroll = "document.documentElement.scrollTop=" + value;
			agent.trigger(setscroll);
			return true;
		} catch (Exception e) {
			logger.error("Scroll error, ", e);
			return false;
		}
	}
}