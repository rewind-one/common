package one.rewind.io.requester.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karajan on 2017/6/3.
 */
public class CookiesManager {

	private ConcurrentHashMap<String, ConcurrentHashMap<String, CookiesHolder>> store = new ConcurrentHashMap<>();

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
	 * 添加一个Cookie
	 * @param ip 访问IP
	 * @param domain 目标网站域名
	 * @param cookieHolder Cookie信息
	 */
	public void addCookiesHolder(String ip, String domain, CookiesHolder cookieHolder) {

		ConcurrentHashMap<String, CookiesHolder> cookiesOneHost = store.get(ip);

		if(cookiesOneHost == null) {

			cookiesOneHost = new ConcurrentHashMap<>();
			store.put(domain, cookiesOneHost);
		}

		CookiesHolder cookiesHolder = cookiesOneHost.get(domain);

		if(cookiesHolder == null) {
			cookiesOneHost.put(domain, cookieHolder);
		}
	}

	/**
	 * 获取一个CookiesHolder
	 * @param ip
	 * @param domain
	 * @return
	 */
	public CookiesHolder getCookiesHolder(String ip, String domain) {

		if( store.get(ip) == null || store.get(ip).get(domain) == null )
		{
			return null;
		}
		else {

			CookiesHolder cookiesHolder = store.get(ip).get(domain);

			cookiesHolder.count ++;

			return cookiesHolder;
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