package one.rewind.io.requester.test;

import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * BasicRequester 的测试方法
 * Created by karajan on 2017/6/3.
 */
public class BasicRequesterTest {

	@Test
	public void basicFetch() throws Exception {

		String url = "https://www.baidu.com";
		String cookies = "JSESSIONID=BE83E060C0FECB71715418659ECFFD63; cninfo_search_record_cookie=%E6%8B%9B%E5%95%86%E9%93%B6%E8%A1%8C";
		String ref = "http://www.cninfo.com.cn/cninfo-new/disclosure/szse/showFulltext/600036";

		Task<Task> t = new Task(url, null, cookies, ref);
		t.setPreProc();
		Proxy proxy = new ProxyImpl("127.0.0.1", 3128, null, null);
		t.setProxy(proxy);

		BasicRequester.getInstance().submit(t, 30000);

		for(Throwable e : t.getExceptions()) {
			e.printStackTrace();
		}

		System.err.println(t.getDuration() + "\t" + t.getResponse().getText());
	}

	@Test
	public void domBuildTest() throws Exception {

		String url = "https://www.baidu.com";

		Task<Task> t = new Task(url);
		t.setBuildDom();

		BasicRequester.getInstance().submit(t, 30000);

		for(Throwable e : t.getExceptions()) {
			e.printStackTrace();
		}

		System.err.println(t.getDuration() + "\t" + t.getResponse().getDoc().select("#su").attr("value"));

	}

	/**
	 * TODO
	 * 已知问题：https请求 使用需要验证的proxy 会出错 原因是 HttpURLConnection不支持单独设定Authenticator
	 * 解决方法：使用Apache HttpClient
	 */
	@Test
	public void testSwitchProxy() throws Exception {

		for(int i=0; i<1; i++) {

			Thread thread = new Thread(() -> {

				for(int j=0; j<1; j++) {

					Task<Task> t;

					try {

						if(j % 2 == 0){
							t = new Task("http://carnegieeurope.eu/search/?qry=&maxrow=10&lang=en&search_op=&search_mode=&search_sort=articlePubDate%20desc&tabName=date&fltr=&pageOn=1");
							/*ProxyWrapper pw = new ProxyWrapperImpl("198.23.253.200", 59998, "tfelab", "TfeLAB2@15");
							t.setProxy(pw);*/
						} else {
							t = new Task("https://www.baidu.com/s?wd=ip");
							/*ProxyWrapper pw = new ProxyWrapperImpl("124.206.133.227", 80, null, null);
							t.setProxy(pw);*/
						}
						t.setPreProc();

						BasicRequester.getInstance().submit(t, 30000);

						for(Throwable e : t.getExceptions()) {
							e.printStackTrace();
						}

						System.err.println(t.getDuration() + "\t" + t.getResponse().getText());

					} catch (MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}

				}
			});

			thread.start();
		}
	}

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

		for(Throwable e : t.getExceptions()) {
			e.printStackTrace();
		}

		System.err.println(t.getDuration() + "\n" + t.getResponse().getText());

	}

	@Test
	public void postTest() throws MalformedURLException, URISyntaxException {

		String url = "http://www.baidu.com";

		Task<Task> t = new Task(url);
		t.addDoneCallback((task)->{
			System.out.println("A");
		});

		String json = t.toJSON();

		Task t_ = JSON.fromJson(json, Task.class);

		BasicRequester.getInstance().submit(t_,10000);

		System.err.println(t.getResponse().getText());
	}

	/**
	 * Basic proxy authentication for HTTPS URLs returns HTTP/1.0 407 Proxy Authentication Required
	 * https://stackoverflow.com/questions/34877470/basic-proxy-authentication-for-https-urls-returns-http-1-0-407-proxy-authenticat
	 */
	@Test
	public void testProxiedHttpsRequest() throws Exception {

		Proxy proxy = new ProxyImpl("http-dyn.abuyun.com",9020,"HC712I309A6549HD","B9BA137D9DD68EAC");

		Task<Task> task = new XueQiuTask("https://xueqiu.com/friendships/groups/members.json?uid=3013624218&page=1&gid=0");

		task.setProxy(proxy);

		BasicRequester.getInstance().submit(task);

		for (TaskCallback taskCallback : task.doneCallbacks) {
			taskCallback.run(task);
		}

		for(Throwable throwable : task.getExceptions()) {

		}
	}
}

class XueQiuTask extends Task{

	public static HashMap<String, String> genHeaders() {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Host", "xueqiu.com");
		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
		// Accept-Encoding 不能为gzip
		headers.put("Accept-Encoding", "*");
		headers.put("Accept-Charset", "utf-8,gb2312;q=0.8,*;q=0.8");
		headers.put("Cache-Control", "max-age=0");
		headers.put("Connection", "keep-alive");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("Pragma", "no-cache");
		headers.put("Cookie", "aliyungf_tc=AQAAAOY2Bl60MAoAe4bzdPn++5JA5RVK; __utmc=1; device_id=bc931461d2e2f6e7f546f2b92106d760; __utmz=1.1525920324.2.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); s=fl11xr6rdk; _ga=GA1.2.956805427.1524307479; __utma=1.956805427.1524307479.1526635062.1526965879.6; Hm_lvt_1db88642e346389874251b5a1eded6e3=1533048030; _gid=GA1.2.245365313.1533048031; _gat_gtag_UA_16079156_4=1; xq_a_token=aef774c17d4993658170397fcd0faedde488bd20; xq_a_token.sig=F7BSXzJfXY0HFj9lqXif9IuyZhw; xq_r_token=d694856665e58d9a55450ab404f5a0144c4c978e; xq_r_token.sig=Ozg4Sbvgl2PbngzIgexouOmvqt0; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1533048054; u=361533048054356");
		return headers;

	}

	public XueQiuTask(String url) throws MalformedURLException, URISyntaxException {

		super(url, genHeaders(), "","", "");

		this.addDoneCallback((t) -> {

			String src = new String(getResponse().getSrc(), "UTF-8");

			String text = getResponse().getText();
			System.err.println(text);

		});
	}
}
