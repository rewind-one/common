package one.rewind.util.test;

import one.rewind.txt.URLUtil;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class URLUtilTest {

	/**
	 * 通过URL获取domain
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	@Test
	public void getDomain() throws MalformedURLException, URISyntaxException {

		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://shop.jfh.com/1182/bu"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://jfh.com/1182/bu"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://shop.jfh.com/1182/bu"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://www.jfh.com/1182/bu"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://www.jfh.com/1182/bu"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://jfh.com"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://dfsdf.dfec.dfght.fgr"));
		System.err.println(one.rewind.txt.URLUtil.getDomainName("http://jfh.jfh.jfh.jfh.com"));
	}


	@Test
	public void testGetParam() {

		String q = "sd=32324&zz=sdf";
		System.err.println(URLUtil.getParam(q, "sd"));

		q = "https://mp.weixin.qq.com/s?__biz=MjM5NDM1Mzc4MQ==&mid=2651794228&idx=1&sn=3ffbc0c158bf0380e4c7a48002a02020&chksm=bd728c2e8a050538393b2cbfa4cd530c4c2fd9471e31f07fefb7808a252bf6bb27f207f6c4da&scene=4&subscene=126&ascene=0&devicetype=android-25&version=2607033d&nettype=WIFI&abtest_cookie=BQABAAoACwASABMAFAAFACOXHgBamR4Am5keAJ2ZHgDRmR4AAAA%3D&lang=zh_CN&pass_ticket=SqivLFxBvIbBoK7hL3RWJjbMxrnIZG%2By4u6XPN%2BSTNQ0WJxXI64s98DA8SOBs6cM&wx_header=1";

		System.err.println(URLUtil.getParam(q, "abtest_cookie"));
	}
}
