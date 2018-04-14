/**
 * ProxyInfo.java
 *
 * @author "karajan"
 * @date 上午10:47:04
 */
package one.rewind.io.requester.proxy;

import one.rewind.json.JSONable;
import org.apache.http.HttpHost;
import one.rewind.json.JSONable;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * 
 * @author karajan
 *
 */
public interface ProxyWrapper extends JSONable, Serializable {

	public String getId();
	
	public String getInfo();
	
	/**
	 * 获取 java.net.Proxy对象
	 *
	 * @return
	 */
	public Proxy toProxy();

	/**
	 * @return
	 */
	public HttpHost toHttpHost();

	public InetSocketAddress getInetSocketAddress();

	/**
	 * 返回 http 或 https
	 * @return
	 */
	public String getProtocal();
	
	public String getHost();
	
	public boolean needAuth();
	
	public int getPort();
	
	public String getUsername();
	
	public String getPassword();

	public int getRequestPerSecondLimit();

	/**
	 * 获取Proxy登录信息Byte数组
	 *
	 * @return 保存Proxy登录信息
	 */
	public String getAuthenticationHeader();
	
	/**
	 * 
	 * @return
	 */
	public boolean isValid();
	
	/**
	 * 成功回调函数
	 * @throws Exception 
	 */
	public boolean success() throws Exception;

	/**
	 * 出错调用
	 * @throws Exception
	 */
	public boolean failed() throws Exception;
	
	public boolean timeout() throws Exception;

}