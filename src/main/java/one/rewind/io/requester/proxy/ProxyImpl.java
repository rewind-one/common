package one.rewind.io.requester.proxy;

public class ProxyImpl extends Proxy {

	public ProxyImpl(String host, int port, String username, String password) {
		super(null, host, port, username, password, null, 0);
	}

	public ProxyImpl(String group, String host, int port, String username, String password, String location, int request_per_second_limit) {
		super(group, host, port, username, password, location, request_per_second_limit);
	}

}
