package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;

/**
 * Created by karajan on 2017/6/3.
 */
public interface ChromeAction {

	public boolean run(ChromeDriverAgent agent) throws Exception;
}