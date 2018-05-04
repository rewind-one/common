package one.rewind.io.test;

import org.junit.Test;
import one.rewind.io.ssh.SshManager;

/**
 * 测试在远程主机执行命令/上传文件
 */
public class SshManagerTest {

	@Test
	public void test() throws Exception {

		String[] hosts = {
				"118.190.83.89",
				"118.190.44.184",
				"118.190.133.34",
				"114.215.70.14",
				"114.215.45.48"
		};

		for(String hs : hosts) {

			SshManager.Host host = new SshManager.Host(hs, 22, "root", "********");
			host.connect();

			String output = host.exec("jps | grep Crawler | awk '{print $1}' | xargs kill -9");
			System.err.println(output);

			host.upload("build/libs/musical-sharing-1.0-SNAPSHOT.jar", "/opt/muse/musical-sharing-1.0-SNAPSHOT/lib");

			output = host.exec("cd /opt/muse/musical-sharing-1.0-SNAPSHOT && nohup bin/musical-sharing &");
			System.err.println(output);
		}

	}
}
