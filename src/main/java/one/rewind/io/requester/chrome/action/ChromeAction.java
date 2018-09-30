package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;

/**
 * Created by karajan on 2017/6/3.
 */
public interface ChromeAction {

	public boolean run(ChromeAgent agent) throws Exception;
}