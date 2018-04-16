package one.rewind.io.requester.chrome;

import com.typesafe.config.Config;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.Task;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.util.DocumentSettleCondition;
import one.rewind.json.JSON;
import one.rewind.util.Configs;
import one.rewind.util.EnvUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import static one.rewind.io.requester.chrome.ChromeDriverRequester.REQUESTER_LOCAL_IP;

/**
 * Chrome请求器
 * 复用
 * karajan@2017.9.17
 */
public class ChromeDriverAgent {

	public static final Logger logger = LogManager.getLogger(ChromeDriverAgent.class.getName());

	// 获取元素超时时间
	private static int GET_ELEMENT_TIMEOUT = 3;

	// 启动超时时间
	private static int INIT_TIMEOUT = 120000;

	// 关闭超时时间
	private static int CLOSE_TIMEOUT = 120000;

	// 配置设定
	static {

		// A. 读取配置文件
		Config ioConfig = Configs.getConfig(BasicRequester.class);

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
	public static final List<ChromeDriverAgent> instances = new ArrayList<ChromeDriverAgent>();

	// 名称
	public String name;

	// 参数标签
	public Set<Flag> flags;

	public URL remoteAddress;

	// 代理地址
	public one.rewind.io.requester.proxy.Proxy proxy;

	// 存储用于启动 ChromeDriver 的 capabilities
	private DesiredCapabilities capabilities;

	// ChromeDriver 句柄
	private RemoteWebDriver driver;

	// 代理服务器
	public BrowserMobProxyServer bmProxy;

	public int bmProxy_port = 0;

	// 启动后的初始化脚本
	private List<ChromeAction> autoScripts = new ArrayList<>();

	// Executor Queue
	private LinkedBlockingQueue queue = new LinkedBlockingQueue<Runnable>();

	// Executor
	private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);

	volatile Status status = Status.STARTING;

	volatile boolean starting = false;

	volatile boolean stopping = false;

	// Agent状态信息
	public enum Status {
		STARTING, // 启动中
		NEW, // 新创建
		BUSY, // 处理采集任务
		IDLE, // 空闲
		STOPPING, // 终止中
		TERMINATED, // 停止
		FAILED // 失败状态
	}

	private List<Runnable> newCallbacks = new ArrayList<>();

	private List<Runnable> idleCallbacks = new ArrayList<>();

	private List<Runnable> terminatedCallbacks = new ArrayList<>();

	private List<Runnable> accountFailedCallbacks = new ArrayList<>();

	private List<Runnable> accountFrozenCallbacks = new ArrayList<>();

	private List<Runnable> proxyFailedCallbacks = new ArrayList<>();

	private List<Runnable> proxyTimeoutCallbacks = new ArrayList<>();

	/**
	 * 启动标签类
	 */
	public static enum Flag {
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

			synchronized (instances) {

				// 启动 Chrome
				if(remoteAddress != null) {
					driver = new RemoteWebDriver(remoteAddress, capabilities);
				} else {
					driver = new ChromeDriver(capabilities);
				}

				logger.info("Create chromedriver done.");

				instances.add(ChromeDriverAgent.this);
			}

			name = "ChromeDriverAgent-" + instances.size();

			logger.info("New chromedriver name:[{}].", name);

			// 设置脚本运行超时参数
			driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);

			// 设置等待超时参数
			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

			// 这个值要设置的比较大
			// 否则会出现 org.openqa.selenium.TimeoutException: timeout: cannot determine loading status
			// karajan 2018/4/4
			driver.manage().timeouts().pageLoadTimeout(300, TimeUnit.SECONDS);

			boolean execute_success = true;

			if(autoScripts.size() > 0) {

				logger.info("Try to execute auto scripts...");

				for (ChromeAction action : autoScripts) {
					action.setAgent(ChromeDriverAgent.this);
					action.run();
					execute_success = execute_success && action.success;
				}

				logger.info("Auto scripts execute {}.", execute_success ? "succeed" : "failed");
			}

			return execute_success;
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

				synchronized(instances) {
					instances.remove(this);
				}

				logger.info("[{}] stopping.", name);
			}

			return null;
		}
	}

	/**
	 *
	 */
	public class ChangeProxy implements Callable<Void> {

		one.rewind.io.requester.proxy.Proxy proxy = null;

		public ChangeProxy(one.rewind.io.requester.proxy.Proxy proxy) {
			this.proxy = proxy;
		}

		public Void call() throws ProxyException.Failed {

			if(proxy == null || !proxy.validate()) {
				throw new ProxyException.Failed();
			}

			// 关闭 ProxyServer
			if(bmProxy != null && !bmProxy.isStopped()) {
				bmProxy.stop();
			}

			bmProxy = ChromeDriverRequester.buildBMProxy(bmProxy_port, proxy);

			return null;
		}
	}

	/**
	 * 任务封装
	 * @author karajan@tfelab.org
	 * 2017年3月22日 上午10:04:11
	 */
	class Wrapper implements Callable<Void> {

		Task task;

		/**
		 * @param task 采集任务
		 */
		Wrapper(Task task) {
			this.task = task;
			this.task.setStartTime();
			this.task.setException(null);
		}

		/**
		 * 主要任务执行方法
		 */
		public Void call() throws Exception {

			logger.info("{}", task.getUrl());

			getUrl(task.getUrl());
			waitPageLoad(task.getUrl());

			// 正常解析到页面
			if(!driver.getCurrentUrl().matches("^data:.+?")) {

				boolean actionResult = true;

				for(ChromeAction action : task.getActions()) {
					action.setAgent(ChromeDriverAgent.this);
					action.run();
					actionResult = actionResult && action.success;
				}

				task.getResponse().setActionDone(actionResult);
				task.getResponse().setSrc(getAllSrc().getBytes());
				task.getResponse().setText();

				if(task.buildDom()){
					task.getResponse().buildDom();
				}

				if(task.shootScreen()) {
					task.getResponse().setScreenshot(driver.getScreenshotAs(OutputType.BYTES));
				}

				task.validate();
			}

			task.setDuration();
			// 停止页面加载
			driver.executeScript("window.stop()");

			return null;
		}
	}

	/**
	 *
	 * @param flags
	 */
	public ChromeDriverAgent(Flag... flags) {
		this(null, null, flags);
	}

	/**
	 *
	 * @param remoteAddress
	 * @param flags
	 */
	public ChromeDriverAgent(URL remoteAddress, Flag... flags) {
		this(remoteAddress, null, flags);
	}

	/**
	 *
	 * @param proxy
	 * @param flags
	 */
	public ChromeDriverAgent(one.rewind.io.requester.proxy.Proxy proxy, Flag... flags) {
		this(null, proxy, flags);
	}

	/**
	 *
	 * @param proxy 代理
	 * @param flags 启动标签
	 */
	public ChromeDriverAgent(URL remoteAddress, one.rewind.io.requester.proxy.Proxy proxy, Flag... flags) {

		status = Status.STARTING;

		this.remoteAddress = remoteAddress;
		this.proxy = proxy;
		this.flags = new HashSet<Flag>(Arrays.asList(flags));
	}

	/**
	 *
	 */
	public ChromeDriverAgent start() {

		if(starting) return this;

		starting = true;

		Future<Boolean> initFuture = executor.submit(new Init());

		try {

			boolean initSuccess = initFuture.get(INIT_TIMEOUT, TimeUnit.MILLISECONDS);

			status = Status.NEW;

			if(initSuccess) {
				logger.info("INIT done.");
			} else {
				logger.error("INIT done, Auto scripts exec failed.");
			}

			// 执行状态回调函数
			runCallbacks(newCallbacks);

		} catch (InterruptedException e) {

			status = Status.FAILED;
			logger.error("INIT interrupted. ", e);
			stop();

		} catch (ExecutionException e) {

			status = Status.FAILED;
			logger.error("INIT failed. ", e.getCause());
			stop();

		} catch (TimeoutException e) {

			initFuture.cancel(true);

			status = Status.FAILED;
			logger.error("INIT failed. ", e);
			stop();
		}

		return this;

	}

	/**
	 * 停止
	 */
	public void stop() {

		if(stopping) return;

		stopping = true;

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

			status = Status.FAILED;
			logger.error("Stop failed. ", e.getCause());

		}
		catch (TimeoutException e) {

			closeFuture.cancel(true);
			status = Status.FAILED;
			logger.error("Stop failed. ", e);
		}

		runCallbacks(terminatedCallbacks);

		executor.shutdown();
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
		if(proxy != null) {

			Proxy seleniumProxy;

			bmProxy = ChromeDriverRequester.buildBMProxy(proxy);
			bmProxy_port = bmProxy.getPort();

			// 重载 本地代理服务器网络地址
			seleniumProxy = ClientUtil.createSeleniumProxy(bmProxy, InetAddress.getByName(REQUESTER_LOCAL_IP));

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
		options.addArguments("--disk-cache-dir=/dev/null");
		options.addArguments("--disk-cache-size=1");
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

	/**
	 * MITM 监听
	 * 对请求信息进行过滤监听
	 * @param requestFilter 请求过滤器
	 */
	public void addProxyRequestFilter(RequestFilter requestFilter) {

		if(bmProxy != null)
			bmProxy.addRequestFilter(requestFilter);
	}

	public boolean isRemote() {
		return this.remoteAddress != null;
	}

	/**
	 * MITM 监听
	 * 对请求信息进行过滤监听
	 * @param responseFilter 响应过滤器
	 */
	public void addProxyResponseFilter(ResponseFilter responseFilter) {
		if(bmProxy != null)
			bmProxy.addResponseFilter(responseFilter);
	}

	/**
	 * @return ChromeDriver 对象
	 */
	public RemoteWebDriver getDriver() {

		return driver;
	}

	/**
	 * 添加自运行脚本
	 * @param json
	 * @param className
	 * @throws Exception
	 */
	public void addAutoActions(String json, String className) throws Exception {

		Class<ChromeAction> clazz = (Class<ChromeAction>) Class.forName(className);
		this.autoScripts.add(JSON.fromJson(json, clazz));
	}

	/**
	 *
	 * @param dimension
	 */
    public void setSize(Dimension dimension) {

		if(driver != null) {
			// dimension = new Dimension(1024, 600);
			driver.manage().window().setSize(dimension);
		}
	}

	/**
	 *
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

		// TODO 追踪所报异常，throw proxy timeout exception
		// 需要进行测试
		DocumentSettleCondition<WebElement> settleCondition = new DocumentSettleCondition<WebElement>(
			ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body")));

		new FluentWait<WebDriver>(driver)
			.withTimeout(30, TimeUnit.SECONDS)
			.pollingEvery(settleCondition.getSettleTime(), TimeUnit.MILLISECONDS)
			.ignoring(WebDriverException.class)
			.until(settleCondition);

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
	 * 同步执行任务
	 * @param task
	 */
	public synchronized void submit(Task task) throws ChromeDriverException.IllegalStatusException {

		submit(task, ChromeDriverRequester.CONNECT_TIMEOUT + ChromeDriverRequester.READ_TIMEOUT);
	}
	
	/**
	 * 同步执行任务 可以设定超时时间
	 * @param task
	 * @param timeout
	 */
	public synchronized void submit(Task task, long timeout) throws ChromeDriverException.IllegalStatusException {

		// 状态时间切分可能存在问题
		if(status != Status.NEW && status != Status.IDLE) {
			throw new ChromeDriverException.IllegalStatusException();
		}

		status = Status.BUSY;

		Future<Void> taskFuture = executor.submit(new Wrapper(task));

		try {

			taskFuture.get(timeout, TimeUnit.MILLISECONDS);
			status = Status.IDLE;
			logger.info("Task done.");

		}
		// 超时终止
		catch (InterruptedException e) {

			logger.error("Task interrupted. ", e);
			task.setException(e.getCause());
			task.setDuration();
			task.setRetry();

		}
		// 运行时异常
		catch (ExecutionException ex) {

			logger.error("Task failed.");

			task.setException(ex.getCause());
			task.setDuration();

			// 重试机制
			task.setRetry();

			try {
				throw ex.getCause();
			}
			// 代理问题
			catch (SocketException e) {
				logger.error("Proxy may error, ", e);
				status = Status.FAILED;
			}
			// 脚本异常
			// 异常参考 http://www.softwaretestingstudio.com/common-exceptions-selenium-webdriver/
			catch (ElementNotVisibleException |
					NoAlertPresentException |
					ElementNotSelectableException |
					NoSuchFrameException |
					NoSuchWindowException | // 特定的window handle 无法使用
					NoSuchElementException e) {
				logger.error("Task script error, ", e);
				status = Status.IDLE;
			}
			// WebDriver 命令超时问题 网络连接问题
			catch (org.openqa.selenium.TimeoutException e) {
				logger.error("WebDriver command timeout, ", e);
				status = Status.IDLE;
			}
			// chromedriver连接问题 --> 关闭
			catch (UnreachableBrowserException e) {
				logger.error("Browser unreachable, ", e);
				status = Status.FAILED;
			}
			// 非正常 WebDriver.quit() 调用
			catch (NoSuchSessionException e) {
				logger.error("Session broken, ", e);
				status = Status.FAILED;
			}
			// 服务端问题 --> 关闭
			catch (ErrorHandler.UnknownServerException e) {
				logger.error("Server failed, ", e);
				status = Status.FAILED;;
			}
			// 无法正常调用WebDriver --> 关闭
			catch (WebDriverException e) {
				logger.error("WebDriver exception, ", e);
				status = Status.FAILED;
			}
			// 帐号被冻结
			catch (AccountException.Frozen e) {
				logger.error("Account Frozen, ", e);

				runCallbacks(accountFrozenCallbacks);
				return;

			}
			// 帐号失效
			catch (AccountException.Failed e) {
				logger.error("Account failed, ", e);

				runCallbacks(accountFailedCallbacks);
				return;

			}
			// 代理失效
			catch (ProxyException.Failed e) {

				logger.error("Proxy failed, ", e);

				runCallbacks(proxyFailedCallbacks);
				return;

			}
			// 其他异常 TODO 待验证
			catch (Throwable e) {
				logger.error("Unknown exception, ", e);
				status = Status.IDLE;
			}

			if(status == Status.FAILED) {
				stop();
			}
		}
		catch (TimeoutException e) {

			status = Status.IDLE;
			taskFuture.cancel(true);
			logger.error("Task timeout. ", e);

		}

		// Set idle callback
		// TODO 为什么要Check queue.size
		if(status == Status.IDLE && queue.size() == 0) {

			runCallbacks(idleCallbacks);
		}
	}

	/**
	 *
	 * @param proxy
	 */
	public void changeProxy(one.rewind.io.requester.proxy.Proxy proxy) {
		executor.submit(new ChangeProxy(proxy));
	}

	/**
	 *
	 * @param callbacks
	 */
	private void runCallbacks(List<Runnable> callbacks) {

		if(callbacks == null) return;
		for(Runnable callback : callbacks) {
			executor.submit(callback);
		}
	}

	/**
	 *
	 * @param callback
	 */
	public ChromeDriverAgent addNewCallback(Runnable callback) throws ChromeDriverException.IllegalStatusException {

		if(status != Status.STARTING) throw new ChromeDriverException.IllegalStatusException();

		newCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @param callback
	 */
	public ChromeDriverAgent addIdleCallback(Runnable callback) {

		idleCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @param callback
	 */
	public ChromeDriverAgent addTerminatedCallback(Runnable callback) {

		terminatedCallbacks.add(callback);
		return this;
	}

	public ChromeDriverAgent clearTerminatedCallbacks() {

		this.terminatedCallbacks.clear();
		return this;
	}

	/**
	 *
	 * @param callback
	 * @return
	 */
	public ChromeDriverAgent addAccountFailedCallback(Runnable callback) {

		accountFailedCallbacks.add(callback);
		return this;
	}

	public ChromeDriverAgent addAccountFrozenCallback(Runnable callback) {

		accountFrozenCallbacks.add(callback);
		return this;
	}

	/**
	 * 只需要设置agent的状态，不需要调用agent.stop()
	 * @param callback
	 * @return
	 */
	public ChromeDriverAgent addProxyFailedCallback(Runnable callback) {

		proxyFailedCallbacks.add(callback);
		return this;
	}

	public ChromeDriverAgent addProxyTimeoutCallback(Runnable callback) {

		proxyTimeoutCallbacks.add(callback);
		return this;
	}
}