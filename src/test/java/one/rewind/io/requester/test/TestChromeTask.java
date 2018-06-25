package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
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
		init_map_class = ImmutableMap.of("question", String.class);
		// init_map_defaults
		init_map_defaults = ImmutableMap.of("question", "ip");
		// url_template
		url_template = "https://shop.zbj.com/{{question}}/";
	}

	/**
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public TestChromeTask(String url) throws MalformedURLException, URISyntaxException {

		super(url);

		this.addDoneCallback((t) -> {

			ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t).scheduledTaskId);

			if (map_index.containsKey(getUrl())) {
				map_index.put(getUrl(), map_index.get(getUrl()) + 1);
			} else {
				map_index.put(getUrl(), 0);
			}

			System.err.println(t.getResponse().getText().length());

			if (map_index.get(getUrl()) == 2) {

				System.err.println("22222222222222222222");
				ChromeTask task1 = new ChromeTask("http://localhost/task/unschedule/" + map_id.get(this.getUrl()));
				System.err.println(task1.getUrl());
				task1.setPost();
				BasicRequester.getInstance().submit(task1);

				Thread.sleep(10000);

				String url1 = getPostURL(TestChromeTask.class, "","question","11656226",0,"",true,null,"*/2 * * * *");
				task1 = new ChromeTask(url1);
				task1.setPost();
				BasicRequester.getInstance().submit(task1);

			}

		});

		/*this.addAction(new LoginWithGeetestAction(new AccountImpl("zbj.com","17600668061","gcy116149")));*/
	}



	public static void main(String[] args) throws Exception {



		String url = getPostURL(TestChromeTask.class, "","question","11656226",0,"",true,null,"* * * * *");

		System.err.println(url);

		Class.forName(TestChromeTask.class.getName());

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent();

		distributor.addAgent(agent);

		ChromeTask task = new ChromeTask(url);

		task.setPost();

		BasicRequester.getInstance().submit(task);

		System.err.println(task.getResponse().getText());

		ResponseBody responseBody = JSON.fromJson(task.getResponse().getText(), ResponseBody.class);

		ChromeTask.map_id.put(TestChromeTask.url_template.replace("{{question}}","11656226"), responseBody.data.get("id"));

		System.err.println(TestChromeTask.url_template.replace("{{question}}","11656226"));
		System.err.println(responseBody.data.get("id"));

	}

	/**
	 * 生成task的url地址
	 * @param clazz
	 * @param username
	 * @param key
	 * @param value
	 * @param step
	 * @param domain
	 * @param needLogin
	 * @param getBasePriority
	 * @param cron
	 * @return
	 */
	public static String getPostURL(Class clazz, String username, String key, String value, Integer step, String domain, Boolean needLogin, String getBasePriority, String cron) {

		String classname_ = "class_name="+clazz.getName();

		String init_map = "";
		if (key !=null && value!= null) {
			if (key != "") {
				init_map = "&init_map={\"" + key + "\":\"" + value + "\"}";
			}
		}

		String step_ = "";
		if (step!=null) {
			step_ = "&step=" + step;
		}

		String needLogin_ = "";
		if (needLogin != null) {
			needLogin_ = "&needLogin=" + needLogin;
		}
		if (domain !=null && domain!="") {
			domain = "&domain=" + domain;
		} else {
			domain ="";
		}
		if (getBasePriority != null && getBasePriority != "") {
			getBasePriority = "&getBasePriority=" + getBasePriority;
		} else {
			getBasePriority = "";
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
		String url = "http://localhost/task?"+classname_+username+init_map+step_+domain+needLogin_+getBasePriority+cron;

		return url;
	}


	class ResponseBody {

		public String code;
		public String msg;
		public Map<String ,String> data;
	}
}
