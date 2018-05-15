package one.rewind.io.docker.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.typesafe.config.Config;
import one.rewind.util.Configs;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 容器
 */
public class ChromeDriverDockerContainer extends DockerContainer {

	public static int SELENIUM_BEGIN_PORT = 31000;
	public static int VNC_BEGIN_PORT = 32000;
	public static String DEFAULT_IMAGE_NAME = "selenium/standalone-chrome-debug";

	static {

		Config config = Configs.getConfig(DockerHost.class).getConfig("chromeDriverContainer");
		SELENIUM_BEGIN_PORT = config.getInt("seleniumBeginPort");
		VNC_BEGIN_PORT = config.getInt("vncBeginPort");
		DEFAULT_IMAGE_NAME = config.getString("defaultImageName");
	}

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int seleniumPort;

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int vncPort;

	/**
	 *
	 */
	public ChromeDriverDockerContainer() {}

	/**
	 *
	 * @param host
	 * @param name
	 * @param seleniumPort
	 * @param vncPort
	 */
	public ChromeDriverDockerContainer(DockerHost host, String name, int seleniumPort, int vncPort) {

		this.host = host;
		this.ip = host.ip;
		this.name = name;
		this.imageName = DEFAULT_IMAGE_NAME;
		this.seleniumPort = seleniumPort;
		this.vncPort = vncPort;
	}

	/**
	 *
	 * @param host
	 * @param name
	 * @param imageName
	 * @param seleniumPort
	 * @param vncPort
	 */
	public ChromeDriverDockerContainer(DockerHost host, String name, String imageName, int seleniumPort, int vncPort) {

		this.host = host;
		this.ip = host.ip;
		this.name = name;
		this.imageName = imageName; // selenium/standalone-chrome-debug
		this.seleniumPort = seleniumPort;
		this.vncPort = vncPort;
	}

	/**
	 *
	 * @throws Exception
	 */
	public void create() throws Exception {

		String cmd = "docker run -d --name " + name
				+ " -p " + seleniumPort + ":4444 -p " + vncPort + ":5900" +
				" -e SCREEN_WIDTH=\"1360\" -e SCREEN_HEIGHT=\"768\" -e SCREEN_DEPTH=\"24\"" +
				" " + imageName;

		if (host != null) {

			String output = host.exec(cmd);
			// TODO 根据output 判断是否执行成功
			DockerHost.logger.info(output);

			status = Status.IDLE;
		} else {
			throw new Exception("DockerHost is null");
		}
	}

	public void setIdle() throws Exception {
		this.status = Status.IDLE;
		this.update();
	}

	public void setOccupied() throws Exception {
		this.status = Status.OCCUPIED;
		this.update();
	}

	/**
	 * 获取路由的地址
	 *
	 * @throws MalformedURLException
	 */
	public URL getRemoteAddress() throws MalformedURLException {
		return new URL("http://" + ip + ":" + seleniumPort + "/wd/hub");
	}
}

