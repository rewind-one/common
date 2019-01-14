package one.rewind.util.test;

import one.rewind.util.NetworkUtil;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtilTest {

	@Test
	public void testGetIP() throws UnknownHostException {
		System.err.println(NetworkUtil.getAllInetIpString());
	}
}
