package one.rewind.io.requester;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.rewind.io.requester.callback.NextTaskGenerator;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.proxy.IpDetector;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.task.Task;
import one.rewind.util.Configs;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class BasicDistributor extends Thread {

	private static final Logger logger = LogManager.getLogger(BasicDistributor.class.getName());

	protected static BasicDistributor instance;

	/**
	 *
	 * @return
	 */
	public static BasicDistributor getInstance() {

		if (instance == null) {
			synchronized (BasicDistributor.class) {
				if (instance == null) {
					instance = new BasicDistributor();
					instance.setName("BasicDistributor");
					instance.start();
				}
			}
		}

		return instance;
	}


	public static String LOCAL_IP = IpDetector.getIp() + " :: " + NetworkUtil.getLocalIp();

	// 全局每秒请求数
	static int REQUEST_PER_SECOND_LIMIT = 1;

	// 单次请求TIMEOUT
	static int CONNECT_TIMEOUT = 5000;

	// 重试次数
	static int RETRY_LIMIT = 3;

	static {
		try {
			REQUEST_PER_SECOND_LIMIT = Configs.getConfig(BasicRequester.class).getInt("requestPerSecondLimit");
			CONNECT_TIMEOUT = Configs.getConfig(BasicRequester.class).getInt("connectTimeout");
		} catch (Exception e) {
			logger.error("Error load config, ", e);
		}
	}

	Proxy proxy = null;

	private long lastRequestTime = System.currentTimeMillis();

	private ConcurrentHashMap<String, Date> fingerprints = new ConcurrentHashMap<>();

	BlockingQueue<Task> queue;

	private volatile boolean done = false;

	ThreadPoolExecutor executor =  new ThreadPoolExecutor(
			20 * REQUEST_PER_SECOND_LIMIT,
			40 * REQUEST_PER_SECOND_LIMIT,
			0, TimeUnit.MICROSECONDS,
			new LinkedBlockingQueue<>());


	/**
	 *
	 */
	private BasicDistributor() {

		queue = new LinkedBlockingQueue();

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("BasicDistributor-%d").build());
	}

	/**
	 *
	 */
	public void run() {

		while(!done) {

			Task t = null;

			try {

				t = queue.take();

				if(t.switchProxy()) {

					logger.info("New Request Group: {}", t.getUrl());

					t.addHeader("Proxy-Switch-Ip","yes");

					RequestGroupWrapper gw = new RequestGroupWrapper(t);

					executor.submit(gw);

					gw.phaser.arriveAndAwaitAdvance();

					gw.done = true;
					gw = null;

					logger.info("Request Group: {} Done.", t.getUrl());

				} else {

					executor.submit(new RequestWrapper(t, queue));
					waits();

				}

				logger.info("Active: {}, in queue: {}.", executor.getActiveCount(), executor.getQueue().size());

			} catch (InterruptedException e) {
				logger.error(e);
			} catch (Exception e) {
				logger.error("{}", t.toJSON(), e);
			}
		}
	}

	/**
	 *
	 * @param t
	 * @throws InterruptedException
	 */
	public void submit(Task t) throws InterruptedException {

		if(!fingerprints.containsKey(t.getFingerprint())) {
			fingerprints.put(t.getFingerprint(), new Date());
			queue.put(t);
		} else {
			logger.warn("Duplicate fingerprints [{}] --> {}", t.getFingerprint(), t.getUrl());
		}
	}

	/**
	 *
	 * @param proxy
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 *
	 */
	public void count() {

	}

	/**
	 *
	 */
	public void setDone() {
		this.done = true;
	}

	/**
	 *
	 * @throws InterruptedException
	 */
	public synchronized void waits() throws InterruptedException {

		long wait_time = lastRequestTime + (long) Math.ceil(1000D / (double) REQUEST_PER_SECOND_LIMIT) - System.currentTimeMillis();

		if(wait_time > 0) {
			logger.info("Wait {} ms.", wait_time);
			Thread.sleep(wait_time);
		}

		lastRequestTime = System.currentTimeMillis();
	}

	/**
	 *
	 */
	class RequestGroupWrapper implements Runnable {

		BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

		Phaser phaser;

		private volatile boolean done = false;

		/**
		 *
		 */
		public RequestGroupWrapper(Task t) throws InterruptedException {
			queue.put(t);
			phaser = new Phaser(2);
		}

		/**
		 *
		 */
		public void run() {

			while(!done) {

				Task t = null;

				try {

					t = queue.poll(1, TimeUnit.SECONDS);

					if(t != null) {

						executor.submit(new RequestWrapper(t, queue, phaser));

						waits();
					}

				} catch (Exception e) {
					logger.error("Error poll task, ", e);
				}
			}
		}
	}

	/**
	 *
	 */
	class RequestWrapper implements Runnable {

		Task<Task> t;

		BlockingQueue<Task> queue;

		Phaser phaser;

		RequestWrapper(Task t, BlockingQueue<Task> queue, Phaser phaser) {
			this.t = t;
			this.queue = queue;
			this.phaser = phaser;
		}

		RequestWrapper(Task t, BlockingQueue<Task> queue) {
			this(t, queue, null);
		}

		@Override
		public void run() {

			List<Task> nts = new ArrayList<>();

			try {

				t.setProxy(proxy);
				// t.setHeaders(genHeaders());

				BasicRequester.getInstance().submit(t, CONNECT_TIMEOUT);

				// A 重试
				if (t.getExceptions().size() > 0) {

					for(Throwable e : t.getExceptions()) {
						logger.error("Fetch Error: {}.", t.getUrl(), e);
					}

					if(t.getRetryCount() < RETRY_LIMIT) {
						t.addRetryCount();
						queue.put(t);
						return;
					} else {
						t.insert();
					}
				}
				// B 成功执行
				else {

					for (TaskCallback tc : t.doneCallbacks) {
						tc.run(t);
					}

					for (NextTaskGenerator ntg : t.nextTaskGenerators) {
						ntg.run(t, nts);
					}

					// 计数
					count();

					for(Task nt : nts) {

						if(!fingerprints.containsKey(nt.getFingerprint())) {

							fingerprints.put(nt.getFingerprint(), new Date());

							if(nt.switchProxy()) {

								BasicDistributor.this.queue.put(nt);

							}
							else {

								// TODO phaser 没有传
								System.err.println(nt.getUrl());
								queue.put(nt);

								if(phaser != null)
									phaser.register();
							}
						}
						else {
							logger.warn("Duplicate fingerprints [{}] --> {}", nt.getFingerprint(), nt.getUrl());
						}
					}
				}

				logger.info("{} {} duration: {}", t.getClass().getSimpleName(), t.getUrl(), t.getDuration());

			} catch (Exception e) {

				logger.error("Error exec request: {}, ", t.getUrl(), e);
			}

			if(phaser != null)
				phaser.arriveAndDeregister();
		}
	}

	/**
	 *
	 * @return
	 */
	public static String getUserAgent() {

		String[] agents = {
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1 QIHU 360SE",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 QIHU 360SE",
				"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; 360SE)",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1",
				"Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.15 (KHTML, like Gecko) Chrome/24.0.1295.0 Safari/537.15",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.93 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1467.0 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1623.0 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.103 Safari/537.36",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0b7) Gecko/20101111 Firefox/4.0b7",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0b8pre) Gecko/20101114 Firefox/4.0b8pre",
				"Mozilla/5.0 (X11; Linux x86_64; rv:2.0b9pre) Gecko/20110111 Firefox/4.0b9pre",
				"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:2.0b9pre) Gecko/20101228 Firefox/4.0b9pre",
				"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:2.2a1pre) Gecko/20110324 Firefox/4.2a1pre",
				"Mozilla/5.0 (X11; U; Linux amd64; rv:5.0) Gecko/20100101 Firefox/5.0 (Debian)",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:6.0a2) Gecko/20110613 Firefox/6.0a2",
				"Mozilla/5.0 (X11; Linux i686 on x86_64; rv:12.0) Gecko/20100101 Firefox/12.0",
				"Mozilla/5.0 (Windows NT 6.1; rv:15.0) Gecko/20120716 Firefox/15.0a2",
				"Mozilla/5.0 (X11; Ubuntu; Linux armv7l; rv:17.0) Gecko/20100101 Firefox/17.0",
				"Mozilla/5.0 (Windows NT 6.1; rv:21.0) Gecko/20130328 Firefox/21.0",
				"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:22.0) Gecko/20130328 Firefox/22.0",
				"Mozilla/5.0 (Windows NT 5.1; rv:25.0) Gecko/20100101 Firefox/25.0",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:25.0) Gecko/20100101 Firefox/25.0",
				"Mozilla/5.0 (Windows NT 6.1; rv:28.0) Gecko/20100101 Firefox/28.0",
				"Mozilla/5.0 (X11; Linux i686; rv:30.0) Gecko/20100101 Firefox/30.0",
				"Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0",
				"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:58.0) Gecko/20100101 Firefox/58.0",
				"Mozilla/5.0 (Windows NT 6.4; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36 Edge/12.0",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.9600",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.10240",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10547",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/14.14359"
		};

		int seed = new Random().nextInt(agents.length);
		return agents[seed];

	}

	/**
	 *
	 * @param host
	 * @return
	 */
	public static HashMap<String, String> genHeaders(String host) {

		HashMap<String, String> headers = new HashMap<>();

		headers.put("Host", host);
		headers.put("Connection", "Keep-Alive");
		headers.put("Cache-Control", "no-cache");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("Accept-Language", "zh-CN,zh;q=0.8");
		headers.put("Accept-Charset", "utf-8,gb2312;q=0.8,*;q=0.8");
		headers.put("User-Agent", getUserAgent());
		//headers.put("User-Agent", getUserAgent());
		//headers.put("Cookie", "pgv_pvi=7228721152; RK=9HYJF9Ik8x; ptcz=9eb72791f3f6403680844c7d4f6dfa90f0e142797bbca1d4f21db76567fc3a5f; ua_id=nH3IrBSrbrAcNn3vAAAAAO8gdtPurzMXN5W1qWdfohY=; eas_sid=S1O5S1o6j4k5b9q9s4I4M7w7y4; pgv_pvid=2899769615; LW_uid=Z1j5q1e6d540T0l7k7C3A3A1W3; tvfe_boss_uuid=051d402f854c09cc; ptui_loginuin=1161493143; pt2gguin=o1161493143; o_cookie=1161493143; LW_sid=S1J5x321x4D0f2O0C478R932u2; mm_lang=zh_CN; __lnkrntdmcvrd=-1; rewardsn=; wxtokenkey=777");
		headers.put("Accept-Encoding", "zip");
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("X-Auth5-Token", "GCY-" + System.currentTimeMillis());

		return headers;
	}
}
