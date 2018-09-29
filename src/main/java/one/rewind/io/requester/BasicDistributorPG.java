package one.rewind.io.requester;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.rewind.io.requester.task.Task;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

public class BasicDistributorPG extends BasicDistributor {

	/**
	 *
	 * @return
	 */
	public static BasicDistributor getInstance() {

		if (instance == null) {
			synchronized (BasicDistributor.class) {
				if (instance == null) {
					instance = new BasicDistributorPG();
					instance.setName("BasicDistributorPG");
					instance.start();
				}
			}
		}

		return instance;
	}

	private volatile boolean done = false;

	// 上一次更换代理时间
	public long lastChangeIpTime = System.currentTimeMillis();

	/**
	 * 私有构造器
	 */
	private BasicDistributorPG() {

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("BasicDistributorPG-%d").build());
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

					logger.info("*RG {}:[{}]", t.getClass().getSimpleName(), t.getFingerprint());

					RequestGroupWrapper gw = new RequestGroupWrapper(t);
					executor.submit(gw);

					gw.phaser.arriveAndAwaitAdvance();
					gw.done = true;

					logger.info("RG* {}:[{}]", t.getClass().getSimpleName(), t.getFingerprint());

				} else {

					executor.submit(new RequestWrapper(t));
					waits();

				}

				logger.info("[{} / {} / {}]", executor.getActiveCount(), executor.getQueue().size(), queue.size());

			} catch (Exception e) {

				logger.error("{}", t.toJSON(), e);
			}
		}
	}

	/**
	 * 切换IP任务等待
	 * @throws InterruptedException
	 */
	public synchronized void waitForChangeProxy() throws InterruptedException {

		// 获取当前时间
		long ct = System.currentTimeMillis();

		// 计算两次换IP时间间隔
		long ts = ct - lastChangeIpTime;

		// 若时间间隔过短， 进行延时
		if(ts < 1200) {
			ts = 1200;
			Thread.sleep(ts);
			logger.info("Wait for change proxy: {} ms", ts);
		}

		// 记录此次换IP时间
		lastChangeIpTime = System.currentTimeMillis();
	}

	/**
	 * 请求组封装
	 * 在同组中的出口IP是唯一的
	 */
	class RequestGroupWrapper implements Runnable {

		// 请求组内部任务对接
		BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

		// 同步控制
		Phaser phaser;

		// 单个请求的共同请求头，第一个任务随机生成，后续任务复用
		Map<String, String> commonHeader;

		public volatile boolean switchProxy = false;

		// 终止标志位
		private volatile boolean done = false;

		/**
		 * 构造器
		 */
		public RequestGroupWrapper(Task t) throws InterruptedException {
			queue.put(t);
			commonHeader = t.getHeaders();
			phaser = new Phaser(2);
		}

		/**
		 *
		 */
		public void run() {

			while(!done) {

				try {

					Task t = queue.poll(1, TimeUnit.SECONDS);

					if(t != null) {

						// 设置header
						t.setHeaders(commonHeader);

						if(switchProxy) {

							t.addHeader("Proxy-Switch-Ip","yes");
							waitForChangeProxy(); // TODO
							switchProxy = false;
						}

						// 切换代理等待
						if(t.switchProxy()) {
							waitForChangeProxy();
						}

						// 执行任务
						executor.submit(new RequestWrapper(t, this));

						waits();
					}

				} catch (Exception e) {
					logger.error("Error poll task, ", e);
				}
			}
		}
	}

	/**
	 * 请求封装
	 */
	class RequestWrapper extends BasicDistributor.RequestWrapper {

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
		 * @param nt
		 * @throws InterruptedException
		 */
		public void submit(Task nt) {

			nt.removeHeader("Proxy-Switch-Ip");
			nt.getExceptions().clear();

			if(nt.switchProxy()) {

				logger.info("Submit to main queue.");

				BasicDistributorPG.this.submit(nt);
			}
			// 不需要更新代理的任务，添加到GroupWrapper队列
			else {

				logger.info("Submit to RG queue.");

				BasicDistributorPG.this.submit(nt, requestGroup.queue);

				// 更新phaser计数
				if(requestGroup != null) {
					requestGroup.phaser.register();
				}
			}
		}

		@Override
		public void run() {

			super.run();

			// 如果出现验证异常
			if(validatorException || t.getExceptions().size() > 0) {

				// Request Group 中
				if(requestGroup != null) {
					requestGroup.switchProxy = true;
					logger.info("Set RG switch proxy");
				}
			}

			// Request Group 中 同IP
			if(requestGroup != null) {
				requestGroup.phaser.arriveAndDeregister();
			}
		}
	}
}
