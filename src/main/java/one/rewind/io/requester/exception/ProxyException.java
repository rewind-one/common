package one.rewind.io.requester.exception;

import one.rewind.io.requester.proxy.Proxy;

/**
 * 代理异常
 */
public class ProxyException extends Exception {

	/**
	 * 代理失效
	 */
	public static class Failed extends Exception {

		public Proxy proxy;

		public Failed(Proxy proxy) {
			this.proxy = proxy;
		}
	}

	/**
	 * 代理超时
	 */
	public static class Timeout extends Exception {

		public Proxy proxy;

		public Timeout(Proxy proxy) {
			this.proxy = proxy;
		}
	}
}
