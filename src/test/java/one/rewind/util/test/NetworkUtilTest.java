package one.rewind.util.test;

import one.rewind.util.NetworkUtil;
import org.junit.Test;
import one.rewind.util.NetworkUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtilTest {

	@Test
	public void testGetIP() throws UnknownHostException {
		System.err.println(NetworkUtil.getLocalIp());
		System.err.println(InetAddress.getLocalHost());
	}
}
