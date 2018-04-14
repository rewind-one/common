package one.rewind.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置文件适配器
 * 单独为每个类进行配置
 * @author scisaga@gmail.com
 * @date 2017.11.12
 */
public class Configs {
	
	private static final Config base = ConfigFactory.load();

	private static Map<String, Config> configs = new HashMap<String,Config>();

	/**
	 * 基于 Class 单独定义每个类的配置
	 * 配置文件在conf/下
	 * 单独配置文件名与类名相同
	 * @param clazz
	 * @return
	 */
	public static Config getConfig(Class<?> clazz) {

		Config config;

		if(configs.get(clazz.getSimpleName()) == null) {

			File file = new File("conf/" + clazz.getSimpleName() + ".conf");


			if(file.exists()){
				config = ConfigFactory.parseFile(file).withFallback(base);


			} else {
				InputStream stream = Configs.class.getClassLoader().getResourceAsStream("conf/" + clazz.getSimpleName() + ".conf");
				config = ConfigFactory.parseReader(new InputStreamReader(stream)).withFallback(base);
			}
		} else {
			config = configs.get(clazz.getSimpleName());
		}

		return config;
	}
}
