package one.rewind.txt.test;

import one.rewind.io.requester.task.Task;
import one.rewind.txt.URLUtil;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class URLUtilTest {

	@Test
	public void getRootDomainNameTest() {
		URLUtil.getRootDomainName("abc.sss.taobao.com.cn");
	}

	@Test
	public void getProtocolTest() throws MalformedURLException, URISyntaxException {
		URLUtil.getProtocol("https://pic.36krcnd.com/avatar/201609/08060207/6b5gr77ktodsf70j.jpg!heading");
	}
}
