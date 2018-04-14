package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.chrome.ChromeDriver;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

/**
 * Created by karajan on 2017/6/3.
 */
public class ChromeAction implements Runnable, JSONable {

	public static final Logger logger = LogManager.getLogger(ChromeAction.class.getName());

	public transient boolean success = false;
	public transient ChromeDriverAgent agent;

	public ChromeAction() {}

	public void setAgent(ChromeDriverAgent agent) {
		this.agent = agent;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	@Override
	public void run() {
		success = true;
	}
}