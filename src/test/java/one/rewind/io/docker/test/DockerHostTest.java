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

		DockerHost host = new DockerHost("10.0.0.62", 22, "root");

		host.delAllDockerContainers();

		ChromeDriverDockerContainer container = host.createChromeDriverDockerContainer();

		System.err.println(container.vncPort);

		// 4.
	}
}
