/**
 * ExternalIpAddressFetcher.java
 *
 * @author karajan
 * @date 下午12:53:57
 */
package one.rewind.io.requester.proxy;

import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IpDetector 用于获取当前主机或所使用代理的实际外网地址
 * @author karajan
 * @date 2015年2月21日 下午12:53:57
 */
public class IpDetector {

	private static final Logger logger = LogManager.getLogger(IpDetector.class.getSimpleName());
	
	private static String ipPattern = "([1-9]\\d{0,2}\\.){3}([1-9]\\d{0,2})|(?:[a-fA-F0-9]{1,4}:){7}[a-fA-F0-9]{1,4}";
	
	private static final List<String> providerList = new ArrayList<String>();
	
	static {
		
		providerList.addAll(Configs.getConfig(IpDetector.class).getStringList("providerList"));
		
		providerList.stream().forEach(p -> {
			logger.info("IP detection provider: {}.", p);
		});
	}

	/**
	 * 获取外网IP
	 * @return
	 */
	public static String getIp() {
		return getIp(null);
	}

	/**
	 * 使用代理获取外网IP
	 * @param proxy
	 * @return
	 */
	public static String getIp(Proxy proxy) {
		
		String ipStr = null;
		
		int retryCount = 0;
		
		for (String url : providerList) {
			
			Task t;
			
			try {
				
				t = new Task(url);
				if(proxy != null) {
					t.setProxy(proxy);
				}
				
				BasicRequester.getInstance().submit(t, 10000);
				
				if(t.getResponse().getText() != null){

					ipStr = parse(t.getResponse().getText() );
					return ipStr;

				} else {

					if(proxy != null) {
						logger.info("{} can not reach {}", proxy.getHost() + ":" + proxy.getPort(), url);
					} else {
						logger.info("Can not reach {}", url);
					}
					
					retryCount ++;
					if(retryCount > 1) {
						break;
					}
				}
				
			} catch (MalformedURLException | URISyntaxException e) {
				logger.error(e);
			}
		}
		
		return null;
	}

	/**
	 * 解析源码中的IP地址
	 * @param html
	 * @return
	 */
	private static String parse(String html) {
		Pattern pattern = Pattern.compile(ipPattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(html);
		while (matcher.find()) {
			if(!matcher.group().matches("8.8.8.8|4.4.4.4"))
				return matcher.group(0);
		}
		return null;
	}
}