package one.rewind.io.requester.callback;

import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.chrome.ChromeDriverAgent;

public interface AccountCallback {

	void run(ChromeDriverAgent agent, Account account);
}
