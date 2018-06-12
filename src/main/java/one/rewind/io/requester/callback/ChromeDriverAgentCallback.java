package one.rewind.io.requester.callback;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.exception.ChromeDriverException;

public interface ChromeDriverAgentCallback {

	void run(ChromeDriverAgent agent) throws InterruptedException, ChromeDriverException.IllegalStatusException;
}
