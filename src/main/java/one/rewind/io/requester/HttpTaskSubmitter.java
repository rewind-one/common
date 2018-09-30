package one.rewind.io.requester;

import com.google.gson.reflect.TypeToken;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.task.Task;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;

public class HttpTaskSubmitter {

	// logger 日志
	public static final Logger logger = LogManager.getLogger(ChromeDistributor.class.getName());

	//
	public static HttpTaskSubmitter instance;

	// 单例
	public static HttpTaskSubmitter getInstance() {

		if (instance == null) {
			synchronized (HttpTaskSubmitter.class) {
				if (instance == null) {
					instance = new HttpTaskSubmitter();
				}
			}
		}

		return instance;
	}

	// ip
	public static String host = "127.0.0.1";
	// 端口
	public static int port = 80;

	public HttpTaskSubmitter() {}

	public HttpTaskSubmitter(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 *
	 * @param class_name
	 * @param username
	 * @param map_json
	 * @param step
	 * @param cron
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Msg submit(String class_name, String username, String map_json, int step, String... cron) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {

		String params = "";

		params += "class_name=" + class_name;

		params += "&vars=" + map_json;

		params += "&step=" + step;

		if (cron != null){

			for(String c : cron) {
				params += "&cron=" + URLEncoder.encode(c,"utf-8");
			}
		}

		if (username != null && username.length() > 0) {
			params += "&username=" + username;
		}

		String url = "http://" + host + ":" + port + "/task?" + params;

		Task task = new Task(url);
		task.setPost();
		BasicRequester.getInstance().submit(task);

		Type type = new TypeToken<Msg<Map<String, Object>>>(){}.getType();
		Msg<Map<String, Object>> msg = JSON.fromJson(task.getResponse().getText(), type);

		logger.info(JSON.toPrettyJson(msg));

		return msg;
	}

	/**
	 *
	 * @param clazz
	 * @param map_json
	 * @return
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	public Msg submit(String clazz, String map_json) throws ClassNotFoundException, MalformedURLException, URISyntaxException, UnsupportedEncodingException {
		return submit(clazz, null, map_json, 0);
	}

	/**
	 *
	 * @param clazz
	 * @param username
	 * @param map_json
	 * @return
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	public Msg submit(String clazz, String username, String map_json) throws ClassNotFoundException, MalformedURLException, URISyntaxException, UnsupportedEncodingException {
		return submit(clazz, username, map_json, 0);
	}

	/**
	 *
	 * @param clazz
	 * @param username
	 * @param map_json
	 * @param step
	 * @return
	 * @throws ClassNotFoundException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	public Msg submit(String clazz, String username, String map_json, int step) throws ClassNotFoundException, MalformedURLException, URISyntaxException, UnsupportedEncodingException {
		return submit(clazz, username, map_json, step);
	}
}
