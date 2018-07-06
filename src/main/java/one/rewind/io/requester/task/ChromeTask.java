package one.rewind.io.requester.task;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.exception.TaskException;
import one.rewind.txt.URLUtil;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class ChromeTask extends Task {

	public static Map<Class<? extends ChromeTask>, Builder> Builders = new HashMap<>();

	/**
	 *
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults){

		try {


			Builders.put(clazz, new Builder(
					clazz,
					url_template,
					init_map_class,
					init_map_defaults
			));

		} catch (Exception e) {
			ChromeDriverAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}

	}

	// 执行动作列表
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<ChromeAction> actions = new ArrayList<>();

	public String scheduledTaskId;

	private static class Builder {

		// 变量表达式
		public static String varPattern = "[\\w\\W][\\w\\W\\d\\_]*";

		// 初始化参数类型
		public Map<String, Class> init_map_class;

		// 初始化参数默认值
		public Map<String, Object> init_map_defaults;

		// URL模板
		public String url_template;

		// domain
		public String domain;

		// 该任务是否需要登录
		public boolean need_login = false;

		// 任务的优先级
		public Priority base_priority = Priority.MEDIUM;

		public Builder(
				Class<? extends ChromeTask> clazz,
				String url_template,
				Map<String, Class> init_map_class,
				Map<String, Object> init_map_defaults
		) throws Exception {

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

			// 验证url_template  \{\{[\w\W][\w\W\d\_]*\}\}
			Pattern p = Pattern.compile("\\{\\{.*?\\}\\}");
			Matcher m = p.matcher(url_template);
			Set<String> vars = new HashSet<>();
			while (m.find()) {
				vars.add(m.group().replaceAll("\\{\\{|\\}\\}", ""));
			}

			if(vars.containsAll(init_map_class.keySet()) && init_map_class.keySet().containsAll(vars)) {

			} else {
				throw new Exception("Illegal url_template.");
			}

			this.url_template = url_template;
			this.init_map_class = init_map_class;
			this.init_map_defaults = init_map_defaults;
			this.domain = URLUtil.getDomainName(url_template.replaceAll("\\{\\{|\\}\\}", ""));
		}
	}

	/**
	 * 验证初始参数Map
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> validateInitMap(Builder builder, Map<String, Object> init_map) throws Exception {

		Map<String, Object> init = new HashMap<>();

		// 默认参数赋值
		for(String key : builder.init_map_defaults.keySet()) {

			if(builder.init_map_class.containsKey(key)) {

				if(builder.init_map_defaults.get(key).getClass().equals(builder.init_map_class.get(key))){

					init.put(key, builder.init_map_defaults.get(key));

				} else {
					throw new Exception("init_map_defaults value type error.");
				}

			} else {

				throw new Exception("init_map_defaults define error.");
			}
		}

		// 使用初始化参数进行赋值
		for(String key : init_map.keySet()) {

			if(builder.init_map_class.containsKey(key)) {

				if(init_map.get(key).getClass().equals(builder.init_map_class.get(key))){

					init.put(key, init_map.get(key));

				} else {
					throw new Exception("init_map value type error.");
				}

			} else {
				throw new Exception("init_map define error.");
			}
		}

		// 验证生成参数是否包含所有变量
		for(String key : builder.init_map_class.keySet()) {
			if(!init.keySet().contains(key)) {
				throw new Exception("init_map error, "+ key +" not defined.");
			}
		}

		return init;
	}

	/**
	 *
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static Priority getBasePriority(Class<? extends ChromeTask> clazz) throws Exception {

		Builder builder = Builders.get(clazz);

		if(builder == null) throw new Exception("Builder not exist for " + clazz.getName());

		return builder.base_priority;
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<? extends ChromeTask> clazz, Map<String, Object> init_map) throws Exception {
		return build(clazz, init_map, null, 0, getBasePriority(clazz));
	}

	/**
	 *
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<? extends ChromeTask> clazz, Map<String, Object> init_map, String username) throws Exception {
		return build(clazz, init_map, username, 0, getBasePriority(clazz));
	}

	/**
	 *
	 * @param init_map
	 * @param username
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(Class<? extends ChromeTask> clazz, Map<String, Object> init_map, String username, int step) throws Exception {
		return build(clazz, init_map, username, step, getBasePriority(clazz));
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
	public static ChromeTask build(
			Class<? extends ChromeTask> clazz, Map<String, Object> init_map, String username, int step, Priority priority) throws Exception {

		Builder builder = Builders.get(clazz);

		if(builder == null) throw new Exception("Builder not exist for " + clazz.getName());

		// 重载后的初始化变量表
		Map<String, Object> vars = validateInitMap(builder, init_map);

		String url = builder.url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		Constructor<?> cons = clazz.getConstructor(String.class);

		ChromeTask task = (ChromeTask) cons.newInstance(url);
		if(builder.need_login) task.setLoginTask();
		task.setUsername(username);
		task.setStep(step);
		task.setPriority(priority);

		return task;
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
	 * 获取前置操作
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public List<ChromeAction> getActions() {
		return actions;
	}

	/**
	 * 添加前置操作
	 * ChromeDriverAgent 专用
	 * @param action
	 * @return
	 */
	public ChromeTask addAction(ChromeAction action) {
		this.actions.add(action);
		return this;
	}

	/**
	 *
	 * @param clazz
	 * @param username
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
			Class<? extends ChromeTask> clazz, Map<String, Object> init_map, String username, int step) throws Exception {

		Builder builder = Builders.get(clazz);

		if(builder == null) throw new Exception(clazz.getName() + " builder not exist.");

		return new ChromeTaskHolder(clazz.getName(), builder.domain, builder.need_login, username, init_map, step, builder.base_priority);
	}

	public static ChromeTaskHolder buildHolder(
			Class<? extends ChromeTask> clazz, String username, Map<String, Object> init_map) throws Exception {

		return buildHolder(clazz, init_map, username,0);
	}


	/**
	 *
	 * @param clazz
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
			Class<? extends ChromeTask> clazz, Map<String, Object> init_map, int step) throws Exception {

		return buildHolder(clazz, init_map, null, step);
	}

	/**
	 *
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
			Class<? extends ChromeTask> clazz, Map<String, Object> init_map) throws Exception {

		return buildHolder(clazz, init_map, null, 0);
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

		return new ChromeTaskHolder(this.getClass().getName(), getDomain(), isLoginTask(), getUsername(), init_map, step, priority);
	}
}
