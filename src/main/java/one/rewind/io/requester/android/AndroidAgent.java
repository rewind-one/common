package one.rewind.io.requester.android;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.RootCertificateGenerator;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import one.rewind.io.requester.BasicRequester;
import one.rewind.util.Configs;
import one.rewind.util.FileUtil;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.managers.PackageManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidAgent {

	private static final Logger logger = LogManager.getLogger(AndroidAgent.class.getName());

	public volatile boolean running = true;

	static File nodeJsExecutable = new File("C:\\Program Files\\nodejs\\node.exe");
	static File appniumMainJsFile = new File("C:\\Users\\karajan\\AppData\\Local\\Programs\\appium-desktop\\resources\\app");

	public static String LOCAL_IP;

	// 配置设定
	static {

		Config ioConfig = Configs.getConfig(BasicRequester.class);
		LOCAL_IP = ioConfig.getString("requesterLocalIp");
		LOCAL_IP = NetworkUtil.getLocalIp();
	}

	BrowserMobProxy bmProxy;
	int proxyPort;

	AppiumDriverLocalService service;
	URL serviceUrl;
	AndroidDriver<MobileElement> driver;

	static String webViewAndroidProcessName = "com.tencent.mm:appbrand0";
	static String appPackage = "com.tencent.mm";
	static String appActivity = ".ui.LauncherUI";

	static String UDID = "ZX1G323GNB"; //37e43754

	public AndroidAgent() { }

	public static void generateCert() {
		// create a dynamic CA root certificate generator using default settings (2048-bit RSA keys)
		RootCertificateGenerator rootCertificateGenerator = RootCertificateGenerator.builder().build();

		// save the dynamically-generated CA root certificate for installation in a browser
		rootCertificateGenerator.saveRootCertificateAsPemFile(new File("ca.crt"));
		rootCertificateGenerator.savePrivateKeyAsPemFile(new File("pk.crt"), "sdyk");
	}

	public void startProxy(int port) {

		CertificateAndKeySource source =
				new PemFileCertificateSource(new File("ca.crt"), new File("pk.crt"), "sdyk");

		// tell the MitmManager to use the root certificate we just generated
		ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
				.rootCertificateSource(source)
				.build();

		bmProxy = new BrowserMobProxyServer();
		bmProxy.setTrustAllServers(true);
		bmProxy.setMitmManager(mitmManager);
		bmProxy.start(port);
		proxyPort = bmProxy.getPort();

		logger.info("Proxy started @port {}", proxyPort);

		RequestFilter filter = (request, contents, messageInfo) -> {

			//logger.info(messageInfo.getOriginalUrl());
			//logger.info(contents.getTextContents());

			return null;
		};

		bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));

		bmProxy.addResponseFilter((response, contents, messageInfo) -> {

			//logger.info(messageInfo.getOriginalUrl());
			//logger.info(contents.getTextContents());
		});

	}

	public void stopProxy() {
		bmProxy.stop();
	}

	/**
	 * 设置设备Wifi代理
	 * @param mobileSerial
	 */
	public void setupWifiProxy(String mobileSerial) {

		try {

			JadbConnection jadb = new JadbConnection();

			// TODO
			// 需要调用process 启动adb daemon

			List<JadbDevice> devices = jadb.getDevices();

			for(JadbDevice d : devices) {

				if(d.getSerial().equals(mobileSerial)) {

					execShell(d, "settings", "put", "global", "http_proxy", LOCAL_IP+":"+proxyPort);
					// execShell(d, "settings", "put", "global", "http_proxy", "10.0.0.51:49999");

					// 只需要第一次加载
					/*d.push(new File("ca.crt"),
							new RemoteFile("/sdcard/_certs/ca.crt"));*/

					/*String ssid = "SDYK-AI";
					String password = "sdyk-ai@2018";

					try {
						new PackageManager(d).install(new File("proxy-setter-debug-0.2.1.apk"));
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("bmProxy-setter already installed.");
					}

					execShell(d,"am", "start",
							"-n", "tk.elevenk.proxysetter/.MainActivity",
							"-e", "ssid", ssid,
							"-e", "clear", "true");

					Thread.sleep(2000);

					if(password == null) {
						execShell(d,"am", "start",
								"-n", "tk.elevenk.proxysetter/.MainActivity",
								"-e", "host", LOCAL_IP,
								"-e", "port", String.valueOf(proxyPort),
								"-e", "ssid", ssid,
								"-e", "reset-wifi", "true");
					} else {
						execShell(d,"am", "start",
								"-n", "tk.elevenk.proxysetter/.MainActivity",
								"-e", "host", LOCAL_IP,
								"-e", "port", String.valueOf(proxyPort),
								"-e", "ssid", ssid,
								"-e", "bypass", password,
								"-e", "reset-wifi", "true");
					}*/

					Thread.sleep(2000);
				}
			}


		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 设置设备Wifi代理
	 * @param mobileSerial
	 */
	public void installApk(String mobileSerial, String fileName) {

		try {

			JadbConnection jadb = new JadbConnection();

			List<JadbDevice> devices = jadb.getDevices();

			for(JadbDevice d : devices) {

				if(d.getSerial().equals(mobileSerial)) {

					new PackageManager(d).install(new File(fileName));
					Thread.sleep(2000);
				}
			}

		} catch (Exception e){
			e.printStackTrace();
		}
	}


	/**
	 *
	 * @param mobileSerial
	 */
	public void removeWifiProxy(String mobileSerial) {

		try {

			JadbConnection jadb = new JadbConnection();

			List<JadbDevice> devices = jadb.getDevices();

			for(JadbDevice d : devices) {

				if(d.getSerial().equals(mobileSerial)) {

					execShell(d, "settings", "delete", "global", "http_proxy");
					execShell(d, "settings", "delete", "global", "https_proxy");
					execShell(d, "settings", "delete", "global", "global_http_proxy_host");
					execShell(d, "settings", "delete", "global", "global_http_proxy_port");

					Thread.sleep(2000);
				}
			}


		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param d
	 * @param command
	 * @param args
	 * @throws IOException
	 * @throws JadbException
	 */
	public static void execShell(JadbDevice d, String command, String... args) throws IOException, JadbException {

		InputStream is = d.executeShell(command, args);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n"); //appende a new line
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info(builder.toString());
	}

	/**
	 *
	 */
	public void startAppnium(String udid) {

		DesiredCapabilities serverCapabilities = new DesiredCapabilities();
		serverCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		serverCapabilities.setCapability(MobileCapabilityType.UDID, udid);
		serverCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);
//		serverCapabilities.setCapability(AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE,
//				"C:\\App\\chromedriver\\2.28\\chromedriver.exe");

		service = new AppiumServiceBuilder()
				/*.usingDriverExecutable(nodeJsExecutable)
				.withAppiumJS(appniumMainJsFile)*/
				.withCapabilities(serverCapabilities)
				/*.withIPAddress(ip)*/
				.usingAnyFreePort()
				/*.usingPort(port)
				.withArgument(AndroidServerFlag.CHROME_DRIVER_PORT, "")
				.withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, "")
				.withArgument(AndroidServerFlag.SELENDROID_PORT, "")
				.withLogFile(new File("appium.log"))*/
				.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				.build();

		service.start();

		serviceUrl = service.getUrl();
	}

	/**
	 *
	 * @return
	 */
	public void startDriver(String udid, String appPackage, String appActivity, String webViewAndroidProcessName) throws MalformedURLException {

		ChromeOptions options = new ChromeOptions();
		if (webViewAndroidProcessName != null) {
			options.setExperimentalOption("androidProcess", webViewAndroidProcessName);
		}

		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("app", "");
		capabilities.setCapability("appPackage", appPackage);
		capabilities.setCapability("appActivity", appActivity);
		capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

		capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));

		//capabilities.setCapability(ChromeOptions.CAPABILITY, options);

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);
		/*capabilities.setCapability(MobileCapabilityType.APP, app.getAbsolutePath());
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);*/
		driver = new AndroidDriver<>(serviceUrl, capabilities);
	}

	/**
	 *
	 * @param groupName
	 * @param ids
	 * @throws InterruptedException
	 */
	public void createGroupChat(String groupName, String... ids) throws InterruptedException {

		Thread.sleep(5000);

		// 点 +
		new TouchAction(driver).tap(PointOption.point(1202 + 100, 84 + 80)).perform();

		Thread.sleep(1000);

		// 创建群聊
		new TouchAction(driver).tap(PointOption.point(1050, 335)).perform();

		MobileElement id_input;

		for(String id : ids) {

			// 人名输入框
			id_input = driver.findElement(By.className("android.widget.EditText"));

			// 输入人名
			id_input.setValue(id);

			Thread.sleep(1000);

			// 选择人
			new TouchAction(driver).tap(PointOption.point(1270, 641)).perform();

			Thread.sleep(1000);
		}

		// 创建群
		new TouchAction(driver).tap(PointOption.point(1320, 168)).perform();

		Thread.sleep(10000);

		// 点群设置
		new TouchAction(driver).tap(PointOption.point(1302, 168)).perform();

		Thread.sleep(1000);

		// 点群名称
		new TouchAction(driver).tap(PointOption.point(720, 784)).perform();

		Thread.sleep(1000);

		// 名称输入框
		MobileElement groupNameInput = driver.findElement(By.className("android.widget.EditText"));

		groupNameInput.setValue(groupName);

		Thread.sleep(1000);

		// 确定名称 点OK
		new TouchAction(driver).tap(PointOption.point(1331, 168)).perform();

		Thread.sleep(1000);

		// 返回群聊
		new TouchAction(driver).tap(PointOption.point(70, 168)).perform();

		Thread.sleep(1000);

		// 返回主界面
		new TouchAction(driver).tap(PointOption.point(70, 168)).perform();

		Thread.sleep(1000);
	}

	public void goIntoGroup(String groupName) throws InterruptedException {

		new TouchAction(driver).tap(PointOption.point(1118, 168)).perform();

		Thread.sleep(1000);

		// 名称输入框
		MobileElement groupNameInput = driver.findElement(By.className("android.widget.EditText"));

		groupNameInput.setValue(groupName);

		Thread.sleep(1000);

		// 确定名称 点OK
		new TouchAction(driver).tap(PointOption.point(1064, 532)).perform();

		Thread.sleep(1000);

	}

	/**
	 *
	 * @throws InterruptedException
	 */
	public void getChatInfo() throws InterruptedException {

		String src = driver.getPageSource();

		Document doc = Jsoup.parse(src);

		int h = 0;
		// 找到消息列表框架
		Elements list_views = doc.getElementsByAttributeValue("class", "android.widget.ListView");

		for(Element el : list_views) {

			if(el.attr("bounds").matches("\\[0,.+?\\[1440,.+?")) {

				Pattern pattern = Pattern.compile("(?<h1>\\d+)\\]\\[\\d+,(?<h2>\\d+)\\]");
				Matcher m = pattern.matcher(el.attr("bounds"));

				if (m.find()) {
					h = Integer.valueOf(m.group("h2")) - Integer.valueOf(m.group("h1"));
				}
			}
		}



		Elements els = doc.getElementsByAttributeValue("class", "android.widget.RelativeLayout");

		for(Element el : els) {

			String bounds = el.attr("bounds");

			// 找到一行消息
			if(bounds.matches("\\[0,.+?\\[1440,.+?")) {

				System.err.println(bounds);

				Elements imageEls = el.getElementsByAttributeValue("class", "android.widget.ImageView");
				Elements viewEls = el.getElementsByAttributeValue("class", "android.view.View");

				// 图片数量
				// 0 文本消息 头像未展示 应该第一个消息 或最后一个消息 由于特殊的显示位置不能有效显示
				// 2 文本消息 头像在界面上有展示 不一定展示全 需要拿 第一个ImageView的bounds 解析宽度高度 判定是否完整显示
				// 4 图片类型消息

				// 如果是最后一个消息，图片数量为2或4 需要判断内容是否展示全
				//

				System.err.println("images " + imageEls.size() + " views " + viewEls.size());
			}

		}

		System.err.println(h);

		int start_x = 1;
		int start_y = 2560 / 2 - h / 2;

		int end_x = 1;
		int end_y = 2560 / 2 + h / 2 + 56;

		for(int i=0; i<5; i++) {

			FileUtil.writeBytesToFile(driver.getScreenshotAs(OutputType.BYTES), "tmp/" + i + ".png");

			TouchAction swipe = new TouchAction(driver)
					.press(PointOption.point(start_x, start_y))
					//.waitAction(WaitOptions.waitOptions(Duration.ofSeconds(10)))
					.moveTo(PointOption.point(end_x, end_y))
					.waitAction(WaitOptions.waitOptions(Duration.ofSeconds(1)))
					.release();

			swipe.perform();

			Thread.sleep(1000);
		}

		//byte[] img = me.getScreenshotAs(OutputType.BYTES);
		//FileUtil.writeBytesToFile(img, StringUtil.uuid() + ".png");
	}

	/**
	 *
	 * @throws InterruptedException
	 */
	public void run() throws InterruptedException {

		Thread.sleep(5000);

		goIntoGroup("Askwitionary");

		Thread.sleep(1000);

		getChatInfo();

		/*List<MobileElement> es = driver.findElements(By.className("android.widget.RelativeLayout"));
		// android.widget.RelativeLayout
		// android.widget.TextView
		// android.widget.EditText

		System.err.println(es.size());

		for(MobileElement me : es) {
			if(me.getLocation().x == 1202 && me.getLocation().y == 84) {
				me.click();
			}
		}*/

		/*MobileElement v_view_pager = driver.findElement(By.className("com.zhiliaoapp.musically.customview.VerticalViewPager"));

		TouchAction swipe = new TouchAction(driver).press(v_view_pager, 384, 640 - 200)
				.waitAction(Duration.ofSeconds(1)).moveTo(v_view_pager, 384, 640 + 200).release();
		swipe.perform();

		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("abl")
		));
		Exception e = null;

		while(running && e == null) {

			try {
				new TouchAction(driver)
						.tap(driver.findElement(By.id("abl"))).perform();

				wait.until(ExpectedConditions.presenceOfElementLocated(By
						.id("abw")
				));

				new TouchAction(driver)
						.tap(driver.findElement(By.id("e0"))).perform();

				wait.until(ExpectedConditions.presenceOfElementLocated(By
						.id("abl")
				));

				MobileElement v_view_pager_ = driver.findElement(By.className("com.zhiliaoapp.musically.customview.VerticalViewPager"));
				TouchAction swipe_ = new TouchAction(driver).press(v_view_pager_, 384, 640 + 200)
						.waitAction(Duration.ofSeconds(1)).moveTo(v_view_pager_, 384, 640 - 200).release();
				swipe_.perform();

				Thread.sleep(10000);
			} catch (Exception ex) {
				e = ex;
				ex.printStackTrace();
			}
		}*/

	}

	/**
	 *
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) throws Exception {

		AndroidAgent agent = new AndroidAgent();

		//agent.generateCert();
		agent.startProxy(0);
		// agent.installApk(UDID, "wechat-6-5-23.apk");
		// agent.removeWifiProxy(UDID);
		agent.setupWifiProxy(UDID);
		//agent.stopProxy();

		agent.startAppnium(UDID);
		agent.startDriver(UDID, appPackage, appActivity, webViewAndroidProcessName);
		agent.run();
	}
}

