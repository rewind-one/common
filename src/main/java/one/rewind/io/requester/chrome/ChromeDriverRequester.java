package one.rewind.io.requester.chrome;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.auth.AuthType;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import one.rewind.io.requester.proxy.ProxyWrapper;
import one.rewind.util.Configs;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.exception.ChromeDriverException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

public class ChromeDriverRequester implements Runnable {

	protected static ChromeDriverRequester instance;

	private static final Logger logger = LogManager.getLogger(ChromeDriverRequester.class.getName());

	// 连接超时时间
	public static int CONNECT_TIMEOUT;

	// 读取超时时间
	public static int READ_TIMEOUT;

	public static int AGENT_NUM = 4;

	public static String REQUESTER_LOCAL_IP;

	private static ExecutorService requester_executor;

	// 配置设定
	static {

		Config ioConfig = Configs.getConfig(BasicRequester.class);
		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");
		AGENT_NUM = ioConfig.getInt("chromeDriverAgentNum");
		REQUESTER_LOCAL_IP = ioConfig.getString("requesterLocalIp");

		requester_executor = Executors.newSingleThreadExecutor(
				new ThreadFactoryBuilder().setNameFormat("ChromeDriverRequester-%d").build());
	}

	/**
	 *
	 * @return
	 */
	public static ChromeDriverRequester getInstance() {

		if (instance == null) {
			synchronized (ChromeDriverRequester.class) {
				if (instance == null) {
					instance = new ChromeDriverRequester();
					requester_executor.submit(instance);
				}
			}
		}

		return instance;
	}

	private Map<String, Set<ChromeDriverAgent>> domain_agents_map;

	private Map<String, Set<ChromeDriverAgent>> account_agents_map;

	private List<ChromeDriverAgent> agents = new LinkedList<>();

	public PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

	public BlockingQueue<ChromeDriverAgent> idleAgentQueue = new LinkedBlockingQueue<>();

	ThreadPoolExecutor executor = new ThreadPoolExecutor(
			10,
			20,
			0, TimeUnit.MICROSECONDS,
			new LinkedBlockingQueue<>(20));
	//new SynchronousQueue<>()

	private volatile boolean done = false;

	private ChromeDriverRequester() {

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-Worker-%d").build());

		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

	}

	/**
	 *
	 * @param agent
	 */
	public void addChromeDriverAgent(ChromeDriverAgent agent) {

		agents.add(agent);
		agent.setIdleCallback(() -> {
			idleAgentQueue.add(agent);
		}).setNewCallback(() -> {
			idleAgentQueue.add(agent);
		});
	}

	/**
	 * 对本地ChromeDriverAgent进行重新布局
	 */
	public synchronized void layout() {

		int localAgentCount = 0;
		for(ChromeDriverAgent agent : agents) {

			if(!agent.isRemote())
				localAgentCount ++;
		}


		int gap = 600 / (localAgentCount/2);

		int i = 0;
		for(ChromeDriverAgent agent : agents) {

			if(!agent.isRemote()) {
				Random r = new Random();
				Point startPoint = new Point(0 + gap * (i++ / 2), i % 2 == 0 ? 0 : 400);
				agent.setPosition(startPoint);

				agent.setSize(new Dimension(800, 400));
			}
		}

	}

	public void submit(Task task) {
		queue.offer(task);
	}

	@Override
	public void run() {

		while(!done) {

			try {

				final Task t = queue.take();
				logger.info("Get Task: {}", t.getUrl());

				if(t != null) {

					executor.submit(() -> {

						ChromeDriverAgent agent = null;

						try {
							agent = idleAgentQueue.take();
						} catch (InterruptedException e) {
							logger.error("ChromeDriverAgent assignment interrupted, ", e);
						}

						if(agent != null) {

							logger.info("Assign {}", agent.name);

							try {
								agent.submit(t);

								if(t.needRetry()) {
									submit(t);
								}

							} catch (ChromeDriverException.IllegalStatusException e) {
								logger.error("{} status illegal, ", agent.name, e);
							}
						}
					});

				}

			} catch (InterruptedException e) {
				logger.error("Task assignment interrupted, ", e);
			}
		}
	}

	/**
	 *
	 */
	public void close() {

		executor.shutdown();
		for(ChromeDriverAgent agent : agents) {
			agent.stop();
		}
	}

	/**
	 * 初始化一个 BrowserMobProxyServer
	 * 执行时间点：任意
	 * @param proxy upstream proxy address
	 * @return BrowserMobProxyServer
	 */
	public static BrowserMobProxyServer buildBMProxy(ProxyWrapper proxy) {

		BrowserMobProxyServer bmProxy = new BrowserMobProxyServer();
		bmProxy.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
		bmProxy.setRequestTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

		/**
		 * 设定上游代理地址
		 */
		if(proxy != null) {
			bmProxy.setChainedProxy(proxy.getInetSocketAddress());
			bmProxy.chainedProxyAuthorization(proxy.getUsername(), proxy.getPassword(), AuthType.BASIC);
		}

		bmProxy.setTrustAllServers(true);
		bmProxy.setMitmManager(ImpersonatingMitmManager.builder().trustAllServers(true).build());

		// TODO
		try {
			InetAddress address = InetAddress.getByName(REQUESTER_LOCAL_IP);
			bmProxy.start(0, address);
		} catch (UnknownHostException e) {
			bmProxy.start(0); // Use any free port
		}

		return bmProxy;
	}
}
