package one.rewind.io.requester.callback;

import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.chrome.ChromeAgent;

public interface AccountCallback {

	void run(ChromeAgent agent, Account account);
}
