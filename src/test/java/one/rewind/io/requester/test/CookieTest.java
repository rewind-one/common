package one.rewind.io.requester.test;

import one.rewind.io.requester.basic.Cookies;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/12
 */
public class CookieTest {

	@Test
	public void testParseCookieItem() throws Cookies.IllegalCookieFormat {

		String src = "k1=v1; domain=reid.red; path=/blog";

		Cookies.Item ci = new Cookies.Item(src);

		System.err.println(ci.toJSON());
	}

	@Test
	public void testMultiSet1() throws Cookies.IllegalCookieOperation, URISyntaxException, Cookies.IllegalCookieFormat {

		String url = "http://www.reid.red/asdf/dsdf?q=SSS";

		List<String> srcList = Arrays.asList("k1=v1; domain=reid.red; path=/blog", "k2=v2; domain=reid.red; path=/");

		Cookies.Holder holder = new Cookies.Holder(url, srcList);

		System.err.println(holder.toJSON());

	}

	@Test
	public void testMultiSet2() throws Cookies.IllegalCookieOperation, URISyntaxException, Cookies.IllegalCookieFormat {

		String url = "http://www.reid.red/asdf/dsdf?q=SSS";

		List<String> srcList = Arrays.asList("k1=v1; domain=reid.red; path=/blog", "k1=v1; domain=reid.red; path=/");

		Cookies.Holder holder = new Cookies.Holder(url, srcList);

		System.err.println(holder.getCookies(url));

	}

	@Test
	public void testMultiSet3() throws Cookies.IllegalCookieOperation, URISyntaxException, Cookies.IllegalCookieFormat {

		String url = "http://www.reid.red/blog/dsdf?q=SSS";

		List<String> srcList = Arrays.asList("k1=v1; domain=reid.red; path=/blog", "k3=v3; domain=reid.red; path=/ad");

		Cookies.Holder holder = new Cookies.Holder(url, srcList);

		System.err.println(holder.getCookies(url));

	}

	@Test
	public void testMultiSet4() throws Cookies.IllegalCookieOperation, URISyntaxException, Cookies.IllegalCookieFormat {

		String url = "http://www.reid.red/blog/dsdf?q=SSS";

		List<String> srcList = Arrays.asList("k1=v1; domain=reid.red; path=/blog", "k3=v3; domain=blog.reid.red; path=/blog");

		Cookies.Holder holder = new Cookies.Holder(url, srcList);

		System.err.println(holder.getCookies(url));

	}

	@Test
	public void testMultiSet5() throws Cookies.IllegalCookieOperation, URISyntaxException, Cookies.IllegalCookieFormat {

		String url = "https://mp.weixin.qq.com/s?__biz=MjM5NDM1Mzc4MQ==&mid=2651794199&idx=1&sn=35240dbc3ad45c70deec48d21780dde5&chksm=bd728c0d8a05051b9fc7c02a580e1e1ca41f5d9dac3828ebd9c838da6a530d4d18e6e4e7d50b&scene=4&subscene=126&ascene=0&devicetype=android-25&version=2607033d&nettype=WIFI&abtest_cookie=BQABAAoACwASABMAFAAFACOXHgBamR4Am5keAJ2ZHgDRmR4AAAA%3D&lang=zh_CN&pass_ticket=jCjaRMi4QjnTLCurp8Oy%2BFa8%2FWw7Pd6VHCM%2BcSgeQh%2B8VIJg%2Bv4zhV9xML7%2BXqRH&wx_header=1";

		List<String> srcList = Arrays.asList("malluin=MjE5Mjk4MjQ5MA==; Path=/bizmall/; HttpOnly",
				"mallkey=7963b6e7387dcfc1282834d16922797f9e0a68e6e2d1e1a6d44c8360eb91315aa6920460cb1f7848642852eddf76bb742ad3e4b4202e8b4977b4dfb5e87e398feab38e247728aa8a4309856692454e28; Path=/bizmall/; HttpOnly",
				"malluin=EXPIRED; Path=/; Expires=Sun, 10-Feb-2019 15:46:39 GMT; HttpOnly",
				"mallkey=EXPIRED; Path=/; Expires=Sun, 10-Feb-2019 15:46:39 GMT; HttpOnly",
				"rewardsn=; Path=/",
				"payforreadsn=EXPIRED; Path=/; Expires=Sun, 10-Feb-2019 15:46:39 GMT; HttpOnly",
				"wxtokenkey=777; Path=/; HttpOnly",
				"wxuin=2192982490; Path=/; HttpOnly",
				"devicetype=android-25; Path=/",
				"version=2607033d; Path=/",
				"lang=zh_CN; Path=/",
				"pass_ticket=jCjaRMi4QjnTLCurp8Oy+Fa8/Ww7Pd6VHCM+cSgeQh+8VIJg+v4zhV9xML7+XqRH; Path=/; HttpOnly",
				"wap_sid2=CNqD2ZUIElw0am1nZzAyaENtRGEydmpOTUlidjd0ZmNJS29VR0RVVldDaWhvX1dFMmVzVWxYcjExSmk3R2dfdmhsMk5BWEx0WjhLZGhWSW9DU0hSTkxGbGVqMF8yZU1EQUFBfjDfsYbjBTgNQAE=; Path=/; HttpOnly");

		Cookies.Holder holder = new Cookies.Holder(url, srcList);

		System.err.println(holder.getCookies(url));

	}
}
