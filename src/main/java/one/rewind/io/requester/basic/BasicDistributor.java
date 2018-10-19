package one.rewind.io.requester.basic;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.stmt.query.In;
import one.rewind.db.RedissonAdapter;
import one.rewind.io.requester.Distributor;
import one.rewind.io.requester.callback.NextTaskGenerator;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.parser.TemplateManager;
import one.rewind.io.requester.proxy.IpDetector;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyChannel;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.util.Configs;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMap;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

public class BasicDistributor extends Distributor {

	static final Logger logger = LogManager.getLogger(BasicDistributor.class.getName());

	public static BasicDistributor Instance;

	public static String LOCAL_IP = IpDetector.getIp() + " :: " + NetworkUtil.getLocalIp();

	// 全局每秒请求数
	static int REQUEST_PER_SECOND_LIMIT = 20;

	// 单次请求TIMEOUT
	static int CONNECT_TIMEOUT = 20000;

	// 重试次数
	static int RETRY_LIMIT = 100;

	static {

		try {

			REQUEST_PER_SECOND_LIMIT = Configs.getConfig(BasicRequester.class).getInt("requestPerSecondLimit");
			CONNECT_TIMEOUT = Configs.getConfig(BasicRequester.class).getInt("connectTimeout");

		} catch (Exception e) {
			logger.error("Error load config, ", e);
		}
	}

	/**
	 *
	 * @return
	 */
	public static BasicDistributor getInstance() {

		if (Instance == null) {
			synchronized (BasicDistributor.class) {
				if (Instance == null) {
					Instance = new BasicDistributor();
				}
			}
		}

		return Instance;
	}

	// 默认执行器
	public Operator operator;

	// 配置代理的执行器列表
	public List<Operator> operators = new ArrayList<>();

	// 已经执行完的任务特征缓存
	public RMap<String, Long> fingerprints = RedissonAdapter.redisson.getMap("WX-Fingerprints");

	// 任务队列 TODO --> redisson
	public PriorityBlockingQueue<TaskHolder> queue = new PriorityBlockingQueue();

	/**
	 *
	 */
	public BasicDistributor() {

		super();

		operator = new Operator(null);
		operator.start();

		// 每10s 打印队列中任务数量
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.info("Queue: {}", queue.size());
			}
		}, 10000, 10000);
	}

	/**
	 * 添加一个配置代理隧道的Operator
	 * @param channel
	 */
	public BasicDistributor addOperator(ProxyChannel channel) {

		operator.setDone();
		operator = null;

		Operator op = new Operator(channel);
		operators.add(op);
		op.start();

		return this;
	}

	/**
	 * 添加一个配置代理的Operator
	 * @param proxy
	 */
	public BasicDistributor addOperator(Proxy proxy) {

		operator.setDone();
		operator = null;

		Operator op = new Operator().setProxy(proxy);
		operators.add(op);
		op.start();

		return this;
	}

	/**
	 * 提交任务
	 * 为了复用去重方法，此处可将queue作为一个输入变量
	 * @param th
	 */
	public synchronized SubmitInfo submit(TaskHolder th, BlockingQueue queue) throws InterruptedException {

		if(!fingerprints.containsKey(th.fingerprint)) {

			queue.put(th);
			return new SubmitInfo();
		}

		else {

			// 上一次采集时间
			long lts = fingerprints.get(th.fingerprint);

			long ts = System.currentTimeMillis();

			//
			if(ts - lts >= th.min_interval / 2) {
				queue.put(th);
				return new SubmitInfo();
			} else {
				logger.warn("Duplicate fingerprints {}:[{}] --> {}:{}", th.class_name, th.template_id, th.domain, th.fingerprint);
				return new SubmitInfo(false);
			}
		}
	}

	/**
	 * 默认提交方法
	 * @param th
	 * @return
	 * @throws InterruptedException
	 */
	public SubmitInfo submit(TaskHolder th) throws InterruptedException {
		return submit(th, queue);
	}

	/**
	 *
	 */
	public class Operator extends Thread {

		ProxyChannel channel;

		Proxy proxy;

		long lastRequestTime = System.currentTimeMillis();

		// 是否执行完的标识
		private volatile boolean done = false;

		// 线程池执行器
		ThreadPoolExecutor executor =  new ThreadPoolExecutor(
				20,
				80,
				0, TimeUnit.MICROSECONDS,
				new LinkedBlockingQueue<>());

		public Operator() {
			this(null);
		}

		/**
		 *
		 */
		public Operator(ProxyChannel channel) {

			String name = "Operator";

			if(channel != null) {
				name = "Operator-Ch" + channel.id;
			}

			setName(name);

			executor.setThreadFactory(
					new ThreadFactoryBuilder()
							.setNameFormat(name + "-%d")
							.build());

			this.channel = channel;
		}

		/**
		 *
		 * @param proxy
		 * @return
		 */
		public Operator setProxy(Proxy proxy) {
			this.proxy = proxy;
			return this;
		}

		/**
		 * 任务等待
		 * @throws InterruptedException
		 */
		public synchronized void waits() throws InterruptedException {

			double requestPerSecond = REQUEST_PER_SECOND_LIMIT;
			if(channel != null) {
				lastRequestTime = channel.lastRequsetTime;
				requestPerSecond = channel.requestPerSecond;
			}

			long wait_time = lastRequestTime + (long) Math.ceil(1000D / requestPerSecond) - System.currentTimeMillis();

			if (wait_time > 0) {

				logger.info("Wait {} ms.", wait_time);
				Thread.sleep(wait_time);
			}

			this.lastRequestTime = System.currentTimeMillis();

			if(channel != null) {
				channel.lastRequsetTime = this.lastRequestTime;
			}
		}

		/**
		 *
		 */
		public void run() {

			while(!done) {

				TaskHolder th = null;

				try {

					th = queue.poll(100, TimeUnit.MILLISECONDS);

					if(th != null) {

						waits();

						executor.submit(new RequestWrapper(th.build()));

						logger.info("[{} / {} / {}]", executor.getActiveCount(), executor.getQueue().size(), queue.size());
					}

				} catch (Exception e) {
					logger.error("Error get task, ", e);
				}
			}
		}

		/**
		 * 计数
		 * TODO 未实现
		 */
		public void count(boolean success) {

		}

		/**
		 * 设置结束
		 */
		public void setDone() {
			this.done = true;
		}

		/**
		 * 请求封装
		 */
		class RequestWrapper implements Runnable {

			// 任务
			public Task<Task> t;

			// 验证异常
			public boolean validatorException = false;

			// 采集异常
			public boolean fetchException = false;

			// 重试
			public boolean retry = false;

			/**
			 * 构造方法
			 * @param t
			 */
			RequestWrapper(Task<Task> t) {
				this.t = t;
			}

			/**
			 *
			 * @param nth
			 */
			public void submit(TaskHolder nth) throws InterruptedException {

				BasicDistributor.this.submit(nth);
			}

			@Override
			public void run() {

				try {

					String class_name = t.holder.class_name;
					int template_id = t.holder.template_id;
					String domain = t.holder.domain;
					String fingerprint = t.holder.fingerprint;

					// 二次去重
					if (fingerprints.containsKey(t.holder.fingerprint)) {

						// 上一次采集时间
						long lts = fingerprints.get(t.holder.fingerprint);

						long ts = System.currentTimeMillis();

						if (ts - lts < t.holder.min_interval) {
							logger.warn("Duplicate fingerprints {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);
							return;
						}
					}

					if (channel != null) {
						t.setProxy(channel.proxy);
					}
					else if (proxy != null) {
						t.setProxy(proxy);
					}

					// 下一级任务
					List<TaskHolder> nths = new ArrayList<>();

					BasicRequester.getInstance().submit(t, CONNECT_TIMEOUT);

					// A Validator 验证
					try {
						if (t.validator != null)
							t.validator.run(null, t);

					} catch (Exception e) {

						logger.error("Validator exception {}:[{}] --> {}:{}, ", class_name, template_id, domain, fingerprint, e);
						validatorException = true;
						retry = true;
					}

					// B 异常处理
					if (t.exception != null) {

						logger.error("Fetch Error {}:[{}] {}:[{}] --> {}:{}, ", class_name, template_id, domain, fingerprint, t.exception);

						fetchException = true;
						retry = true;
					}

					// C 成功执行 执行 callbacks 生成下一级任务
					if (!validatorException && !fetchException) {

						try {

							for (TaskCallback tc : t.doneCallbacks) {
								tc.run(t);
							}

							for (NextTaskGenerator ntg : t.nextTaskGenerators) {
								ntg.run(t, nths);
							}

							// 计数
							count(false);

							// 提交下一级任务
							for (TaskHolder nth : nths) {
								submit(nth);
							}

						} catch (Exception e) {

							logger.error("Error exec callbacks {}:[{}] --> {}:{}, ", class_name, template_id, domain, fingerprint, e);
							retry = true;
						}
					}

					// C1 异常 重试
					if (retry) {

						count(false);

						if (t.holder.retry_count < RETRY_LIMIT) {

							logger.warn("Retry {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);

							t.holder.retry_count ++;

							try {
								submit(t.holder); // 非阻塞方法
							} catch (InterruptedException e) {
								logger.warn("Error submit task, {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);
								t.holder.insert(); // 失败保存数据库
							}

						} else {

							logger.warn("Exceed retry limit, {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);
							t.holder.insert(); // 失败保存数据库
						}
					}
					// C2 成功执行
					else {

						count(true);
						fingerprints.put(fingerprint, new Date().getTime());
						logger.info("{}:[{}] --> {}:{} done --> {}", class_name, template_id, domain, fingerprint, t.getDuration());
					}

				} catch (Exception ex) {
					logger.error("Unhandled exception, ", ex);
				}

			}
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
		//headers.put("Cookie", "pgv_pvi=7228721152; RK=9HYJF9Ik8x; ptcz=9eb72791f3f6403680844c7d4f6dfa90f0e142797bbca1d4f21db76567fc3a5f; ua_id=nH3IrBSrbrAcNn3vAAAAAO8gdtPurzMXN5W1qWdfohY=; eas_sid=S1O5S1o6j4k5b9q9s4I4M7w7y4; pgv_pvid=2899769615; LW_uid=Z1j5q1e6d540T0l7k7C3A3A1W3; tvfe_boss_uuid=051d402f854c09cc; ptui_loginuin=1161493143; pt2gguin=o1161493143; o_cookie=1161493143; LW_sid=S1J5x321x4D0f2O0C478R932u2; mm_lang=zh_CN; __lnkrntdmcvrd=-1; rewardsn=; wxtokenkey=777");
		headers.put("Accept-Encoding", "zip");
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("X-Auth5-Token", "GCY-" + System.currentTimeMillis());

		return headers;
	}
}
