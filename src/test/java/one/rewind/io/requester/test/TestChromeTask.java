package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.txt.StringUtil;
import one.rewind.txt.URLUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

public class TestChromeTask extends ChromeTask {

	static {
		// init_map_class
		init_map_class = ImmutableMap.of("admain", String.class);
		// init_map_defaults
		init_map_defaults = ImmutableMap.of("admain", "baidu");
		// url_template
		url_template = "https://www.{{admain}}.com";

		need_login = true;

		base_priority = Priority.HIGH;
	}

	/*public static String domain() throws MalformedURLException, URISyntaxException {
		return "zbj.com";
	}*/


	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestChromeTask(String url) throws MalformedURLException, URISyntaxException {

		super(url);

		this.addDoneCallback((t) -> {

			/*ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t).scheduledTaskId);*/

			System.err.println(t.getResponse().getText().length());

			System.err.println(this.getDomain());

		});

		this.addAction(new LoginWithGeetestAction(new AccountImpl("zbj.com","17600668061","gcy116149")));

	}

	public static void main(String[] args) throws Exception {

		Map<String, String> map = new HashMap<>();
		map = ImmutableMap.of("admain","zbj");

		String url = getPostURL(TestChromeTask.class, "", map,0,"",true,null,null);

		System.err.println(url);

		Class.forName(TestChromeTask.class.getName());

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent();

		distributor.addAgent(agent);

		ChromeTask task = new ChromeTask(url);

		task.setPost();

		BasicRequester.getInstance().submit(task);
		/*ResponseBody responseBody = JSON.fromJson(task.getResponse().getText(), ResponseBody.class);*/

	}

	/**
	 * 生成task的url地址
	 * @param clazz
	 * @param username

	 * @param step
	 * @param domain
	 * @param needLogin
	 * @param getBasePriority
	 * @param cron
	 * @return
	 */
	public static String getPostURL(Class clazz, String username, Map<String, String> map, Integer step, String domain, Boolean needLogin, String getBasePriority, String cron) {

		String classname_ = "class_name="+clazz.getName();

		String init_map = "";

		String json =new Gson().toJson(map);
		System.err.println(json);

		init_map ="&init_map=" + json;

		/*if (key != null && value != null && key.length > 0) {
			String start = "&init_map={";
			String end = "}";
			String s="";
			for (int i = 0; i< key.length; i++ ) {
				if (key[i] != null) {
					if (i>0) {
						s = s +","+ "\"" + key[i] + "\":\"" + value[i] + "\"";
					} else {
						s = "\"" + key[i] + "\":\"" + value[i] + "\"";
					}
				}

			}
			init_map = start + s + end;
		}*/

		String step_ = "";
		if (step!=null) {
			step_ = "&step=" + step;
		}

		if (cron != null && cron!= "") {
			try {
				cron = URLEncoder.encode(cron,"utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			cron = "&cron=" + cron;
		} else {
			cron = "";
		}
		if (username != null && username!= "") {
			username = "&username=" + username;
		} else {
			username ="";
		}
		String url = "http://localhost/task?"+classname_+username+init_map+step_+cron;

		return url;
	}


	class ResponseBody {

		public String code;
		public String msg;
		public Map<String ,String> data;
	}
}
