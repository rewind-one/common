package one.rewind.io.requester.task;

import one.rewind.io.requester.exception.TaskException;

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
public class SemiSerializableChromeTask extends Task {

	public static String varPattern = "[\\w\\W][\\w\\W\\d\\_]*";

	// 初始化参数类型
	public static Map<String, Class> init_map_class;

	// 初始化参数默认值
	public static Map<String, Object> init_map_defaults;

	// URL模板
	public static String url_template;

	// 变量表
	public Map<String, Object> vars;

	// TODO 静态变量赋值
	static {
		// init_map_class
		// init_map_defaults
		// url_template
	}

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
			vars.add(m.group());
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
	 * 创建一个任务
	 * @param init_map
	 * @return
	 */
	public static SemiSerializableChromeTask build(Map<String, Object> init_map, int step) throws Exception {

		staticValidate();

		Map<String, Object> vars = validateInitMap(init_map);

		String url = url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		SemiSerializableChromeTask task = new SemiSerializableChromeTask(url);
		task.setStep(step);

		return task;
	}

	/**
	 *
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public SemiSerializableChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws TaskException.NoMoreStepException
	 */
	public TaskHolder getHolder(Map<String, Object> init_map) throws TaskException.NoMoreStepException {

		if(this.getStep() == 1) {
			throw new TaskException.NoMoreStepException();
		}

		int step = this.getStep() <= 0? 0 : this.getStep() - 1;

		return new TaskHolder(this.getClass().getName(), init_map, step);
	}

}
