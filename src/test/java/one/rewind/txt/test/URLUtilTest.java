package one.rewind.txt.test;

import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.txt.URLUtil;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class URLUtilTest {

	@Test
	public void test() throws MalformedURLException, URISyntaxException {
		String url = "http://aitest.315free.com:50125/rcmd/user/d5f4111e53d34d6f8a12d60d49391af9";
		Task t = new Task(url);
		t.setDelete();
		BasicRequester.getInstance().submit(t);
		System.err.println(t.getResponse().getText());
	}

	@Test
	public void getRootDomainNameTest() {
		URLUtil.getRootDomainName("abc.sss.taobao.com.cn");
	}

	@Test
	public void getProtocolTest() throws MalformedURLException, URISyntaxException {
		URLUtil.getProtocol("https://pic.36krcnd.com/avatar/201609/08060207/6b5gr77ktodsf70j.jpg!heading");
	}
}
