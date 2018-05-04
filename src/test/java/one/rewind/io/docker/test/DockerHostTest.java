package one.rewind.io.docker.test;

import one.rewind.io.docker.model.ChromeDriverDockerContainer;
import one.rewind.io.docker.model.DockerHost;
import org.junit.Test;

public class DockerHostTest {

	/**
	 *
	 */
	@Test
	public void testCreateContainer() throws Exception {

		// 1. 创建 Host
		DockerHost host = new DockerHost("10.0.0.62", 22, "root");

		// 2. 创建 Container
		ChromeDriverDockerContainer container = new ChromeDriverDockerContainer(host, "ChromeContainer-10.0.0.62-1", 31001, 32001);

		// 3. 测试执行命令
		String output = container.exec("xdotool mousemove 200 200; xdotool click 3; sleep 3; xdotool mousemove 400 400; xdotool click 3;");

		System.err.println(output);

		// 4.
	}
}
