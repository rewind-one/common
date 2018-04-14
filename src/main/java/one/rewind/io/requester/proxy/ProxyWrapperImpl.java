package one.rewind.io.requester.proxy;

import one.rewind.json.JSON;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import one.rewind.json.JSON;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class ProxyWrapperImpl implements ProxyWrapper {
	
	private String host;
	private int port;
	private String username;
	private String password;
	private boolean isHttps = false;

	public ProxyWrapperImpl (String host, int port, String username, String password){
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public String getInfo() {
		return this.username + ":" + this.password + "@" + this.host + ":" + this.port;
	}

	@Override
	public Proxy toProxy() {
		InetSocketAddress addr = new InetSocketAddress(host, port);
		return new Proxy(Proxy.Type.HTTP, addr);
	}

	@Override
	public HttpHost toHttpHost() {
		return new HttpHost(host, port, getProtocal());
	}

	@Override
	public InetSocketAddress getInetSocketAddress() {
		return new InetSocketAddress(host, port);
	}

	@Override
	public String getProtocal() {
		return isHttps ? "https" : "http";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public boolean needAuth() {
		return this.password != null;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public int getRequestPerSecondLimit() {
		return 20;
	}

	@Override
	public String getAuthenticationHeader() {
		String authParam = getUsername() + ":" + getPassword();
		authParam = new String(Base64.encodeBase64(authParam.getBytes()));
		return "Basic " + authParam;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean success() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean failed() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean timeout() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
