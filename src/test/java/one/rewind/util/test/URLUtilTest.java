package one.rewind.util.test;

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
}
