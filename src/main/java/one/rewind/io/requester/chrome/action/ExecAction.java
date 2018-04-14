package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;
import org.openqa.selenium.chrome.ChromeDriver;
import one.rewind.json.JSON;


/**
 * 执行脚本
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:48:13
 */
public class ExecAction extends ChromeAction {

	public String script;

	public ExecAction() {}

	public ExecAction(String script) {
		this.script = script;
	}

	public void run() {

		if(script != null & script.length() > 0) {
			agent.trigger(script);
			success = true;
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}