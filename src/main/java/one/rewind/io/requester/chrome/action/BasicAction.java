package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BasicAction implements JSONable, ChromeAction {

	public static final Logger logger = LogManager.getLogger(ChromeAction.class.getName());

	public BasicAction() {}

	public boolean run(ChromeDriverAgent agent) {
		return true;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
