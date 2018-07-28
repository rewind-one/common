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

	public static long MIN_INTERVAL = 10000;

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

	/**
	 *
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param need_login
	 * @param base_priority
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			boolean need_login,
			Priority base_priority
	){

		try {

			Builders.put(clazz, new Builder(
					clazz,
					url_template,
					init_map_class,
					init_map_defaults,
					need_login,
					base_priority
			));

		} catch (Exception e) {
			ChromeDriverAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
	}

	/**
	 *
	 */
	public static class Builder {

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
			this(clazz, url_template, init_map_class, init_map_defaults, false, Priority.MEDIUM);
		}

		public Builder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			boolean need_login,
			Priority base_priority
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

			if(!init_map_class.keySet().containsAll(vars)) {
				throw new Exception("Illegal url_template.");
			}

			this.url_template = url_template;
			this.init_map_class = init_map_class;
			this.init_map_defaults = init_map_defaults;
			this.domain = URLUtil.getDomainName(url_template.replaceAll("\\{\\{|\\}\\}", ""));
			this.need_login = need_login;
			this.base_priority = base_priority;
		}
	}

	/**
	 * 验证初始参数Map
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> validateInitMap(
		Builder builder,
		Map<String, Object> init_map
	) throws Exception {

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
					throw new Exception("init_map value type error, " + key + ":" + init_map.get(key).getClass() + " -- " + builder.init_map_class.get(key));
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
	 * @param builder
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static String generateURL(
		Builder builder,
		Map<String, Object> init_map
	) throws Exception {

		// 重载后的初始化变量表
		Map<String, Object> vars = validateInitMap(builder, init_map);

		String url = builder.url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		return url;
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
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username,
		int step,
		Priority priority
	) throws Exception {

		Builder builder = Builders.get(clazz);

		if(builder == null) throw new Exception("Builder not exist for " + clazz.getName());

		// 生成URL
		String url = generateURL(builder, init_map);

		Constructor<?> cons = clazz.getConstructor(String.class);

		ChromeTask task = (ChromeTask) cons.newInstance(url);

		task.init_map = validateInitMap(builder, init_map); // 多余计算

		if(builder.need_login) task.setLoginTask();
		task.setUsername(username);
		task.setStep(step);
		task.setPriority(priority);

		return task;
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map
	) throws Exception {
		return build(clazz, init_map, null, 0, getBasePriority(clazz));
	}

	/**
	 *
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static ChromeTask build(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username
	) throws Exception {
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
	public static ChromeTask build(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username,
		int step
	) throws Exception {
		return build(clazz, init_map, username, step, getBasePriority(clazz));
	}

	// 初始化参数
	public Map<String, Object> init_map;

	// 执行动作列表
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<ChromeAction> actions = new ArrayList<>();

	// 周期任务ID
	public String _scheduledTaskId;

	// 标志位 是否采集图片
	public boolean noFetchImages = false;

	/**
	 * 手动设定id
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
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
	 * TODO 与 setRequestFilter 和 setResponseFilter 方法冲突
	 * 对图片请求进行过滤，图片只请求第一次
	 * @return
	 */
	public ChromeTask setNoFetchImages() {
		noFetchImages = true;
		return this;
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
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @param username
	 * @param step
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username,
		int step,
		Priority priority
	) throws Exception {

		Builder builder = Builders.get(clazz);

		if(builder == null) throw new Exception(clazz.getName() + " builder not exist.");

		if(priority == null) {
			priority = builder.base_priority;
		}

		return new ChromeTaskHolder(clazz.getName(), builder.domain, builder.need_login, username, init_map, step, priority);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param username
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username,
		int step
	) throws Exception {

		return buildHolder(clazz, init_map, username, step, null);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param username
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
		Class<? extends ChromeTask> clazz,
		String username,
		Map<String, Object> init_map
	) throws Exception {

		return buildHolder(clazz, init_map, username, 0);
	}


	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		int step
	) throws Exception {

		return buildHolder(clazz, init_map, null, step);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static ChromeTaskHolder buildHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map
	) throws Exception {

		return buildHolder(clazz, init_map, null, 0);
	}

	/**
	 * 任务执行过程中调用
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public ChromeTaskHolder getHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map
	) throws Exception {

		return getHolder(clazz, init_map, getPriority());
	}

	/**
	 * 任务执行过程中调用
	 * @param clazz
	 * @param init_map
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public ChromeTaskHolder getHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		Priority priority
	) throws Exception {

		int step = 0;

		if(getStep() == 1 || getStep() < 0) {
			throw new TaskException.NoMoreStepException();
		} else {
			step = getStep() - 1;
		}

		return buildHolder(clazz, init_map, getUsername(), step);
	}
}
