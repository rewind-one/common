package one.rewind.txt;

import com.google.common.collect.Lists;
import com.google.common.net.InternetDomainName;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL相关工具方法
 *
 * @author scisaga@gmail.com
 * @date 2017.11.13
 */
public class URLUtil {

	/**
	 * 从url中获得域名
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public static String getDomainName(String url) throws URISyntaxException, MalformedURLException {
		URL uri = new URL(url);
		String domain = uri.getHost();

		if (domain.contains(".")){
			String[] domains = domain.split("\\.");

			for (int i = 0; i < domains.length; i++) {
				if (domains[i].equals("com") && i != 0) {
					domain = domains[i-1] +"."+ domains[i];
					return domain;
				}
			}
		}
		return domain;
	}

	/**
	 *
	 * @param uri
	 * @return
	 * @throws URISyntaxException
	 */
	public static List<String> getAllDomains(URI uri) throws URISyntaxException {

		List<String> hosts = new ArrayList<>();
		hosts.add(uri.getHost());

		String host = uri.getHost();

		hosts.add(host);

		while(host.split("\\.").length>2) {
			host = host.replaceAll("^.+?\\.", "");
			hosts.add(host);
		}

		return Lists.reverse(hosts); // 第一个是根域名 最后一个是直接域名
	}

	/**
	 * 获得根域名
	 * @param domain
	 * @return
	 */
	public static String getRootDomainName(String domain) {
		return InternetDomainName.from(domain.replaceAll("^//.", "")).topPrivateDomain().toString();
	}

	/**
	 * 从url中获得端口号
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public static int getPort(String url) throws URISyntaxException, MalformedURLException {

		URL uri = new URL(url);

		int port = 80;

		if(uri.getProtocol().equals("https")){
			port = 443;
		}

		if(uri.getPort() > 0) {
			port = uri.getPort();
		}
		return port;
	}

	/**
	 * 从url中获得协议类型
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public static String getProtocol(String url) throws URISyntaxException, MalformedURLException {
		URL uri = new URL(url);
		return uri.getProtocol();
	}

	public static String getParam(String query, String k) {

		if(query.contains("http") && query.contains("?")) {

			try {
				query = new URI(query).getQuery();
			} catch (Exception e) {
			}
		}

		String[] params = query.split("&");
		Map<String, String> map = new HashMap<>();
		for (String param : params)
		{
			String[] token = param.split("=", 2);
			String name = token[0];
			String value = token.length == 2? token[1] : "";
			map.put(name, value);
		}
		return map.get(k);
	}
}
