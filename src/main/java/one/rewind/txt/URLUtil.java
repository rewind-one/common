package one.rewind.txt;

import com.google.common.net.InternetDomainName;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
	 * 获得根域名
	 * @param domain
	 * @return
	 */
	public static String getRootDomainName(String domain) {
		return InternetDomainName.from(domain).topPrivateDomain().toString();
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
}
