package one.rewind.io.requester.basic;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyChannel;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.*;

public class BasicDistributorPG extends BasicDistributor {

	public static long DefaultChangeIpInterval = 1200;

	private volatile boolean switchProxy = false;

	private ConcurrentHashMap<String, Long> bannedIps = new ConcurrentHashMap<>();

	private volatile boolean done = false;

	/**
	 *
	 * @return
	 */
	public static BasicDistributor getInstance() {

		if (Instance == null) {
			synchronized (BasicDistributorPG.class) {
				if (Instance == null) {
					Instance = new BasicDistributorPG("default");
				}
			}
		}

		return Instance;
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public static BasicDistributor getInstance(String name) {

		if (Named_Instances.get(name) == null) {
			Named_Instances.put(name, new BasicDistributorPG(name));
		}

		return Named_Instances.get(name);
	}

	/**
	 *
	 */
	public BasicDistributorPG(String name) {
		super(name);
	}

	/**
	 *
	 */
	public void addDefaultOperator() {
		operator = new BasicDistributorPG.Operator(null);
		operator.start();
	}

	/**
	 * 添加一个配置代理隧道的Operator
	 * @param channel
	 */
	public BasicDistributor addOperator(ProxyChannel channel) {

		BasicDistributorPG.Operator op = new BasicDistributorPG.Operator(channel);
		operators.add(op);
		op.start();

		return this;
	}

	/**
	 * 添加一个配置代理的Operator
	 * @param proxy
	 */
	public BasicDistributor addOperator(Proxy proxy) {

		BasicDistributor.Operator op = new BasicDistributorPG.Operator().setProxy(proxy);
		operators.add(op);
		op.start();

		return this;
	}

	/**
	 *
	 * @param channel
	 * @throws ProxyException.Failed
	 * @throws MalformedURLException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 */
	public synchronized void changeIp(ProxyChannel channel) throws ProxyException.Failed, MalformedURLException, InterruptedException, URISyntaxException {

		String ip = channel.changeIp();

		if(bannedIps.contains(ip) && System.currentTimeMillis() - bannedIps.get(ip) < 5 * 60 * 1000) {
			changeIp(channel);
		}
	}

	/**
	 *
	 */
	public class Operator extends BasicDistributor.Operator {

		public Operator() {
			this(null);
		}

		/**
		 *
		 * @param channel
		 */
		public Operator(ProxyChannel channel) {

			super(channel);

			String name = "OperatorPG";

			if(channel != null) {
				name = "OperatorPG-Ch" + channel.id;
			}

			setName(name);

			executor.setThreadFactory(
					new ThreadFactoryBuilder()
							.setNameFormat(name + "-%d")
							.build());

			this.channel = channel;

			logger.info("{} created.", name);
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

						// 去掉 队列中任务fingerprints 对应记录
						queueFingerprints.remove(th.fingerprint);

						// 需要切换代理
						if (th.flags.contains(Task.Flag.SWITCH_PROXY)) {


							RequestGroupWrapper gw = new RequestGroupWrapper(th, channel);
							executor.submit(gw);

							//logger.info("*Phaser {}", gw.phaser.getUnarrivedParties());

							gw.phaser.arriveAndAwaitAdvance();
							gw.done = true;

						}
						else {

							// 不应该在主线程执行
							if (switchProxy) {
								changeIp(channel);
								switchProxy = false;
							}

							waits();

							Task t = th.build();
							t.setProxy(channel.proxy);

							executor.submit(new RequestWrapper(t));
						}

						logger.info("{} --> [{} / {} / {}]", name, executor.getActiveCount(), executor.getQueue().size(), queue.size());
					}

				} catch (Exception e) {
					logger.error("Error get task, ", e);
				}
			}
		}

		/**
		 * 请求组封装
		 * 在同组中的出口IP是唯一的
		 */
		class RequestGroupWrapper implements Runnable {

			String className;

			String fingerprint;

			// 请求组内部任务对接
			BlockingQueue<TaskHolder> queue = new LinkedBlockingQueue<>();

			ProxyChannel channel;

			// 同步控制
			Phaser phaser;

			// 单个请求的共同请求头，第一个任务随机生成，后续任务复用
			Map<String, String> commonHeader;

			volatile boolean commonHeaderSet = false;

			Cookies.Holder cookies;

			public volatile boolean switchProxy = true;

			// 终止标志位
			private volatile boolean done = false;

			/**
			 * 构造器
			 */
			public RequestGroupWrapper(TaskHolder th, ProxyChannel channel) throws InterruptedException {

				this.channel = channel;
				this.className = th.class_name;
				this.fingerprint = th.fingerprint;

				logger.info("*RG {}:[{}]", className, fingerprint);

				queue.put(th);
				phaser = new Phaser(2);
			}

			/**
			 *
			 */
			public void run() {

				while(!done) {

					try {

						TaskHolder th = queue.poll(100, TimeUnit.MILLISECONDS);

						if(th != null) {

							queueFingerprints.remove(th.fingerprint);

							Task t = th.build();

							// RequestGroupWrapper 的第一个task 设置 commonHeader
							if(!commonHeaderSet) {
								commonHeader = t.getHeaders();
								commonHeaderSet = true;
							}

							// 需要进行切换代理操作
							if(switchProxy) {

								changeIp(channel);

								switchProxy = false;
								cookies = null;
							}

							// 等待切换
							waits();

							// 设置代理
							t.setProxy(channel.proxy);

							// Header的处理
							if(commonHeader != null) {

								// 合并 上一次获取的 Cookie
								if (cookies != null) {
									commonHeader.put("Cookie", cookies.getCookies(t.url));
								} else {
									commonHeader.remove("Cookie");
								}

								// 设置 header
								t.setHeaders(commonHeader);
							}

							// 执行任务
							executor.submit(new RequestWrapper(t, this));
						}

					} catch (Exception e) {
						logger.error("Error poll task, ", e);
						return;
					}
				}

				logger.info("RG* {}:[{}]", className, fingerprint);
			}
		}

		/**
		 * 请求封装
		 */
		class RequestWrapper extends BasicDistributor.Operator.RequestWrapper {

			RequestGroupWrapper requestGroup;

			/**
			 *
			 * @param t
			 */
			RequestWrapper(Task t) {
				this(t, null);
			}

			/**
			 * 构造方法
			 * @param t
			 * @param requestGroup
			 */
			RequestWrapper(Task t, RequestGroupWrapper requestGroup) {
				super(t);
				this.requestGroup = requestGroup;
			}

			/**
			 *
			 * @param nth
			 * @throws InterruptedException
			 */
			public void submit(TaskHolder nth) throws InterruptedException {

				String class_name = nth.class_name;
				int template_id = nth.template_id;
				String domain = nth.domain;
				String fingerprint = nth.fingerprint;

				// RequestGroup的第一个任务，失败重试的情况，当前 RequestGroup 被废弃
				// 生成的新任务 需要切换IP
				if(nth.switchProxy() || requestGroup == null) {

					logger.info("Submit to main queue, {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);

					BasicDistributorPG.this.submit(nth);
				}
				// 不需要更新代理的任务，添加到GroupWrapper队列
				else {

					logger.info("Submit to RG queue, {}:[{}] --> {}:{}", class_name, template_id, domain, fingerprint);

					if(BasicDistributorPG.this.submit(nth, requestGroup.queue).success) {

						// 更新phaser计数
						requestGroup.phaser.register();
					}
				}
			}

			@Override
			public void run() {

				super.run();

				// 如果出现验证异常
				if(validatorException || t.exception != null) {

					logger.info("Set switch proxy");

					// Request Group 中
					if(requestGroup != null) {
						requestGroup.switchProxy = true;
					} else {
						switchProxy = true;
					}

					// 之前的IP 一小时内不用
					logger.info("{} banned for 10 min", requestGroup.channel.currentIp);
					bannedIps.put(requestGroup.channel.currentIp, System.currentTimeMillis());

				}

				// Request Group 中 同IP
				if(requestGroup != null) {

					if(requestGroup.cookies == null) {
						requestGroup.cookies = t.getResponse().getCookies();
					} else {
						requestGroup.cookies.add(t.getResponse().getCookies());
					}

					requestGroup.phaser.arriveAndDeregister();
					//logger.info("Phaser* {}", requestGroup.phaser.getUnarrivedParties());
				}
			}
		}
	}
}
