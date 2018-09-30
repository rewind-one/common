package one.rewind.io.requester.callback;

import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.task.Task;

public interface ProxyCallBack {

	void run(ChromeAgent agent, Proxy proxy, Task task);
}
