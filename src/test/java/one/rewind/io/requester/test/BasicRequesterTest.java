package one.rewind.io.requester.test;

import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * BasicRequester 的测试方法
 * Created by karajan on 2017/6/3.
 */
public class BasicRequesterTest {

	@Before
	public void setup() {
		System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
		System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
	}

	/**
	 * 简单请求测试，多参数
	 * @throws Exception
	 */
	@Test
	public void basicFetch() throws Exception {

		String url = "https://www.baidu.com";
		String cookies = "JSESSIONID=BE83E060C0FECB71715418659ECFFD63; cninfo_search_record_cookie=%E6%8B%9B%E5%95%86%E9%93%B6%E8%A1%8C";
		String ref = "http://www.cninfo.com.cn/cninfo-new/disclosure/szse/showFulltext/600036";

		Task<Task> t = new Task(url, null, cookies, ref);
		Proxy proxy = new ProxyImpl("uml.ink", 60201, "tfelab", "TfeLAB2@15");
		t.setProxy(proxy);

		BasicRequester.getInstance().submit(t, 30000);
		if(t.exception != null) {
			t.exception.printStackTrace();
		}

		System.err.println(t.getDuration() + "\t" + t.getResponse().getText());
	}

	/**
	 * 设置生成DOM
	 * @throws Exception
	 */
	@Test
	public void domBuildTest() throws Exception {

		String url = "https://www.baidu.com";

		Task<Task> t = new Task(url);
		t.setBuildDom();

		BasicRequester.getInstance().submit(t, 30000);

		if(t.exception != null) {
			t.exception.printStackTrace();
		}

		System.err.println(t.getDuration() + "\t" + t.getResponse().getDoc().select("#su").attr("value"));

	}

	/**
	 * 切实切换代理
	 * @throws Exception
	 */
	@Test
	public void testSwitchProxy() throws Exception {

		for(int i=0; i<1; i++) {

			Thread thread = new Thread(() -> {

				for(int j=0; j<10; j++) {

					Task<Task> t;

					try {

						if(j % 2 == 0){
							t = new Task("http://ddns.oray.com/checkip");
							Proxy proxy = new ProxyImpl("uml.ink", 60201, "tfelab", "TfeLAB2@15");
							t.setProxy(proxy);
						} else {
							t = new Task("http://ddns.oray.com/checkip");
							Proxy proxy = new ProxyImpl("uml.ink", 60204, "tfelab", "TfeLAB2@15");
							t.setProxy(proxy);
						}

						t.setPreProc();

						BasicRequester.getInstance().submit(t, 30000);

						if(t.exception != null) {
							t.exception.printStackTrace();
						}

						System.err.println(t.getDuration() + "\t" + t.getResponse().getText());

					} catch (MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}

				}
			});

			thread.start();
		}

		Thread.sleep(60000);
	}

	/**
	 * 测试自定义请求头
	 * @throws Exception
	 */
	@Test
	public void customizedHeader() throws Exception {

		String url = "https://mus-api-prod.zhiliaoapp.com//rest/discover/user/uservo_followed/list?anchor=0&limit=20" +
				//"&___d=eyJhYyI6IkxJU1QiLCJieiI6Im11c2ljYWxzX293bmVkIiwiZG0iOiJNVVNJQ0FMIiwidmVyIjoidjIifQ%3D%3D" +
				"&target_user_id=68616495085350913&user_vo_relations=f";

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("network", "WiFi");
		headers.put("X-Requested-With", "XMLHttpRequest");
		headers.put("build", "1498482131274");
		headers.put("country", "CN");
		headers.put("flavor-type", "muse");
		headers.put("mobile", "Genymotion Google Ne");
		headers.put("version", "5.8.0");
		headers.put("language", "en_US");
		headers.put("User-Agent", "Musical.ly/2017062601 (Android; Genymotion Google Nexus 5X - 6.0.0 - API 23 - 1080x1920 6.0;rv:23)");
		headers.put("Connection", "GMT-04:00");
		headers.put("os", "android 6.0");
		headers.put("X-Request-ID", "1871d73c-ac08-4717-ade3-338a4108b50f");
		headers.put("X-Request-Info5", "eyJvcyI6ImFuZHJvaWQgNi4wIiwidmVyc2lvbiI6IjUuOC4wIiwic2xpZGVyLXNob3ctY29va2llIjoiYjJjdFYzSXhRa2xNU0RGS2FWWnpObEJWZWxkc2VFWTFRbXB0WTE5M1pXTm9ZWFE2ZVRaMWFERkJXV2Q0T0d4aFlXUjNTelZZWmt0UFVUMDlPbUkyWldZeE1UWXdaRE5oTlRFMk5qY3lZV1V5WlRJMFlUTXhPVEUzTWpZMU9qSTFPRGd5TXpBeE1EYzRNRGs1TVRRNE9BIiwiWC1SZXF1ZXN0LUlEIjoiMTg3MWQ3M2MtYWMwOC00NzE3LWFkZTMtMzM4YTQxMDhiNTBmIiwibWV0aG9kIjoiR0VUIiwidXJsIjoiaHR0cHM6XC9cL211cy1hcGktcHJvZC56aGlsaWFvYXBwLmNvbVwvXC9yZXN0XC9kaXNjb3ZlclwvZmVlZFwvb2xkX2ZlZWRzX3VucmVhZGNvdW50P19fX2Q9ZXlKaFl5STZJbE5KVGtkTVJTSXNJbUo2SWpvaVptVmxaSE5mWm05c2JHOTNYM1Z1Y21WaFpGOWpiM1Z1ZENJc0ltUnRJam9pUmtWRlJDSXNJblpsY2lJNkltOXNaRjltWldWa2N5SjkmYW5jaG9yPTI1OTkxOTQxMzU0ODczNjUxMiIsIm9zdHlwZSI6ImFuZHJvaWQiLCJkZXZpY2VpZCI6ImEwOTVmYjhhZDE5OWJhNGVkMDk1NzI4MjcxOTlkNzU5ZWUxMDIxMDIiLCJ0aW1lc3RhbXAiOjE1MDIzMTU3NzA5MzV9");
		headers.put("X-Request-Sign5", "01a661cd8089480e6eb18bc3c159685a457e411a5964");
		headers.put("Authorization", "M-TOKEN \"hash\"=\"N2EyODM1MTcwMDAxMDA0MjY0NTUyMDRiMzE0YzUwMDA4NTIzODE4NTA2ZjM2ZWI4NTI3ZDg2Nzg0NzFjMWU2YjI0YzAzMDg4MzI4ODBhZWRkMmEwZDUxOWQ5NmNlZmIyN2IxMmEzZWUyOTkwMmEwMGNkMzBiOGRkOTAyNGY4YTU3NzAyMDY0ZTViOTU4ZmNlYWIzYmEzZTJkM2Y5NDFmZjk1MzhmMGE3MTk3YmZjOTUyMTcyZmI1N2RmZTMzMmI4MjZlZWEwZDNhMjc4MzRkZDY0OTVhZmU0ZDhmNGQ0Y2IzMjcwMzM0OGUwNTljN2Y5YzU0MTFiZTg4ZTA2NjkxNGRlNGMyNDU1OTg3YWFjNjZjNDVlMjc2ODM3YzUyZmNkYTRiYzczM2U1MzExOWVjYTA0ZDI3MTVkODU5OGRhNGM3ZTkwODQxM2UwMDc3MDRjMzVlYjhhOGM4NmYyMWVjNzc3NWNmNTNmODQwMmQ1MDFlZTY4ZTQ5OTZlYjNjNDViYmRiMmYzMDM3NTQyNTQwMQ==\"");
		headers.put("Host", "mus-api-prod.zhiliaoapp.com");
		headers.put("Accept-Encoding", "gzip");


		Task<Task> t = new Task(url, headers, null, null, null);

		BasicRequester.getInstance().submit(t, 30000);

		if(t.exception != null) {
			t.exception.printStackTrace();
		}

		System.err.println(t.getDuration() + "\n" + t.getResponse().getText());
	}
}