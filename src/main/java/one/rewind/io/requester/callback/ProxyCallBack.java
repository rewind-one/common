package one.rewind.io.requester.callback;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.proxy.Proxy;

public interface ProxyCallBack {

	void run(ChromeDriverAgent agent, Proxy proxy);
}
