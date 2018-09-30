package one.rewind.io.requester.callback;

import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.exception.ChromeDriverException;

public interface ChromeDriverAgentCallback {

	void run(ChromeAgent agent) throws InterruptedException, ChromeDriverException.IllegalStatusException;
}
