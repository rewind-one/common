package one.rewind.io.requester.chrome;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import io.netty.handler.codec.http.*;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.callback.*;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.chrome.action.LoginAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.io.requester.util.DocumentSettleCondition;
import one.rewind.io.ssh.RemoteShell;
import one.rewind.util.Configs;
import one.rewind.util.EnvUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Security;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.rewind.io.requester.chrome.ChromeDistributor.LOCAL_IP;

/**
 * Chrome请求器
 * 复用
 * karajan@2017.9.17
 */
public class ChromeAgent {

	public static final Logger logger = LogManager.getLogger(ChromeAgent.class.getName());

	// 获取元素超时时间
	private static int GET_ELEMENT_TIMEOUT = 3;

	// 启动超时时间
	private static int INIT_TIMEOUT = 120000;

	// 关闭超时时间
	private static int CLOSE_TIMEOUT = 120000;

	// 页面加载判定时间
	private static long PAGE_LOAD_TIMEOUT = 10000;

	// 配置设定
	static {

		// A. 读取配置文件
		Config ioConfig = Configs.getConfig(BasicRequester.class);

		try {
			PAGE_LOAD_TIMEOUT = ioConfig.getLong("pageLoadTimeout");
		} catch (Exception e) {
			logger.error(e);
		}

		// B. 设定chromedriver executable
		if (EnvUtil.isHostLinux()) {
			System.setProperty("webdriver.chrome.driver", ioConfig.getString("chromeDriver"));
		} else {
			System.setProperty("webdriver.chrome.driver", ioConfig.getString("chromeDriver") + ".exe");
		}

		// C. Set log file path
		System.setProperty("webdriver.chrome.logfile", "webdriver.chrome.log");
		System.setProperty("webdriver.chrome.args", "--disable-logging");

		// D. Add BouncyCastleProvider 接受特定环境的HTTPS策略
		Security.addProvider(new BouncyCastleProvider());
	}

	// 所有实例的列表
	public static final List<ChromeAgent> instances = new ArrayList<>();

	// 名称
	public String name;

	// 参数标签
	public Set<Flag> flags;

	public URL remoteAddress;

	public RemoteShell remoteShell;

	// 代理地址
	public one.rewind.io.requester.proxy.Proxy proxy;

	// 存储用于启动 ChromeDriver 的 capabilities
	private DesiredCapabilities capabilities;

	// ChromeDriver 句柄
	private RemoteWebDriver driver;

	// 代理服务器
	public BrowserMobProxyServer bmProxy;

	// Cluster MITM 本地端口
	public int bmProxy_port = 0;

	// HTTP 请求过滤器
	private boolean requestFilterEnabled;

	// HTTP 返回过滤器
	private boolean responseFilterEnabled;

	// Executor Queue
	private LinkedBlockingQueue queue = new LinkedBlockingQueue<Runnable>();

	// Executor
	private ThreadPoolExecutor executor;

	// 状态
	volatile Status status = Status.STARTING;

	// 账户信息
	public ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

	// 账户登录回调
	public AccountCallback accountAddCallback;

	// 账户失效回调
	public AccountCallback accountRemoveCallback;

	// 重启周期
	int cycle = 0;

	// 上次启动时间
	public Date init_time;

	// 执行任务数量
	public int task_count = 0;

	// 产生异常数量
	public int exception_count = 0;

	// Agent状态信息
	public enum Status {
		INIT, // 新创建
		STARTING, // 启动中
		NEW, // 启动完成，待使用 TODO 需要改名 存在混淆
		BUSY, // 处理采集任务
		IDLE, // 空闲
		STOPPING, // 终止中
		TERMINATED, // 停止
		FAILED, // 失败状态
		DESTROYED
	}

	// 启动回调
	List<ChromeDriverAgentCallback> newCallbacks = new ArrayList<>();

	// 空闲回调
	private List<ChromeDriverAgentCallback> idleCallbacks = new ArrayList<>();

	// 终止回调
	List<ChromeDriverAgentCallback> terminatedCallbacks = new ArrayList<>();

	// 账户异常回调
	private List<AccountCallback> accountFailedCallbacks = new ArrayList<>();

	// 账户冻结回调
	private List<AccountCallback> accountFrozenCallbacks = new ArrayList<>();

	// 代理被封禁回调
	private List<ProxyCallBack> proxyFailedCallbacks = new ArrayList<>();

	// 代理超时回调
	private List<ProxyCallBack> proxyTimeoutCallbacks = new ArrayList<>();

	/**
	 * 启动标签类
	 */
	public enum Flag {
		REMOTE,
		MITM // 设定MITM 未启用
	}

	/**
	 * 初始化类
	 */
	public class Init implements Callable<Boolean> {

		public Boolean call() throws Exception {

			logger.info("Init...");

			capabilities = buildCapabilities();

			driver = null;

			File userDir = new File("chrome_user_dir/" + this.hashCode());
			logger.info("User dir: {}", userDir.getAbsolutePath());

			// 启动 Chrome
			if(remoteAddress != null) {
				driver = new RemoteWebDriver(remoteAddress, capabilities);
			} else {
				driver = new ChromeDriver(new ChromeOptions().merge(capabilities));
			}

			logger.info("Create chromedriver: [{}] done, cycle:{}", name, ++cycle);

			// 设置脚本运行超时参数
			driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);

			// 设置等待超时参数
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

			// 这个值要设置的比较大
			// 否则会出现 org.openqa.selenium.TimeoutException: timeout: cannot determine loading status
			// karajan 2018/4/4
			driver.manage().timeouts().pageLoadTimeout(300, TimeUnit.SECONDS);

			init_time = new Date();

			return true;
		}
	}

	/**
	 * 关闭当前 ProxyServer / chromeDriver
	 * 并尝试删除 chromedriver / chrome pid
	 */
	public class Stop implements Callable<Void> {

		public Void call() {

			// 关闭 ProxyServer
			if(bmProxy != null && !bmProxy.isStopped()) {
				bmProxy.stop();
			}

			bmProxy = null;

			// 关闭ChromeDriver
			if(driver != null) {

				logger.info("Stopping [{}] ...", name);

				// TODO 确认这部分代码的作用
				for (String handle : driver.getWindowHandles()) {
					driver.switchTo().window(handle);
				}

				driver.close();
				driver.quit();
				driver = null;

				/*synchronized(instances) {
					instances.remove(this);
				}*/


				logger.info("[{}] stopped.", name);
			}

			return null;
		}
	}

	/**
	 * 更改代理IP
	 */
	public class ChangeProxy implements Callable<Void> {

		one.rewind.io.requester.proxy.Proxy proxy = null;

		public ChangeProxy(one.rewind.io.requester.proxy.Proxy proxy) {
			this.proxy = proxy;
			ChromeAgent.this.proxy = proxy;
		}

		public Void call() throws ProxyException.Failed {

			if(proxy == null || !proxy.validate()) {
				throw new ProxyException.Failed(proxy);
			}

			// 关闭 ProxyServer
			if(bmProxy != null && !bmProxy.isStopped()) {
				bmProxy.stop();
			}

			bmProxy = ChromeDistributor.buildBMProxy(bmProxy_port, proxy);

			logger.info("Change to {}:{}", proxy.host, proxy.port);

			// TODO set request / response filter

			return null;
		}
	}

	/**
	 * 任务封装
	 * @author karajan@tfelab.org
	 * 2017年3月22日 上午10:04:11
	 */
	class Wrapper implements Callable<Void> {

		ChromeTask task;

		/**
		 * @param task 采集任务
		 */
		Wrapper(ChromeTask task) {
			this.task = task;
			this.task.start_time = new Date();
		}

		/**
		 * 主要任务执行方法
		 */
		public Void call() throws Exception {

			logger.info("{}", task.url);

			// 当使用Request/Response Filters时
			// 需要重置bmProxy
			// 参考：https://github.com/lightbody/browsermob-proxy/issues/491
			if(task.getRequestFilter() != null || task.getResponseFilter() != null) {

				if(bmProxy != null) {

					logger.info("Restart BMProxy to add Request/Response filter.");

					bmProxy.stop();

					bmProxy = ChromeDistributor.buildBMProxy(bmProxy_port, proxy);

					logger.info("Restart BMProxy done.");
				}
			}

			if(task.noFetchImages) {
				setImageBypassFilters();
			}

			// 设定ProxyRequestFilter
			if(task.getRequestFilter() != null) {
				setProxyRequestFilter(task.getRequestFilter());
			}

			// 设定ProxyResponseFilter
			if(task.getResponseFilter() != null) {
				setProxyResponseFilter(task.getResponseFilter());
			}

			getUrl(task.url);
			waitPageLoad(task.url);

			// 正常解析到页面
			if(!driver.getCurrentUrl().matches("^data:.+?")) {

				boolean actionSuccess = true;

				// 先执行完Actions
				for(ChromeAction action : task.getActions()) {

					// 当前线程执行 action
					boolean currentActionSuccess = action.run(ChromeAgent.this);

					// 分domain记录当前Chrome登陆的账户
					// 一个 Chrome 一个 domain 下只能记录一个 账户
					if(action instanceof LoginAction && currentActionSuccess) {

						LoginAction action_ = (LoginAction) action;

						// 更新账户信息
						addLoginInfo(action_.getAccount().domain, action_.getAccount());
					}

					actionSuccess = actionSuccess && currentActionSuccess;
				}

				// 再Set Text
				task.getResponse().setActionDone(actionSuccess);
				task.getResponse().setSrc(getAllSrc().getBytes());
				task.getResponse().setText();

				if(task.buildDom()){
					task.getResponse().buildDom();
				}

				// 对内容进行验证
				if(task.validator != null)
					task.validator.run(ChromeAgent.this, task);

				if(task.shootScreen()) {
					task.getResponse().setScreenshot(driver.getScreenshotAs(OutputType.BYTES));
				}
			}

			task.setDuration();

			//
			if(proxy != null) {
				proxy.success();
			}

			// 停止页面加载
			// driver.executeScript("window.stop()");

			if(ChromeAgent.this.requestFilterEnabled) {
				setProxyRequestFilter((request, contents, messageInfo) -> {
					return null;
				});
			}

			if(ChromeAgent.this.responseFilterEnabled) {
				setProxyResponseFilter((response, contents, messageInfo) -> {

				});
			}

			return null;
		}
	}

	/**
	 *
	 * @param flags
	 */
	public ChromeAgent(Flag... flags) {
		this(null, null, null, flags);
	}

	/**
	 *
	 * @param remoteAddress
	 * @param flags
	 */
	public ChromeAgent(URL remoteAddress, RemoteShell remoteShell, Flag... flags) {
		this(remoteAddress, remoteShell, null, flags);
	}

	/**
	 *
	 * @param proxy
	 * @param flags
	 */
	public ChromeAgent(one.rewind.io.requester.proxy.Proxy proxy, Flag... flags) {
		this(null, null, proxy, flags);
	}

	/**
	 *
	 * @param proxy 代理
	 * @param flags 启动标签
	 */
	public ChromeAgent(URL remoteAddress, RemoteShell remoteShell, one.rewind.io.requester.proxy.Proxy proxy, Flag... flags) {

		status = Status.INIT;

		this.remoteAddress = remoteAddress;
		this.remoteShell = remoteShell;
		this.proxy = proxy;
		this.flags = new HashSet<Flag>(Arrays.asList(flags));

		synchronized (instances) {
			instances.add(ChromeAgent.this);
			name = "ChromeAgent-" + instances.size();
		}

		// 初始化单线程执行器
		executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
		executor.setThreadFactory(new ThreadFactoryBuilder()
				.setNameFormat(name + "-%d").build());
	}

	/**
	 * 启动
	 */
	public synchronized ChromeAgent start() throws ChromeDriverException.IllegalStatusException, InterruptedException {

		if(!(status == Status.INIT || status == Status.TERMINATED)) {
			throw new ChromeDriverException.IllegalStatusException();
		}

		status = Status.STARTING;

		//
		Future<Boolean> initFuture = executor.submit(new Init());

		try {

			boolean initSuccess = initFuture.get(INIT_TIMEOUT, TimeUnit.MILLISECONDS);

			status = Status.NEW;

			if(initSuccess) {
				logger.info("INIT[{}] done.", cycle);
			} else {
				logger.error("INIT[{}] done, Auto scripts exec failed.", cycle);
			}

			// 执行状态回调函数
			runCallbacks(newCallbacks);

		} catch (InterruptedException e) {

			status = Status.FAILED;
			logger.error("{} INIT interrupted. ", name, e);
			stop();

		} catch (ExecutionException e) {

			status = Status.FAILED;
			logger.error("{} INIT failed. ", name, e.getCause());
			stop();

		} catch (TimeoutException e) {

			initFuture.cancel(true);

			status = Status.FAILED;
			logger.error("{} INIT failed. ", name, e);
			stop();
		}

		return this;
	}

	/**
	 * 停止
	 */
	public synchronized void stop() throws ChromeDriverException.IllegalStatusException, InterruptedException {

		if(! (status == Status.IDLE || status == Status.NEW || status == Status.BUSY || status == Status.FAILED) ) {
			throw new ChromeDriverException.IllegalStatusException();
		}

		status = Status.STOPPING;

		Future<Void> closeFuture = executor.submit(new Stop());

		try {

			closeFuture.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
			status = Status.TERMINATED;
			logger.info("Stop done.");

		}
		catch (InterruptedException e) {

			status = Status.FAILED;
			logger.error("Stop interrupted. ", e);

		}
		catch (ExecutionException e) {

			status = Status.TERMINATED;
			logger.error("Stop failed. ", e.getCause());

		}
		catch (TimeoutException e) {

			closeFuture.cancel(true);
			status = Status.FAILED;
			logger.error("Stop failed. ", e);
		}

		runCallbacks(terminatedCallbacks);
	}

	/**
	 *
	 * @throws Exception
	 */
	public synchronized void destroy() throws Exception {

		terminatedCallbacks.clear();

		stop();

		executor.shutdown();

		instances.remove(ChromeAgent.this);

		if(proxy != null) {
			proxy.status = one.rewind.io.requester.proxy.Proxy.Status.Free;
			proxy.update();
		}

		status = Status.DESTROYED;
	}

	/**
	 * 生成 Capabilities
	 * 执行时间点：任意
	 * @return
	 */
	private DesiredCapabilities buildCapabilities() throws UnknownHostException {

		DesiredCapabilities capabilities = DesiredCapabilities.chrome();

		/*ChromeOptions options_ = new ChromeOptions();
		options_.addArguments("--log-level=3");
		options_.addArguments("--silent");*/

		// Set no loading images
		// 此处代码可能没有效果
		/*Map<String, Object> contentSettings = new HashMap<String, Object>();
		contentSettings.put("images", 2);

		Map<String, Object> preferences = new HashMap<String, Object>();
		preferences.put("profile.default_content_settings", contentSettings);

		capabilities.setCapability("chrome.prefs", preferences);*/

		// 设置 chromedriver.exe 日志级别
		LoggingPreferences logPrefs = new LoggingPreferences();
		logPrefs.enable(LogType.PERFORMANCE, Level.INFO);

		// 设置 Capabilities
		capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

		/*Map<String, Object> perfLogPrefs = new HashMap<String, Object>();
		perfLogPrefs.put("traceCategories", "browser,devtools.timeline,devtools"); // comma-separated trace categories*/

		// 设定 Chrome 代理
		if(proxy != null || flags.contains(Flag.MITM)) {

			Proxy seleniumProxy;

			bmProxy = ChromeDistributor.buildBMProxy(proxy);
			bmProxy_port = bmProxy.getPort();

			// 重载 本地代理服务器网络地址 InetAddress.getLocalHost()
			seleniumProxy = ClientUtil.createSeleniumProxy(bmProxy, InetAddress.getByName(LOCAL_IP));


			capabilities.setCapability("proxy", seleniumProxy);
		}

		// 设定Session超时后重启动
		capabilities.setCapability("recreateChromeDriverSessions", true);
		capabilities.setCapability("newCommandTimeout", 120);

		// 只加载html的DOM，不会加载js
		// https://stackoverflow.com/questions/43734797/page-load-strategy-for-chrome-driver
		capabilities.setCapability("pageLoadStrategy", "none");

		// 禁用页面提示信息
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("profile.default_content_setting_values.notifications", 2);

		// ChromeOptions 设定 增强Chrome稳定性
		ChromeOptions options = new ChromeOptions();
		/*options.setExperimentalOption("perfLoggingPrefs", perfLogPrefs);
		options.addArguments("user-data-dir=" + userDir.getAbsolutePath());*/
		options.addArguments("--no-sandbox");
		/*options.addArguments("--start-maximized");*/
		options.addArguments("--dns-prefetch-disable");
		options.addArguments("--disable-gpu-watchdog");
		options.addArguments("--disable-gpu-program-cache");
		options.addArguments("--disable-software-rasterizer");
		options.addArguments("--disable-gpu"); // 关闭GPU加速
		options.addArguments("--disk-cache-dir=/dev/null");
		options.addArguments("--disk-cache-size=1");
		options.addArguments("--timeout=28000"); // 	Issues a stop after the specified number of milliseconds. This cancels all navigation and causes the DOMContentLoaded event to fire.
		options.addArguments("--ipc-connection-timeout=28"); // Overrides the timeout, in seconds, that a child process waits for a connection from the browser before killing itself. ↪
        options.addArguments("--disable-setuid-sandbox");

		/*options.addArguments("--log-level=3");
		options.addArguments("--silent");*/

		// 解决Selenium最大化报错问题
		options.addArguments("--start-maximized");

		options.setExperimentalOption("prefs", prefs);
		options.setExperimentalOption("detach", true);

		// 加载禁用图片插件
		/*File block_image_crx = new File("chrome_ext/Block-image_v1.0.crx");
		if (block_image_crx.exists()) {
			options.addExtensions(new File("chrome_ext/Block-image_v1.0.crx"));
		}*/
		capabilities.setCapability(ChromeOptions.CAPABILITY, options);

		return capabilities;
	}


	public boolean isRemote() {
		return this.remoteAddress != null;
	}

	/**
	 *
	 */
	public void setImageBypassFilters() throws Exception {

		if(bmProxy == null) throw new Exception("BrowserMob Proxy is not set.");

		// 通过请求uri 判断该资源是否请求过 是否直接bypass
		HttpFiltersSource filter = new HttpFiltersSourceAdapter() {
			@Override
			public HttpFilters filterRequest(HttpRequest originalRequest) {

				return new HttpFiltersAdapter(originalRequest) {

					@Override
					public HttpResponse clientToProxyRequest(HttpObject httpObject) {
						if (httpObject instanceof HttpRequest) {

							HttpRequest httpRequest = (HttpRequest) httpObject;

							String type = ChromeDistributor.getInstance().responseTypeCache.get(httpRequest.uri());

							if(type != null &&
								(
									type.contains("image")
									|| type.contains("application/octet-stream") &&
											( httpRequest.uri().contains("jpg") || httpRequest.uri().contains("png") || httpRequest.uri().contains("gif"))
								)
							) {
								HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
								return httpResponse;
							}
						}

						return super.clientToProxyRequest(httpObject);
					}
				};
			}
		};

		if(!this.requestFilterEnabled) {
			this.requestFilterEnabled = true;
			bmProxy.addFirstHttpFilterFactory(filter);
		} else {
			bmProxy.replaceFirstHttpFilterFactory(filter);
		}

		// 在ResponseFilter对返回的资源进行记录
		// 使用uri的原因 https 的 httpRequest 无法获取完整 url
		this.setProxyResponseFilter((response, contents, messageInfo) -> {

			if(contents != null) {

				String contentType = contents.getContentType();
				String uri = messageInfo.getOriginalRequest().uri();

				/*System.err.println(contentType + "\t" + uri);*/

				ChromeDistributor.getInstance().responseTypeCache.put(uri, contentType);
			}
		});
	}

	/**
	 * MITM 监听
	 * 对请求信息进行过滤监听
	 * @param requestFilter 请求过滤器
	 */
	public void setProxyRequestFilter(RequestFilter requestFilter) throws Exception {

		if(bmProxy == null) throw new Exception("BrowserMob Proxy is not set.");

		// 第1次设定
		if(!this.requestFilterEnabled) {
			this.requestFilterEnabled = true;
			bmProxy.addRequestFilter(requestFilter);

		}
		// 第2 ~ n次设定
		else {
			bmProxy.replaceFirstHttpFilter(requestFilter);
		}
	}

	/**
	 * MITM 监听
	 * 对请求信息进行过滤监听
	 * @param responseFilter 响应过滤器
	 */
	public void setProxyResponseFilter(ResponseFilter responseFilter) throws Exception {

		if(bmProxy == null) throw new Exception("BrowserMob Proxy is not set.");

		// 第1次设定
		if(!this.responseFilterEnabled) {
			this.responseFilterEnabled = true;
			bmProxy.addResponseFilter(responseFilter);
		}
		// 第2 ~ n次设定
		else {
			bmProxy.replaceLastHttpFilter(responseFilter);
		}
	}

	/**
	 * @return ChromeDriver 对象
	 */
	public RemoteWebDriver getDriver() {

		return driver;
	}

	/**
	 * 设定Chrome窗口大小
	 * @param dimension
	 */
	public void setSize(Dimension dimension) {

		if(driver != null) {
			// dimension = new Dimension(1024, 600);
			driver.manage().window().setSize(dimension);
		}
	}

	/**
	 * 设定Chrome窗口在屏幕位置
	 * @param startPoint
	 */
	public void setPosition(Point startPoint) {
		if(driver != null) {
			/*Random r = new Random();
			startPoint = new Point(60 * r.nextInt(10), 40 * r.nextInt(10));*/
			driver.manage().window().setPosition(startPoint);
		}
	}

	/**
	 * 找到特定元素
	 * @param path
	 * @return
	 */
	public WebElement getElementWait(String path) {
		WebDriverWait wait = new WebDriverWait(driver, GET_ELEMENT_TIMEOUT);
		return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(path)));
	}

	/**
	 * 在特定元素上执行JavaScript脚本
	 * Really should only be used when the web driver is sucking at exposing
	 * functionality natively
	 * @param script The script to execute
	 * @param element The target of the script, referenced as arguments[0]
	 */
	public void trigger(String script, WebElement element) {
		((JavascriptExecutor) driver).executeScript(script, element);
	}

	/**
	 * 执行JavaScript脚本
	 * @note Really should only be used when the web driver is sucking at exposing
	 * functionality natively
	 * @param script The script to execute
	 */
	public Object trigger(String script) {
		return ((JavascriptExecutor) driver).executeScript(script);
	}

	/**
	 * Opens a new tab for the given URL
	 */
	/*public void openTab(String url) throws JavascriptException {
	    String script = "var d=document,a=d.createElement('a');a.target='_blank';a.href='%s';a.innerHTML='.';d.body.appendChild(a);return a";
	    Object element = trigger(String.format(script, url));
	    if (element instanceof WebElement) {
	        WebElement anchor = (WebElement) element; anchor.click();
	        trigger("var a=arguments[0];a.parentNode.removeChild(a);", anchor);
	    } else {
	        throw new org.openqa.selenium.JavascriptException("Unable to open tab");
	    }
	}*/

	/**
	 * 截图
	 * @param imgPath 图片的CSS路径
	 * @return 图片byte数组
	 * @throws IOException ImageIO异常
	 */
	public byte[] shoot(String imgPath) throws IOException {

		WebElement element = getElementWait(imgPath);

		File screen = driver.getScreenshotAs(OutputType.FILE);

		Point p = element.getLocation();

		int width = element.getSize().getWidth();
		int height = element.getSize().getHeight();

		Rectangle rect = new Rectangle(width, height);

		// 先整体截图
		BufferedImage img = null;
		img = ImageIO.read(screen);

		// 再根据元素相对位置抠图
		BufferedImage dest = img.getSubimage(p.getX(), p.getY(), rect.width, rect.height);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(dest, "png", baos);

		return baos.toByteArray();
	}


	/**
	 * 解析URL
	 * @param url 需要访问的URL地址
	 * @throws InterruptedException
	 * @throws SocketException
	 */
	public void getUrl(String url) throws SocketException {

		driver.get(url);

		// Bypass 验证
		if(driver.getPageSource().contains("安全检查中")){
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		} else {
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
		}

		// TODO 增加规则
		if(driver.getPageSource().matches("Bad Gateway|Gateway Timeout"))
		{
			throw new SocketException("Connection to upstream server failed.");
		}
	}

	/**
	 * 等待页面加载
	 * @param url
	 * @throws Exception
	 */
	public void waitPageLoad(String url) {

		/*
		 * 自定义ExpectedCondition 判断源码中是否出现body
		 */
		io.appium.java_client.functions.ExpectedCondition<String> ec =
			new io.appium.java_client.functions.ExpectedCondition<String>() {

				String regx = "body";

				@Override
				public String apply(WebDriver driver) {

					Pattern p = Pattern.compile(regx);
					Matcher m = p.matcher(driver.getPageSource());

					if(m.find()) {
						logger.info("Found " + regx);
						return m.group();
					}

					return null;
				}

				@Override
				public String toString() {
					return "presence of text by: " + regx;
				}
			};

		// TODO 追踪所报异常，throw proxy timeout exception
		// org.openqa.selenium.TimeOutExpection
		// 需要进行测试
		/*DocumentSettleCondition<WebElement> settleCondition = new DocumentSettleCondition<>(
			ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector("body"))));*/

		DocumentSettleCondition<String> settleCondition = new DocumentSettleCondition<>(ec);

		try {

			new FluentWait<WebDriver>(driver)
					.withTimeout(Duration.ofMillis(PAGE_LOAD_TIMEOUT))
					.pollingEvery(Duration.ofSeconds(1))
					.ignoring(WebDriverException.class) // TODO add TimeoutException.class may lead wait infinitely.
					.until(settleCondition);

		} catch (Exception e) {
			logger.warn("Wait page load error, {}", e.getMessage());
		}

		String readyState = driver.executeScript("return document.readyState").toString();
		logger.info(readyState);
		logger.info("{} page {}", url, readyState.equals("complete") ? "complete" : "loading");
	}

	/**
	 * 合并 iframe 中的源码
	 * TODO 遍历所有iframe
	 */
	private String getAllSrc() {

		String src = driver.getPageSource();

		/*List<WebElement> iframes = driver.findElements(By.tagName("iframe"));

		for(int i=0; i<iframes.size(); i++) {
			driver.switchTo().frame(iframes.get(i));
			src += driver.getPageSource();
			driver.switchTo().defaultContent();
		}*/

		return src;
	}

	/**
	 *
	 * @param task
	 * @throws ChromeDriverException.IllegalStatusException
	 * @throws InterruptedException
	 */
	public void submit(ChromeTask task) throws ChromeDriverException.IllegalStatusException, InterruptedException {

		submit(task, (task.getActions().size() + 1) * (ChromeDistributor.CONNECT_TIMEOUT + ChromeDistributor.READ_TIMEOUT), false);
	}

	/**
	 * 同步执行任务
	 * @param task
	 */
	public void submit(ChromeTask task, boolean ignoreIdleCallback) throws ChromeDriverException.IllegalStatusException, InterruptedException {

		submit(task, (task.getActions().size() + 1) * (ChromeDistributor.CONNECT_TIMEOUT + ChromeDistributor.READ_TIMEOUT), ignoreIdleCallback);
	}

	/**
	 * 同步执行任务 可以设定超时时间
	 * @param task
	 * @param timeout
	 */
	public void submit(ChromeTask task, long timeout, boolean ignoreIdleCallback) throws ChromeDriverException.IllegalStatusException, InterruptedException {

		// 状态时间切分可能存在问题
		if(status != Status.NEW && status != Status.IDLE) {
			throw new ChromeDriverException.IllegalStatusException();
		}

		status = Status.BUSY;

		Future<Void> taskFuture = executor.submit(new Wrapper(task));

		try {

			taskFuture.get(timeout, TimeUnit.MILLISECONDS);
			status = Status.IDLE;

			// 处理 DoneCallbacks
			for(TaskCallback callback : task.doneCallbacks) {
				ChromeDistributor.getInstance().post_executor.submit(()->{
					try {
						callback.run(task);
					} catch (Exception e) {
						logger.error("Error proc doneCallback, Task:{}. ", task.url, e);
					}
				});
			}

			// 处理 NextTaskHolderGenerators
			for(NextTaskGenerator ntg : task.nextTaskGenerators) {

				ChromeDistributor.getInstance().post_executor.submit(()->{

					try {

						List<TaskHolder> nths = new ArrayList<>();
						ntg.run(task, nths);

						for(TaskHolder nth : nths) {
							ChromeDistributor.getInstance().submit(nth);
						}

					} catch (Exception e) {
						logger.error("Error proc doneCallback, Task:{}. ", task.url, e);
					}
				});
			}

			if (task.holder != null) {
				task.holder.done = true;
			}

			logger.info("Task done. {}", task.url);
		}
		// 超时终止
		catch (InterruptedException e) {

			logger.error("Task interrupted. {} ", task.url, e);
			task.exception = e.getCause();
			task.needRetry = true;

		}
		// 运行时异常
		catch (ExecutionException ex) {

			logger.error("Task failed.");

			task.exception = ex.getCause();
			task.needRetry = true;

			try {
				throw ex.getCause();
			}
			// 代理问题
			catch (SocketException e) {
				logger.error("{}, Proxy may error, ", name, e);
				status = Status.FAILED;
			}
			// 脚本异常
			// 异常参考 http://www.softwaretestingstudio.com/common-exceptions-selenium-webdriver/
			catch (ElementNotVisibleException |
					NoAlertPresentException |
					ElementNotSelectableException |
					NoSuchFrameException |
					NoSuchElementException e) {
				logger.error("{}, Task script error, ", name, e);
				status = Status.IDLE;
			}
			// WebDriver 命令超时问题 网络连接问题
			catch (org.openqa.selenium.TimeoutException e) {
				logger.error("{}, WebDriver command timeout, ", name, e);
				status = Status.IDLE;
			}
			catch (NoSuchWindowException e) {
				logger.error("{}, Window unreachable, ", name, e);
				status = Status.FAILED;
			}
			// chromedriver连接问题 --> 关闭
			catch (UnreachableBrowserException e) {
				logger.error("{}, Browser unreachable, ", name, e);
				status = Status.FAILED;
			}
			// 非正常 WebDriver.quit() 调用
			catch (NoSuchSessionException e) {
				logger.error("{}, Session broken, ", name, e);
				status = Status.FAILED;
			}
			// 服务端问题 --> 关闭
			catch (ErrorHandler.UnknownServerException e) {
				logger.error("{}, Server failed, ", name, e);
				status = Status.FAILED;;
			}
			// 无法正常调用WebDriver --> 关闭
			catch (WebDriverException e) {
				logger.error("{}, WebDriver exception, ", name, e);
				status = Status.FAILED;
			}
			// 帐号被冻结
			catch (AccountException.Frozen e) {

				logger.error("{}, Account {}::{} frozen, ", name, e.account.getDomain(), e.account.getUsername(), e);

				removeLoginInfo(e.account.domain, e.account);

				if(accountFrozenCallbacks == null) return;
				for(AccountCallback callback : accountFrozenCallbacks) {
					callback.run(this, e.account);
				}

				status = Status.IDLE;
			}
			// 帐号失效
			catch (AccountException.Failed e) {

				try {

					logger.error("{}, Account {}::{} failed, ", name, e.account.getDomain(), e.account.getUsername(), e);

					// 删除账户登录信息
					removeLoginInfo(e.account.getDomain(), e.account);

					if (accountFailedCallbacks == null) return;
					for (AccountCallback callback : accountFailedCallbacks) {

						// 通常AccountFailedCallback对应的都是重登陆任务
						callback.run(this, e.account);
					}
				} catch ( Exception eException) {
					logger.error("", eException);
				}
				finally {
					status = Status.IDLE;
				}

			}
			// 代理失效
			catch (ProxyException.Failed e) {

				if(e.proxy != null) {

					logger.error("{}, Proxy {}:{} failed, ", name, e.proxy.host, e.proxy.port, e);

					if (proxyFailedCallbacks == null) return;
					for (ProxyCallBack callback : proxyFailedCallbacks) {
						callback.run(this, e.proxy, task);
					}
				}

				status = Status.IDLE;
			}
			// 其他异常 TODO 待验证
			catch (Throwable e) {
				logger.error("{}, Unknown exception, ", name, e);
				status = Status.IDLE;
			}

			if(status == Status.FAILED) {
				// TODO 这样处理会导致 IDLE状态判断两次，执行两次idle callback
				stop();
				return;
			}
		}
		catch (TimeoutException e) {

			task.needRetry = true;
			status = Status.IDLE;
			taskFuture.cancel(true);
			logger.error("{} Task timeout. ", name, e);

		}
		finally {
			task.setDuration();
		}

		runExceptionCallbacks(task);

		// Set idle callback
		// TODO 为什么要check queue.size
		if( status == Status.IDLE ) {
			runCallbacks(idleCallbacks);
		}
	}

	/**
	 *
	 * @param t
	 */
	private void runExceptionCallbacks(ChromeTask t) {
		for(TaskCallback callback : t.exceptionCallbacks) {
			ChromeDistributor.getInstance().post_executor.submit(()->{
				try {
					callback.run(t);
				} catch (Exception e) {
					logger.error("Error proc doneCallback, Task:{}. ", t.url, e);
				}
			});
		}
	}

	/**
	 * 更新加载账户信息
	 */
	private void addLoginInfo(String domain, Account account) {

		accounts.put(domain, account);

		if(accountAddCallback != null)
			accountAddCallback.run(this, account);
	}

	/**
	 *
	 * @param domain
	 * @param account
	 */
	private void removeLoginInfo(String domain, Account account) {

		accounts.remove(domain);

		if(accountRemoveCallback != null) {
			accountRemoveCallback.run(this, account);
		}
	}

	/**
	 * 切换代理
	 * @param proxy
	 */
	public void changeProxy(one.rewind.io.requester.proxy.Proxy proxy) throws InterruptedException, ChromeDriverException.IllegalStatusException {

		Future<Void> changeProxyFuture = executor.submit(new ChangeProxy(proxy));

		try {

			changeProxyFuture.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
			status = Status.IDLE;
		}
		catch (InterruptedException e) {

			status = Status.FAILED;
			logger.error("Change proxy interrupted. ", e);

		}
		catch (ExecutionException e) {

			status = Status.FAILED;
			logger.error("Change proxy failed. ", e.getCause());

		}
		catch (TimeoutException e) {

			changeProxyFuture.cancel(true);
			status = Status.FAILED;
			logger.error("Change proxy failed. ", e);
		}

		if(status == Status.FAILED) {
			try {
				stop();
			} catch (ChromeDriverException.IllegalStatusException e) {
				logger.error(e);
			}
		} else if (status == Status.IDLE) {
			runCallbacks(idleCallbacks);
		}
	}

	/**
	 *
	 * @param callbacks
	 */
	private void runCallbacks(List<ChromeDriverAgentCallback> callbacks) throws InterruptedException, ChromeDriverException.IllegalStatusException {

		if(callbacks == null) return;
		for(ChromeDriverAgentCallback callback : callbacks) {
			callback.run(this);
		}
	}

	/**
	 * 新增加的最先执行
	 * @param callback
	 */
	public ChromeAgent addNewCallback(ChromeDriverAgentCallback callback) throws ChromeDriverException.IllegalStatusException {

		if(status != Status.INIT) throw new ChromeDriverException.IllegalStatusException();

		// TODO
		newCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @param callback
	 */
	public ChromeAgent addIdleCallback(ChromeDriverAgentCallback callback) {

		idleCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @param callback
	 */
	public ChromeAgent addTerminatedCallback(ChromeDriverAgentCallback callback) {

		terminatedCallbacks.add(callback);
		return this;
	}

	public ChromeAgent clearTerminatedCallbacks() {

		this.terminatedCallbacks.clear();
		return this;
	}

	/**
	 *
	 * @param callback
	 * @return
	 */
	public ChromeAgent addAccountFailedCallback(AccountCallback callback) {

		accountFailedCallbacks.add(callback);
		return this;
	}

	public ChromeAgent addAccountFrozenCallback(AccountCallback callback) {

		accountFrozenCallbacks.add(callback);
		return this;
	}

	/**
	 * 增加代理失效异常回调
	 * @param callback
	 * @return
	 */
	public ChromeAgent addProxyFailedCallback(ProxyCallBack callback) {

		proxyFailedCallbacks.add(callback);
		return this;
	}

	/**
	 * 增加代理超时异常回调
	 * @param callback
	 * @return
	 */
	public ChromeAgent addProxyTimeoutCallback(ProxyCallBack callback) {

		proxyTimeoutCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @return
	 */
	public Map<String, Object> getInfo() {

		Map<String, Object> info = new HashMap<>();

		info.put("name", name);

		if(remoteAddress != null) info.put("remoteAddress", remoteAddress);

		if(proxy != null) info.put("proxy", proxy);

		if(bmProxy_port != 0) info.put("localProxy", ChromeDistributor.LOCAL_IP + ":" + bmProxy_port);

		if(remoteShell != null && remoteShell.getVncPort() != 0)
			info.put("vncAddress", remoteShell.getHost() + ":" + remoteShell.getVncPort());
		info.put("status", status);
		info.put("accountInfo", accounts);

		return info;
	}
}