package one.rewind.io.requester.chrome;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.auth.AuthType;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import java.util.*;
import java.util.concurrent.*;

public class ChromeDriverRequester implements Runnable {

	public static ChromeDriverRequester instance;

	public static final Logger logger = LogManager.getLogger(ChromeDriverRequester.class.getName());

	// 连接超时时间
	public static int CONNECT_TIMEOUT;

	// 读取超时时间
	public static int READ_TIMEOUT;

	public static int AGENT_NUM = 4;

	public static String REQUESTER_LOCAL_IP;

	public static ExecutorService requester_executor;

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

	//
	private Map<String, BlockingQueue<Task>> tasks = new HashMap<>();

	public PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

	private Map<String, Set<ChromeDriverAgent>> domain_agents_map;

	private Map<String, Set<ChromeDriverAgent>> account_agents_map;

	public List<ChromeDriverAgent> agents = new LinkedList<>();

	public BlockingQueue<ChromeDriverAgent> idleAgentQueue = new LinkedBlockingQueue<>();

	ThreadPoolExecutor executor = new ThreadPoolExecutor(
			10,
			20,
			0, TimeUnit.MICROSECONDS,
			//new ArrayBlockingQueue<>(20)
			new SynchronousQueue<>()
	);

	ThreadPoolExecutor post_executor = new ThreadPoolExecutor(
			10,
			10,
			0, TimeUnit.MICROSECONDS,
			new LinkedBlockingQueue<>()
	);

	ThreadPoolExecutor restart_executor = new ThreadPoolExecutor(
			4,
			4,
			0, TimeUnit.MICROSECONDS,
			new LinkedBlockingQueue<>()
	);

	private volatile boolean done = false;

	/**
	 *
	 */
	public ChromeDriverRequester() {

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-Worker-%d").build());

		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		post_executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-PostWorker-%d").build());

		restart_executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-RestartWorker-%d").build());

	}

	/**
	 *
	 * @return
	 */
	public ChromeDriverDockerContainer getChromeDriverDockerContainer() {
		return null;
	}

	/**
	 *
	 * @param agent
	 */
	public void addAgent(ChromeDriverAgent agent) throws ChromeDriverException.IllegalStatusException {

		agents.add(agent);

		agent.addIdleCallback((a) -> {

			idleAgentQueue.add(a);

		}).addNewCallback((a) -> {

			// TODO 此处应该增加登陆操作
			idleAgentQueue.add(a);

		}).addTerminatedCallback((a) -> {

			// agents.remove(agent);
			// idleAgentQueue.remove(agent);

			/*URL newRemoteAddress = null;
			RemoteShell newRemoteShell = null;*/

			// 需要 dockerMgr 终止旧容器
			if(a.remoteAddress != null) {

				// 重启旧容器
				if(a.remoteShell instanceof ChromeDriverDockerContainer) {

					try {
						// logger.info("Remote container: {}", ((ChromeDriverDockerContainer) agent.remoteShell).getRemoteAddress());
						((ChromeDriverDockerContainer) a.remoteShell).rebuild();
						logger.info("Sleep 5s for container restart.");
						Thread.sleep(5000);
					} catch (Exception e) {
						logger.error("Restart container error, ", e);
					}

				} else {
					return;
				}

				/*ChromeDriverDockerContainer container = getChromeDriverDockerContainer();

				if(container != null) {
					try {
						logger.info("Set new remote address: {}", container.getRemoteAddress());
						newRemoteAddress = container.getRemoteAddress();
					} catch (MalformedURLException e) {
						logger.error(e);
					}
					newRemoteShell = container;
				} else {
					// 没有必要创建新的agent
					// agent会越用越少
					return;
				}*/
			}

			/*agent.remoteAddress = newRemoteAddress;
			agent.remoteShell = newRemoteShell;*/

			restart_executor.submit(
					()->{
						try {
							a.start();
						} catch (ChromeDriverException.IllegalStatusException e) {
							logger.error("{} status:{}", a.name, a.status, e);
						}
					}
			);

			/*ChromeDriverAgent new_agent = new ChromeDriverAgent(
					newRemoteAddress,
					newRemoteShell,
					agent.proxy,
					agent.flags.toArray(new ChromeDriverAgent.Flag[agent.flags.size()])
			);

			// 把原来的newCallbacks复制到新agent
			List<Runnable> newCallbacks = agent.newCallbacks;
			newCallbacks.remove(newCallbacks.size()-1);
			if(newCallbacks.size() > 0) {
				new_agent.newCallbacks = newCallbacks;
			}

			try {
				addAgent(new_agent);
				new_agent.start();
			} catch (ChromeDriverException.IllegalStatusException e) {
				logger.error("Can't add callbacks for new agent, ", e);
			}*/

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

		if(localAgentCount < 2) return;

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

	/**
	 *
	 * @param task
	 */
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

							t.addDoneCallback(() -> {

								if(t.needRetry()) {
									if( t.getRetryCount() < 3 ) {

										t.addRetryCount();
										submit(t);

									} else {

										try {
											t.insert();
										} catch (Exception e) {
											logger.error(e);
										}
									}
								}
							});

							// TODO 如果Agent出现了错误 导致task需要retry
							// 此时 原agent 时不能提交的
							try {
								agent.submit(t);
							} catch (ChromeDriverException.IllegalStatusException e) {
								logger.error("{}, ", agent.name, e);
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
	public void close() throws ChromeDriverException.IllegalStatusException {

		executor.shutdown();
		for(ChromeDriverAgent agent : agents) {
			agent.clearTerminatedCallbacks();
			agent.destroy();
		}
	}

	/**
	 * 初始化一个 BrowserMobProxyServer
	 * 执行时间点：任意
	 * @param proxy upstream proxy address
	 * @return BrowserMobProxyServer
	 */
	public static BrowserMobProxyServer buildBMProxy(int localPort, one.rewind.io.requester.proxy.Proxy proxy) {

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

		bmProxy.start(localPort);
//		try {
//			InetAddress address = InetAddress.getByName(REQUESTER_LOCAL_IP);
//			bmProxy.start(localPort, address);
//		} catch (UnknownHostException e) {
//			bmProxy.start(localPort); // Use any free port
//		}

		return bmProxy;
	}

	public static BrowserMobProxyServer buildBMProxy(one.rewind.io.requester.proxy.Proxy proxy) {
		return buildBMProxy(0, proxy);
	}
}
