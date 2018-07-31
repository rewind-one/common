package one.rewind.io.requester.test;

import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.OkHttpRequester;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OkHttpRequesterTest {

	@Test
	public void basicTest() throws Exception {

		Proxy proxy = new ProxyImpl("118.190.44.184",59998,"tfelab","TfeLAB2@15");
		//example.setProxy(proxy);

		Task<Task> task = new XueQiuTask("https://xueqiu.com/friendships/groups/members.json?uid=3013624218&page=1&gid=0");

		//task.setPost();

		task.setProxy(proxy);

		//task.setHeaders(genHeaders());

		BasicRequester.getInstance().submit(task);

		for (TaskCallback taskCallback : task.doneCallbacks) {
			taskCallback.run(task);
		}

		//System.out.println(new String(task.getResponse().getSrc(), "UTF-8"));

		//Thread.sleep(100000);
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
		headers.put("Cookie", "s=eq12h2qes7; _ga=GA1.2.1912659196.1531978729; _gid=GA1.2.433629090.1531978729; device_id=fd6e2ca5e2d7506846026f2de503b50e; xqat=114d0743c7a8bd3a2c431f898c893342ab6914ea; bid=ddcf77e3808489ee6c12a105113ea8a8_jjs57zag; __lnkrntdmcvrd=-1; xq_a_token.sig=3dXmfOS3uyMy7b17jgoYQ4gPMMI; xq_r_token.sig=6hcU3ekqyYuzz6nNFrMGDWyt4aU; Hm_lvt_1db88642e346389874251b5a1eded6e3=1531978728,1532066349,1532090836; xq_a_token=114d0743c7a8bd3a2c431f898c893342ab6914ea; xq_r_token=ae3083a4b927e33b0894e91803036e9b5cb8e75d; xq_token_expire=Tue%20Aug%2014%202018%2020%3A47%3A40%20GMT%2B0800%20(CST); xq_is_login=1; u=9132619086; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1532090898");
		return headers;

	}

	public XueQiuTask(String url) throws MalformedURLException, URISyntaxException {
		/*super(url,  XueQiuTask.genHeaders(), "","","");*/
		super(url, genHeaders(), "","", "");

		this.addDoneCallback((t) -> {

			String src = new String(getResponse().getSrc(), "UTF-8");

			String test = getResponse().getText();
			System.err.println(src);

			Pattern p = Pattern.compile("(?s)<h2>(?<T>.+?)</h2>");
			Matcher m = p.matcher(src);

			while(m.find()) {
				System.err.println(m.group());
			}

		});
	}
}
