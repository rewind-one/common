package one.rewind.io.requester.chrome;

import com.google.common.collect.Maps;
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
import java.util.stream.Collectors;

public class ChromeDriverRequester {

	public static ChromeDriverRequester instance;

	public static final Logger logger = LogManager.getLogger(ChromeDriverRequester.class.getName());

	// 连接超时时间
	public static int CONNECT_TIMEOUT;

	// 读取超时时间
	public static int READ_TIMEOUT;

	public static int AGENT_NUM = 4;

	public static String REQUESTER_LOCAL_IP;

	// 配置设定
	static {

		Config ioConfig = Configs.getConfig(BasicRequester.class);

		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");
		AGENT_NUM = ioConfig.getInt("chromeDriverAgentNum");
		REQUESTER_LOCAL_IP = ioConfig.getString("requesterLocalIp");
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
				}
			}
		}

		return instance;
	}

	private ConcurrentHashMap<ChromeDriverAgent, PriorityBlockingQueue<Task>> queues = new ConcurrentHashMap<>();

	private Map<String, ChromeDriverAgent> domain_account_agent_map = new HashMap<>();

	private Map<String, List<ChromeDriverAgent>> domain_agent_map = new HashMap<>();

	// 任务 Wrapper 线程池
	ThreadPoolExecutor executor = new ThreadPoolExecutor(
			10,
			20,
			0, TimeUnit.MICROSECONDS,
			//new ArrayBlockingQueue<>(20)
			new SynchronousQueue<>()
	);

	// 后续任务线程池
	ThreadPoolExecutor post_executor = new ThreadPoolExecutor(
			10,
			10,
			0, TimeUnit.MICROSECONDS,
			new LinkedBlockingQueue<>()
	);

	private volatile boolean done = false;

	/**
	 * 初始化
	 */
	public ChromeDriverRequester() {

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-Worker-%d").build());

		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		post_executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDriverRequester-PostWorker-%d").build());

	}

	/**
	 *
	 * @return
	 */
	public ChromeDriverDockerContainer getChromeDriverDockerContainer() {
		return null;
	}

	/**
	 * 获取任务
	 * @param agent
	 * @return
	 * @throws InterruptedException
	 */
	public Task getTask(ChromeDriverAgent agent) throws InterruptedException {

		Task t = queues.get(agent).take();

		// 任务失败重试逻辑
		t.addDoneCallback(() -> {

			if(t.needRetry()) {

				// 重试逻辑
				if( t.getRetryCount() < 3 ) {

					t.addRetryCount();
					submit(t);

				}
				// 失败任务保存到数据库
				else {

					try {
						t.insert();
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}
		});

		logger.info("Assign {}.", agent.name);
		return t;
	}

	/**
	 * 添加Agent
	 * @param agent
	 */
	public void addAgent(ChromeDriverAgent agent)
			throws ChromeDriverException.IllegalStatusException, InterruptedException
	{

		CountDownLatch down = new CountDownLatch(1);

		// 空闲回调
		agent.addIdleCallback((a) -> {

			a.submit(getTask(a));

		})
		// 启动回调
		.addNewCallback((a) -> {

			// 解锁同步
			down.countDown();

			a.submit(getTask(a));

		})
		// 失败回调
		.addTerminatedCallback((a) -> {

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
			}

			// 重启Agent
			try {
				a.start();
			} catch (InterruptedException | ChromeDriverException.IllegalStatusException e) {
				logger.error("{} status:{}", a.name, a.status, e);
			}

		});

		// 账户登录回调
		agent.accountAddCallback = (a, account) -> {

			domain_agent_map.computeIfAbsent(account.getDomain(), k -> new ArrayList<>());

			domain_agent_map.get(account.getDomain()).add(a);

			domain_account_agent_map.put(account.getDomain() + "-" + account.getUsername(), a);

		};

		// 账户退出回调
		agent.accountRemoveCallback = (a, account) -> {

			domain_agent_map.computeIfAbsent(account.getDomain(), k -> new ArrayList<>());

			domain_agent_map.get(account.getDomain()).remove(a);

			domain_account_agent_map.remove(account.getDomain() + "-" + account.getUsername());

		};

		// 添加Queue
		queues.put(agent, new PriorityBlockingQueue<>());

		// 在单独进程执行
		executor.submit(() -> {
			try {
				agent.start();
			} catch (InterruptedException | ChromeDriverException.IllegalStatusException e) {
				logger.error("{} add failed, status:{}. ", agent.name, agent.status, e);
			}
		});

		down.await();
	}


	/**
	 * 对本地ChromeDriverAgent进行重新布局
	 */
	public synchronized void layout() {

		int localAgentCount = 0;
		for(ChromeDriverAgent agent : queues.keySet()) {

			if(!agent.isRemote())
				localAgentCount ++;
		}

		if(localAgentCount < 2) return;

		int gap = 600 / (localAgentCount/2);

		int i = 0;
		for(ChromeDriverAgent agent : queues.keySet()) {

			if(!agent.isRemote()) {
				Random r = new Random();
				Point startPoint = new Point(0 + gap * (i++ / 2), i % 2 == 0 ? 0 : 400);
				agent.setPosition(startPoint);

				agent.setSize(new Dimension(800, 400));
			}
		}

	}

	/**
	 * 提交任务
	 * @param task
	 */
	public void submit(Task task) {

		String domain = task.getDomain();
		String username = task.getUsername();

		ChromeDriverAgent agent;

		// 特定用户的采集任务
		if(username != null) {

			String account_key = domain + "-" + username;

			agent = domain_account_agent_map.get(account_key);

			task.setPriority(Task.Priority.HIGHER);

		}
		// 需要登录采集的任务
		else if(task.isLoginTask()){

			if(!domain_agent_map.keySet().contains(domain)) {
				logger.warn("No agent has {} login accounts.", domain);
				return;
			}

			agent = domain_agent_map.get(domain).stream().map(a -> {
				int queue_size = queues.get(a).size();
				return Maps.immutableEntry(a, queue_size);
			})
			.sorted(Map.Entry.<ChromeDriverAgent, Integer>comparingByValue())
			.limit(1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList())
			.get(0);
		}
		// 一般任务
		else {

			agent = queues.keySet().stream().map(a -> {
				int queue_size = queues.get(a).size();
				return Maps.immutableEntry(a, queue_size);
			})
			.sorted(Map.Entry.<ChromeDriverAgent, Integer>comparingByValue())
			.limit(1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList())
			.get(0);
		}

		if(agent != null) {
			logger.info("Assign task:{}-{} to Agent:{}.", domain, username, agent.name);
			queues.get(agent).put(task);
		} else {
			logger.warn("Agent not found for {}-{}.", domain, username);
		}

	}

	/**
	 * 关闭
	 * TODO 应该将未执行的任务持久化
	 */
	public void close() throws ChromeDriverException.IllegalStatusException, InterruptedException {

		executor.shutdown();
		for(ChromeDriverAgent agent : queues.keySet()) {
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
