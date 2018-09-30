package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;


/**
 * 执行脚本
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:48:13
 */
public class ExecAction extends Action {

	public String script;

	public ExecAction() {}

	public ExecAction(String script) {
		this.script = script;
	}

	public boolean run(ChromeAgent agent) {

		if(script != null & script.length() > 0) {
			agent.trigger(script);
			return true;
		}

		return false;
	}
}