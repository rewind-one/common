package one.rewind.io.requester.task;

import one.rewind.io.requester.exception.TaskException;
import one.rewind.txt.URLUtil;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class ChromeTask extends Task {

	public static String varPattern = "[\\w\\W][\\w\\W\\d\\_]*";

	// 初始化参数类型
	public static Map<String, Class> init_map_class;

	// 初始化参数默认值
	public static Map<String, Object> init_map_defaults;

	// URL模板
	public static String url_template;

	// 该任务是否需要登录
	public static boolean need_login = false;

	// 任务的优先级
	public static Priority base_priority = Priority.MEDIUM;

	/**
	 * 静态验证
	 * @throws Exception
	 */
	public static void staticValidate() throws Exception {

		// 验证class定义
		for(String key : init_map_class.keySet()) {
			if (!key.matches(varPattern)) {
				throw new Exception("var name define error.");
			}
		}

		// 验证默认值合法性
		for(String key : init_map_defaults.keySet()) {

			if(init_map_class.containsKey(key)) {

				if(init_map_defaults.get(key).getClass().equals(init_map_class.get(key))){

				} else {
					throw new Exception("init_map_defaults value type error.");
				}

			} else {
				throw new Exception("init_map_defaults define error.");
			}
		}

		// 验证url_template
		Pattern p = Pattern.compile("\\{\\{[\\w\\W][\\w\\W\\d\\_]*\\}\\}");
		Matcher m = p.matcher(url_template);
		Set<String> vars = new HashSet<>();
		while (m.find()) {
			vars.add(m.group().replaceAll("\\{\\{|\\}\\}", ""));
		}

		if(vars.containsAll(init_map_class.keySet()) && init_map_class.keySet().containsAll(vars)) {

		} else {
			throw new Exception("Illegal url_template.");
		}
	}

	/**
	 * 验证初始参数Map
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> validateInitMap(Map<String, Object> init_map) throws Exception {

		Map<String, Object> init = new HashMap<>();

		// 默认参数赋值
		for(String key : init_map_defaults.keySet()) {

			if(init_map_class.containsKey(key)) {

				if(init_map_defaults.get(key).getClass().equals(init_map_class.get(key))){

					init.put(key, init_map_defaults.get(key));

				} else {
					throw new Exception("init_map_defaults value type error.");
				}

			} else {
				throw new Exception("init_map_defaults define error.");
			}
		}

		// 使用初始化参数进行赋值
		for(String key : init_map.keySet()) {

			if(init_map_class.containsKey(key)) {

				if(init_map.get(key).getClass().equals(init_map_class.get(key))){

					init.put(key, init_map.get(key));

				} else {
					throw new Exception("init_map value type error.");
				}

			} else {
				throw new Exception("init_map define error.");
			}
		}

		// 验证生成参数是否包含所有变量
		for(String key : init_map_class.keySet()) {
			if(!init.keySet().contains(key)) {
				throw new Exception("init_map error, "+ key +" not defined.");
			}
		}

		return init;
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<?> clazz, Map<String, Object> init_map) throws Exception {
		return build(clazz, init_map, null, 0, getBasePriority());
	}

	/**
	 *
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<?> clazz, Map<String, Object> init_map, String username) throws Exception {
		return build(clazz, init_map, username, 0, getBasePriority());
	}

	/**
	 *
	 * @param init_map
	 * @param username
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<?> clazz, Map<String, Object> init_map, String username, int step) throws Exception {
		return build(clazz, init_map, username, step, getBasePriority());
	}

	/**
	 * 通过TaskHolder build Task
	 * @param init_map
	 * @param username
	 * @param step
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<?> clazz, Map<String, Object> init_map, String username, int step, Priority priority) throws Exception {

		staticValidate();

		Map<String, Object> vars = validateInitMap(init_map);

		String url = url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		Constructor<?> cons = clazz.getConstructor(String.class);

		ChromeTask task = (ChromeTask) cons.newInstance(url);
		if(needLogin()) task.setLoginTask();
		task.setUsername(username);
		task.setStep(step);
		task.setPriority(priority);

		return task;
	}

	/**
	 *
	 * @return
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public static String domain() throws MalformedURLException, URISyntaxException {
		return URLUtil.getDomainName(url_template.replaceAll("\\{\\{|\\}\\}", ""));
	}

	/**
	 *
	 * @return
	 */
	public static boolean needLogin() {
		return need_login;
	}

	/**
	 *
	 * @return
	 */
	public static Priority getBasePriority() {
		return base_priority;
	}

	/**
	 *
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public ChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);

	}

	/**
	 *
	 * @param init_map
	 * @return
	 */
	public ChromeTaskHolder buildHolder(Map<String, Object> init_map) throws TaskException.NoMoreStepException, MalformedURLException, URISyntaxException {

		return buildHolder(init_map, getPriority());
	}

	/**
	 *
	 * @param init_map
	 * @param priority
	 * @return
	 * @throws TaskException.NoMoreStepException
	 */
	public ChromeTaskHolder buildHolder(Map<String, Object> init_map, Priority priority) throws TaskException.NoMoreStepException, MalformedURLException, URISyntaxException {

		int step = 0;

		if(getStep() == 1 || getStep() < 0) {
			throw new TaskException.NoMoreStepException();
		} else {
			step = getStep() - 1;
		}

		return new ChromeTaskHolder(this.getClass().getName(), domain(), isLoginTask(), getUsername(), init_map, step, priority);
	}
}
