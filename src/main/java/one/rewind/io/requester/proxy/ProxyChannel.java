package one.rewind.io.requester.proxy;

import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author scisaga@gmail.com
 * @date 2018/10/5
 */
public class ProxyChannel {

	static int count = 0;

	static String ChangeIpUrl = "http://proxy.abuyun.com/switch-ip";

	static long DefaultChangeIpInterval = 1200;

	public int id;

	public Proxy proxy;

	public long requestPerSecond = 5;

	public String currentIp;

	public long lastRequsetTime = System.currentTimeMillis();

	public long lastChangeIpTime = System.currentTimeMillis();

	public ProxyChannel(Proxy proxy) {
		this(proxy, 5);
	}

	public ProxyChannel(Proxy proxy, long requestPerSecond) {
		id = count ++;
		this.proxy = proxy;
	}

	public synchronized String changeIp() throws MalformedURLException, URISyntaxException, InterruptedException {

		long wait_time = lastChangeIpTime + DefaultChangeIpInterval - System.currentTimeMillis();

		if (wait_time > 0) {

			Proxy.logger.info("Wait {} ms.", wait_time);
			Thread.sleep(wait_time);
		}

		this.lastChangeIpTime = System.currentTimeMillis();

		Task t = new Task(ChangeIpUrl);
		t.setProxy(proxy);

		BasicRequester.getInstance().submit(t);

		String ipStr = t.getResponse().getText();

		if(ipStr != null && ipStr.length() > 0) {

			ipStr = ipStr.replaceAll(",.+?$|\r?\n", "");

			if(ipStr.matches("\\d+(\\.\\d+)+")) {

				String newIp = ipStr;

				if(!newIp.equals(currentIp)) {
					currentIp = newIp;
					Proxy.logger.info("Channel:{} New IP:{}", id, currentIp);
					return currentIp;
				}
				else {
					Proxy.logger.info("Channel:{} Get Same IP: {}", id, currentIp);
					return changeIp();
				}
			}
		}

		Proxy.logger.info("Channel:{} illegal response", id);
		return changeIp();
	}

}
