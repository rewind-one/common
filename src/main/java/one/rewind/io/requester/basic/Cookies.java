package one.rewind.io.requester.basic;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.URLUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于请求域名的多实例Cookie管理器
 *
 * Created by karajan on 2017/6/3 v1
 * scisaga@gmail.com 2019/2/12 v2
 */
public class Cookies {

	private static final Logger logger = LogManager.getLogger(Cookies.class.getName());

	private ConcurrentHashMap<String, Store> stores = new ConcurrentHashMap<>();

	public static class IllegalCookieOperation extends Exception {};
	public static class IllegalCookieFormat extends Exception {};

	/**
	 * 简单Cookie数据类
	 */
	public static class Item implements JSONable<Item> {
		String k;
		String v;
		String domain;
		String path;
		String c;

		/**
		 * 初始化
		 * 解析Set-Cookie: <cookie-name>=<cookie-value>; Domain=<domain-value>; Secure; HttpOnly
		 * 参考：https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Set-Cookie
		 *
		 * @param setCookieStr
		 * @throws IllegalCookieFormat
		 */
		public Item(String setCookieStr) throws IllegalCookieFormat {

			c = setCookieStr;

			String[] items = setCookieStr.split(";");

			for(String item : items) {

				if(item.contains("=")) {

					String[] token = item.split("=", 2);

					if(k == null) {
						k = token[0].trim();
						v = token.length == 1? null : token[1].trim();
					}
					// 设定 domain 属性
					else if(token[0].trim().matches("(?i)(domain)")) {
						domain = token.length == 1? null : token[1].trim();
					}
					// 设定 path 属性
					else if(token[0].trim().matches("(?i)(path)")) {
						path = token.length == 1? null : token[1].trim();
					}
				}
			}

			if(k == null || k.length() == 0) throw new IllegalCookieFormat();
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	/**
	 * 简单Cookie存储
	 *
	 * 同一个domain 不同path的 cookie不作区分
	 *
	 */
	public static class Store implements JSONable<Store>{

		Map<String, Map<String, Item>> store = new HashMap<>();

		public Store() {}

		/**
		 *
		 * @param url
		 * @param setCookieStrList
		 * @throws IllegalCookieFormat
		 * @throws IllegalCookieOperation
		 * @throws URISyntaxException
		 */
		public Store(String url, List<String> setCookieStrList) throws IllegalCookieFormat, IllegalCookieOperation, URISyntaxException {

			for(String setStr : setCookieStrList) {
				addCookie(url, setStr);
			}
		}

		/**
		 *
		 * @param store
		 */
		public void add(Store store) {

			for(String host : store.store.keySet()) {

				this.store.computeIfAbsent(host, k -> new HashMap<>());

				for(Item ci : store.store.get(host).values()) {
					add(ci);
				}
			}
		}

		/**
		 *
		 * @param ci
		 */
		private void add(Item ci) {

			store.computeIfAbsent(ci.domain, k -> new HashMap<>());

			if(ci.v == null || ci.v.length() == 0 || ci.v.equals("EXPIRED")) {
				store.get(ci.domain).remove(ci.k);
			}
			else {
				store.get(ci.domain).put(ci.k, ci);
			}
		}

		/**
		 *
		 * @param url
		 * @param setCookieStr
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 * @throws IllegalCookieOperation
		 * @throws IllegalCookieFormat
		 */
		public void addCookie(String url, String setCookieStr) throws URISyntaxException, IllegalCookieOperation, IllegalCookieFormat {

			URI uri = new URI(url);
			String domain = uri.getHost();

			/*logger.info(setCookieStr);*/

			Item ci = new Item(setCookieStr);

			if(ci.domain == null || ci.domain.length() == 0) {
				ci.domain = domain;
			}
			else {
				ci.domain = ci.domain.replaceAll("^\\.", "");
			}

			if(ci.domain != null && !URLUtil.getRootDomainName(domain).equals(URLUtil.getRootDomainName(ci.domain))) {
				throw new IllegalCookieOperation();
			}

			if(ci.path == null || ci.path.length() == 0) {
				ci.path = "/"; // 默认设定为根路径
			}

			add(ci);
		}

		/**
		 * 根据URL 获取Cookie字串
		 * @param url
		 * @return
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public String getCookies(String url) throws URISyntaxException {

			LinkedHashMap<String, Item> cookies = new LinkedHashMap<>();

			URI uri = new URI(url);

			List<String> domains = URLUtil.getAllDomains(uri);
			String path = uri.getPath();

			if(path == null || path.length() == 0) path = "/";

			// 查找可以发送的Cookie
			for(String domain : domains) {

				if(store.get(domain) != null) {

					for(Map.Entry<String, Item> ci : store.get(domain).entrySet()) {

						if(path.contains(ci.getValue().path)) {
							cookies.put(ci.getKey(), ci.getValue());
						}
					}
				}
			}

			// 生成Cookie字串
			return cookies.values().stream().map(ci -> {
				if(ci.v != null) return ci.k+"="+ci.v;
				return ci.k+"=";
			}).filter(s -> s!=null).collect(Collectors.joining("; "));
		}

		public String getCookies(String url, String k) throws URISyntaxException {

			String v = null;

			URI uri = new URI(url);

			List<String> domains = URLUtil.getAllDomains(uri);
			String path = uri.getPath();

			if(path == null || path.length() == 0) path = "/";

			// 查找可以发送的Cookie
			for(String domain : domains) {

				if(store.get(domain) != null) {

					for(Map.Entry<String, Item> ci : store.get(domain).entrySet()) {

						if(ci.getKey().equals(k) && path.contains(ci.getValue().path)) {
							v = ci.getValue().v;
						}
					}
				}
			}

			return v;
		}

		@Override
		public String toJSON() {
			return JSON.toPrettyJson(this.store);
		}
	}

	/**
	 * 添加一个Cookie
	 * @param ip 访问IP
	 * @param newStore
	 */
	public void addCookiesHolder(String ip, Store newStore) throws IllegalCookieOperation, IllegalCookieFormat, URISyntaxException {

		Store store = stores.get(ip);

		if(store == null) {
			stores.put(ip, newStore);
		} else {
			store.add(newStore);
		}

	}

	/**
	 *
	 * @param ip
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	public String getCookie(String ip, String url) throws URISyntaxException {

		Store store = stores.get(ip);

		if(store == null) return null;

		return store.getCookies(url);

	}
}