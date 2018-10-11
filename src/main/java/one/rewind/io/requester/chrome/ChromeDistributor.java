package one.rewind.io.requester.chrome;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import io.netty.handler.codec.http.*;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.auth.AuthType;
import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.route.ChromeTaskRoute;
import one.rewind.io.requester.route.DistributorRoute;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.io.server.MsgTransformer;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class ChromeDistributor {

	public static ChromeDistributor instance;

	public static final Logger logger = LogManager.getLogger(ChromeDistributor.class.getName());

	// 连接超时时间
	public static int CONNECT_TIMEOUT;

	// 读取超时时间
	public static int READ_TIMEOUT;

	// 本地IP
	public static String LOCAL_IP;

	// Web服务端口号
	//public static int WEB_PORT = 80;

	// 配置设定
	static {

		Config ioConfig = Configs.getConfig(BasicRequester.class);

		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");
		LOCAL_IP = ioConfig.getString("requesterLocalIp");
		// LOCAL_IP = NetworkUtil.getLocalIp();
	}

	/**
	 *
	 * @return
	 */
	public static ChromeDistributor getInstance() {

		if (instance == null) {
			synchronized (ChromeDistributor.class) {
				if (instance == null) {
					instance = new ChromeDistributor();
				}
			}
		}

		return instance;
	}

	// 任务队列
	// TODO 使用RBlockingQueue
	public ConcurrentHashMap<ChromeAgent, PriorityBlockingQueue<TaskHolder>> queues
			= new ConcurrentHashMap<>();

	// 域名-账户 Agent 映射
	public Map<String, ChromeAgent> domain_account_agent_map = new HashMap<>();

	// 域名 Agent列表 映射
	public Map<String, List<ChromeAgent>> domain_agent_map = new HashMap<>();

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

	private Date startTime = new Date();

	public volatile int taskCount = 0;

	public ConcurrentHashMap<String, String> responseTypeCache = new ConcurrentHashMap<>();

	/**
	 * 初始化
	 */
	public ChromeDistributor() {

		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDistributor-Worker-%d").build());

		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		post_executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat("ChromeDistributor-PostWorker-%d").build());

		buildHttpApiServer();

	}

	/**
	 *
	 */
	public void buildHttpApiServer() {

		//port(WEB_PORT);

		before("/*", (q, a) -> logger.info("Received api call"));

		get("/distributor", DistributorRoute.getInfo, new MsgTransformer());

		get("/scheduler", DistributorRoute.getSchedulerInfo, new MsgTransformer());

		path("/task", () -> {

			post("", ChromeTaskRoute.submit, new MsgTransformer());

			post("/unschedule/:id", ChromeTaskRoute.unschedule, new MsgTransformer());

		});

		// 适用于跨域调用
		after((request, response) -> {

			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Allow-Methods", "POST, OPTIONS");
			response.header("Access-Control-Allow-Headers", "X-Custom-Header");
			response.header("Access-Control-Max-Age", "1000");

		});
	}

	/**
	 *
	 * @return
	 */
	public ChromeDriverDockerContainer getChromeDriverDockerContainer() {
		return null;
	}

	/**
	 * 添加Agent
	 * @param agent
	 */
	public void addAgent(ChromeAgent agent)
			throws ChromeDriverException.IllegalStatusException, InterruptedException
	{

		CountDownLatch down = new CountDownLatch(1);

		// 空闲回调
		agent.addIdleCallback((a) -> {

			down.countDown();
			a.submit(distribute(a));

		})
		// 启动回调
		.addNewCallback((a) -> {

			// 解锁同步
			down.countDown();
			a.submit(distribute(a));

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

			logger.info("{} {} {}", a.name, account.getDomain(), account.getUsername());

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
		for(ChromeAgent agent : queues.keySet()) {

			if(!agent.isRemote())
				localAgentCount ++;
		}

		if(localAgentCount < 2) return;

		int gap = 600 / (int) Math.ceil(localAgentCount/4);
		int i = 0;
		for(ChromeAgent agent : queues.keySet()) {

			if(!agent.isRemote()) {
				Random r = new Random();
				Point startPoint = new Point(0 + gap * (i++ / 2), i % 2 == 0 ? 0 : 400);
				agent.setPosition(startPoint);

				agent.setSize(new Dimension(800, 400));
			}
		}
	}

	/**
	 * 提交任务到特定队列
	 * @param holder
	 * @return
	 * @throws ChromeDriverException.NotFoundException
	 * @throws AccountException.NotFound
	 */
	public Map<String, Object> submit(TaskHolder holder) throws Exception
	{

		String domain = holder.domain;
		String username = holder.username;
		String account_key = domain + "-" + username;

		ChromeAgent agent;

		// 特定用户的采集任务
		if(holder.username != null && holder.username.length() > 0) {

			agent = domain_account_agent_map.get(account_key);

			if(agent == null) {

				logger.warn("No agent hold account {}.", account_key);
				throw new AccountException.NotFound();
			} else {
				Account account = agent.accounts.get(domain);
				account.use_cnt ++;
				account.update();
			}

		}
		// 需要登录采集的任务 或 没有找到加载指定账户的Agent
		else if(holder.need_login){

			if(!domain_agent_map.keySet().contains(domain)) {
				logger.warn("No agent hold {} accounts.", domain);
				throw new AccountException.NotFound();
			}

			agent = domain_agent_map.get(domain).stream().map(a -> {
				int queue_size = queues.get(a).size();
				return Maps.immutableEntry(a, queue_size);
			})
			.sorted(Map.Entry.<ChromeAgent, Integer>comparingByValue())
			.limit(1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList())
			.get(0);

			if(agent == null) {
				logger.warn("No agent hold domain:{} accounts.", domain);
				throw new AccountException.NotFound();
			} else {
				Account account = agent.accounts.get(domain);
				account.use_cnt ++;
				account.update();
			}

		}
		// 一般任务
		else {

			agent = queues.keySet().stream().map(a -> {
				int queue_size = queues.get(a).size();
				return Maps.immutableEntry(a, queue_size);
			})
			.sorted(Map.Entry.<ChromeAgent, Integer>comparingByValue())
			.limit(1)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList())
			.get(0);
		}

		// 生成指派信息
		if(agent != null) {

			logger.info("Assign {} {} {} to agent:{}.", holder.class_name, domain, username!=null?username:"", agent.name);

			queues.get(agent).put(holder);

			Map<String, Object> info = new HashMap<>();
			info.put("localIp", LOCAL_IP);
			info.put("agent", agent.getInfo());
			info.put("domain", domain);
			info.put("account", username);
			info.put("id", holder.id);

			return info;
		}

		logger.warn("Agent not found for task:{}-{}.", domain, username);

		throw new ChromeDriverException.NotFoundException();
	}

	/**
	 *
	 * @param holder
	 * @param agent
	 */
	public void submit(TaskHolder holder, ChromeAgent agent) {

		// 生成指派信息
		if(agent != null) {

			logger.info("Assign {} to agent:{}.", holder.class_name, agent.name);

			queues.get(agent).put(holder);

			return;

		} else {
			logger.warn("Agent is null.");
		}
	}

	/**
	 * 从阻塞队列中 获取任务
	 * @param agent
	 * @return
	 * @throws InterruptedException
	 */
	public ChromeTask distribute(ChromeAgent agent) throws InterruptedException {

		TaskHolder holder = queues.get(agent).take();

		holder.exec_time = new Date();

		ChromeTask task = null;

		try {

			task = (ChromeTask) holder.build();

			// 任务失败重试逻辑
			task.addDoneCallback((t) -> {

				if(t.needRetry()) {

					// 重试逻辑
					if( t.getRetryCount() < 3 ) {

						t.addRetryCount();
						submit(holder);

					}
					// 失败任务保存到数据库
					else {

						try {
							t.holder.insert();
						} catch (Exception e) {
							logger.error(e);
						}
					}
				}
			});

			logger.info("Task:{} Assign {}.", task.url, agent.name);

			taskCount++;

			return task;

		}
		catch (Exception e) {

			// Recursive call to get task
			logger.error("Task build failed. {} ", holder, e);
			return distribute(agent);
		}
	}

	/**
	 *
	 * @return
	 */
	public Map<String, Object> getInfo() {

		Map<String, Object> info = new TreeMap<>();

		List<Map<String, Object>> agent_info_list = new ArrayList<>();

		for(ChromeAgent agent : queues.keySet()) {

			Map<String, Object> agent_info = agent.getInfo();
			agent_info.put("queueSize", queues.get(agent).size());

			agent_info_list.add(agent_info);
		}

		info.put("agents", agent_info_list);

		info.put("taskCount", taskCount);

		info.put("runTime", new Date().getTime() - startTime.getTime());

		info.put("localIp", LOCAL_IP);

		return info;
	}

	/**
	 * 关闭
	 * TODO 应该将未执行的任务持久化
	 */
	public void close() throws Exception {

		executor.shutdown();
		for(ChromeAgent agent : queues.keySet()) {
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

		/*bmProxy.addRequestFilter((request, contents, messageInfo) -> {
			if(contents != null && contents.getBinaryContents()!=null && proxy!=null) {
				proxy.addSendBytes(contents.getBinaryContents().length);
				proxy.use_cnt ++;
			}
			return null;
		});*/

		// 通过请求uri 判断该资源是否请求过 是否直接bypass
		HttpFiltersSource filter = new HttpFiltersSourceAdapter() {
			@Override
			public HttpFilters filterRequest(HttpRequest originalRequest) {

				return new HttpFiltersAdapter(originalRequest) {

					@Override
					public HttpResponse clientToProxyRequest(HttpObject httpObject) {
						if (httpObject instanceof HttpRequest) {

							if(httpObject != null && httpObject.toString()!=null && proxy!=null) {
								proxy.addSendBytes(httpObject.toString().length());
								proxy.use_cnt ++;
							}
						}

						return super.clientToProxyRequest(httpObject);
					}
				};
			}
		};
		bmProxy.addFirstHttpFilterFactory(filter);

		bmProxy.addResponseFilter((response, contents, messageInfo) -> {
			if(contents != null && contents.getBinaryContents()!=null && proxy!=null) {
				proxy.addRevBytes(contents.getBinaryContents().length);
				proxy.use_cnt ++;
			}
		});

//		try {
//			InetAddress address = InetAddress.getByName(LOCAL_IP);
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
