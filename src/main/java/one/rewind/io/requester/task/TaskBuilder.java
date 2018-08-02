package one.rewind.io.requester.task;

import one.rewind.txt.URLUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskBuilder {

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
	public Task.Priority base_priority = Task.Priority.MEDIUM;

	/**
	 *
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @throws Exception
	 */
	public TaskBuilder(
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults
	) throws Exception {
		this(url_template, init_map_class, init_map_defaults, false, Task.Priority.MEDIUM);
	}

	/**
	 *
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param need_login
	 * @param base_priority
	 * @throws Exception
	 */
	public TaskBuilder(
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			boolean need_login,
			Task.Priority base_priority
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

	/**
	 * 验证初始参数Map
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> validateInitMap(
			Map<String, Object> init_map
	) throws Exception {

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
					throw new Exception("vars value type error, " + key + ":" + init_map.get(key).getClass() + " -- " + init_map_class.get(key));
				}

			} else {
				throw new Exception("vars define error.");
			}
		}

		// 验证生成参数是否包含所有变量
		for(String key : init_map_class.keySet()) {
			if(!init.keySet().contains(key)) {
				throw new Exception("vars error, "+ key +" not defined.");
			}
		}

		return init;
	}
}
