package one.rewind.io.requester.parser;

import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.URLUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.rewind.io.requester.task.TaskHolder.Flag;

/**
 * TaskHolder 生成器
 *
 * @author scisaga@gmail.com
 * @date 2018/10/9
 */
public class Builder implements JSONable<Builder> {

	// 变量表达式
	public static String varPattern = "[\\w\\W][\\w\\W\\d\\_]*";

	// 初始化参数类型
	public Map<String, Class> init_map_class;

	// 初始化参数默认值
	public Map<String, Object> init_map_defaults;

	// URL模板
	public String url_template;

	// POST DATA 模板
	public String post_data_template;

	// domain
	public String domain;

	// 相同 fingerprint 最小采集间隔
	public long min_interval = 0;

	// 该任务是否需要登录
	public boolean need_login = false;

	// 任务的优先级
	public Task.Priority base_priority = Task.Priority.MEDIUM;

	// 任务标签
	public List<Flag> flags = new ArrayList<>();

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @return
	 * @throws Exception
	 */
	public static Builder getBuilder(String url_template, Flag... flags) throws Exception {

		return getBuilder(url_template, null, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param post_data_template
	 * @return
	 * @throws Exception
	 */
	public static Builder getBuilder(
			String url_template,
			String post_data_template,
			Flag... flags
	) throws Exception {

		Set<String> vars = Builder.getVarNames(url_template + post_data_template);

		Map<String, Class> init_map_class = new HashMap<>();
		Map<String, Object> init_map_defaults = new HashMap<>();

		for(String key : vars) {
			init_map_class.put(key, String.class);
		}

		return new Builder(url_template, post_data_template, init_map_class, init_map_defaults, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param init_map_class
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			Map<String, Class> init_map_class,
			Flag... flags
	) throws Exception {
		this(url_template, null, init_map_class, new HashMap<String, Object>(), 0, false, Task.Priority.MEDIUM, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			Flag... flags
	) throws Exception {
		this(url_template, null, init_map_class, init_map_defaults, 0, false, Task.Priority.MEDIUM, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param post_data_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			String post_data_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			Flag... flags
	) throws Exception {
		this(url_template, post_data_template, init_map_class, init_map_defaults, 0, false, Task.Priority.MEDIUM, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			long min_interval,
			Flag... flags
	) throws Exception {
		this(url_template, null, init_map_class, init_map_defaults, min_interval, false, Task.Priority.MEDIUM, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param post_data_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			String post_data_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			long min_interval,
			Flag... flags
	) throws Exception {
		this(url_template, post_data_template, init_map_class, init_map_defaults, min_interval, false, Task.Priority.MEDIUM, flags);
	}

	/**
	 * 生成新的Builder
	 *
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param need_login
	 * @param base_priority
	 * @throws Exception
	 */
	public Builder(
			String url_template,
			String post_data_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			long min_interval,
			boolean need_login,
			Task.Priority base_priority,
			Flag... flags
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
		Set<String> vars = getVarNames(url_template + post_data_template);

		// init_map 包含 url_template 和 post_data_template 的变量
		if(!init_map_class.keySet().containsAll(vars)) {
			throw new Exception("Illegal url_template.");
		}

		this.url_template = url_template;
		this.post_data_template = post_data_template;
		this.init_map_class = init_map_class;
		this.init_map_defaults = init_map_defaults;

		this.min_interval = min_interval;

		// TODO url_template 中应该有完整的域名，否则此处会抛异常
		this.domain = URLUtil.getDomainName(url_template.replaceAll("\\{\\{|\\}\\}", ""));

		this.need_login = need_login;
		this.base_priority = base_priority;

		this.flags = Arrays.asList(flags);
	}

	/**
	 * 获取变量名
	 * @return
	 */
	public static Set<String> getVarNames(String template) {

		Set<String> vars = new HashSet<>();

		Pattern p = Pattern.compile("\\{\\{.*?\\}\\}");
		Matcher m = p.matcher(template);

		while (m.find()) {
			vars.add(m.group().replaceAll("\\{\\{|\\}\\}", ""));
		}

		return vars;
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

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}