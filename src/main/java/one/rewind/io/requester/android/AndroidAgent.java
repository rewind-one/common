package one.rewind.io.requester.android;

import com.typesafe.config.Config;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
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
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

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

	public void startProxy() {

		CertificateAndKeySource source =
				new PemFileCertificateSource(new File("ca.crt"), new File("pk.crt"), "sdyk");

		// tell the MitmManager to use the root certificate we just generated
		ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
				.rootCertificateSource(source)
				.build();

		bmProxy = new BrowserMobProxyServer();
		bmProxy.setTrustAllServers(true);
		bmProxy.setMitmManager(mitmManager);
		bmProxy.start(0);
		proxyPort = bmProxy.getPort();

		logger.info("Proxy started @port {}", proxyPort);

		RequestFilter filter = (request, contents, messageInfo) -> {

			logger.info(messageInfo.getOriginalUrl());
			logger.info(contents.getTextContents());

			return null;
		};

		bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));

		bmProxy.addResponseFilter((response, contents, messageInfo) -> {

			logger.info(messageInfo.getOriginalUrl());
			logger.info(contents.getTextContents());
		});

	}

	/**
	 * 设置设备Wifi代理
	 * @param mobileSerial
	 */
	public void setupWifiProxy(String mobileSerial) {

		try {

			JadbConnection jadb = new JadbConnection();

			List<JadbDevice> devices = jadb.getDevices();

			for(JadbDevice d : devices) {

				if(d.getSerial().equals(mobileSerial)) {

					execShell(d, "settings", "put", "global", "http_proxy", LOCAL_IP+":"+proxyPort);
					// execShell(d, "settings", "put", "global", "http_proxy", "10.0.0.51:49999");

					// 只需要第一次加载
					/*d.push(new File("ca.crt"),
							new RemoteFile("/sdcard/_certs/ca.crt"));*/

					/*try {
						new PackageManager(d).install(new File("proxy-setter-debug-0.2.1.apk"));
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("bmProxy-setter already installed.");
					}

					d.push(new File("ca-certificate-rsa.crt"),
							new RemoteFile("/sdcard/_certs/ca-certificate-rsa.crt"));

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
		serverCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 60);
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
		capabilities.setCapability("fastReset", "false");
		capabilities.setCapability("fullReset", "false");
		capabilities.setCapability("noReset", "true");

		capabilities.setCapability(ChromeOptions.CAPABILITY, options);

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);
		/*capabilities.setCapability(MobileCapabilityType.APP, app.getAbsolutePath());
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);*/
		driver = new AndroidDriver<>(serviceUrl, capabilities);
	}

	public void run() {

		MobileElement v_view_pager = driver.findElement(By.className("com.zhiliaoapp.musically.customview.VerticalViewPager"));

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
		}

	}

	public static void main(String[] args) throws MalformedURLException {

		AndroidAgent agent = new AndroidAgent();

		//agent.generateCert();
		agent.startProxy();
		agent.setupWifiProxy(UDID);


		/*wrapper.startAppnium(UDID);
		wrapper.startDriver(UDID, appPackage, appActivity, webViewAndroidProcessName);*/
		// wrapper.run();

	}
}

