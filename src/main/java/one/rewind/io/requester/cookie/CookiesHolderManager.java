package one.rewind.io.requester.cookie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karajan on 2017/6/3.
 */
public class CookiesHolderManager {

	private ConcurrentHashMap<String, Map<String, List<CookiesHolder>>> cookieStore = new ConcurrentHashMap<>();

	/**
	 *
	 * @param host
	 * @param domain
	 * @param cookieHolder
	 */
	public void addCookiesHolder(String host, String domain, CookiesHolder cookieHolder) {

		Map<String, List<CookiesHolder>> cookiesOneDomain = cookieStore.get(domain);

		if(cookiesOneDomain == null) {

			cookiesOneDomain = new HashMap<String, List<CookiesHolder>>();
			cookieStore.put(domain, cookiesOneDomain);
		}

		List<CookiesHolder> cookiesOneHost = cookiesOneDomain.get(host);
		if(cookiesOneHost == null) {
			cookiesOneHost = new ArrayList<CookiesHolder>();
			cookiesOneDomain.put(host, cookiesOneHost);
		}

		cookiesOneHost.add(cookieHolder);
	}

	/**
	 * 获取一个CookiesHolder
	 * @param host
	 * @param domain
	 * @return
	 */
	public CookiesHolder getCookiesHolder(String host, String domain) {

		if(cookieStore.get(domain) == null || cookieStore.get(domain).get(host) == null
				|| cookieStore.get(domain).get(host).size() < 20) {
			return null;
		}
		else {

			CookiesHolder cookiesHolder = cookieStore.get(domain).get(host).remove(0);

			cookiesHolder.count ++;

			if(cookiesHolder.count > 40) {
				return null;
			} else {
				return cookiesHolder;
			}
		}
	}

	/**
	 * 简单Cookie数据类
	 */
	public static class CookiesHolder {

		int count = 0;

		private String v;

		public CookiesHolder(String cookies) {
			this.v = cookies;
		}

		public void setCookie(String cookie) {
			this.v = cookie;
		}

		public String getCookie() {
			return this.v;
		}

	}

	/**
	 * 合并Cookies
	 * @param cookies1
	 * @param cookies2
	 * @return
	 */
	public static String mergeCookies(String cookies1, String cookies2) {

		String cookies = "";
		Map<String, String> map = new HashMap<String, String>();

		if(cookies1 != null && cookies1.length() > 0) {
			String[] cookie_items1 = cookies1.split(";");
			for(String cookie_item : cookie_items1) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {
					map.put(kv[0], kv[1]);
				}
			}
		}

		if(cookies2 != null && cookies2.length() > 0) {
			String[] cookie_items2 = cookies2.split(";");
			for(String cookie_item : cookie_items2) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {
					map.put(kv[0], kv[1]);
				}
			}
		}

		for(String key : map.keySet()) {
			cookies += key + "=" + map.get(key) + "; ";
		}

		return cookies;
	}
}