package one.rewind.io.requester.task;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SemiSerializableChromeTask extends Task {

	// 初始化参数类型
	public static Map<String, Class> init_map_class;

	// 初始化参数默认值
	public static Map<String, Object> init_map_defaults;

	// URL模板
	public static String url_template;

	// TODO 静态变量赋值
	static {

	}

	/**
	 *
	 * @throws Exception
	 */
	public static void staticValidate() throws Exception {

		// 验证默认值
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

	}

	public static Task buildTask(Map<String, Object> init_map) {

		return null;
	}

	public static Map<String, Object> validateInitMap(Map<String, Object> init_map) throws Exception {

		Map<String, Object> init = new HashMap<>();

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

		for(String key : init_map_class.keySet()) {
			if(!init.keySet().contains(key)) {
				throw new Exception("init_map error, "+ key +" not defined.");
			}
		}

		return init;
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




}
