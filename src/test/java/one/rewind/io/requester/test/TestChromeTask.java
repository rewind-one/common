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

			System.err.println(t.getResponse().getText().length());


		});

		/*this.addAction(new LoginWithGeetestAction(new AccountImpl("zbj.com","17600668061","gcy116149")));*/
	}



	public static void main(String[] args) throws Exception {


		String url = getPostURL(TestChromeTask.class, "","question","11656226",0,"",true,null,"*/1 * * * *,*/2 * * * *,*/3 * * * *");

		System.err.println(url);

		Class.forName(TestChromeTask.class.getName());

		ChromeDriverDistributor distributor = ChromeDriverDistributor.getInstance();

		ChromeDriverAgent agent = new ChromeDriverAgent();

		distributor.addAgent(agent);

		ChromeTask task = new ChromeTask(url);

		task.setPost();

		BasicRequester.getInstance().submit(task);

		ResponseBody responseBody = JSON.fromJson(task.getResponse().getText(), ResponseBody.class);



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
