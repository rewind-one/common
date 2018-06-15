package one.rewind.io.requester.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ChromeTaskRoute {

	private static final Logger logger = LogManager.getLogger(ChromeTaskRoute.class.getName());

	// 执行任务，返回任务分派信息
	public static Route runTask = (Request request, Response response) -> {

		try {

			String class_name = request.params(":class_name");

			// 用户名
			String username = request.params(":username");

			String init_map_str = request.params(":init_map");

			int step = Integer.valueOf(request.params(":step"));

			ObjectMapper mapper = new ObjectMapper();
			TypeReference<HashMap<String, Object>> typeRef
					= new TypeReference<HashMap<String, Object>>() {};

			HashMap<String, Object> init_map = mapper.readValue(init_map_str, typeRef);

			// 获取 domain
			Class<?> threadClazz = Class.forName(class_name);

			Method method = threadClazz.getMethod("domain");

			String domain = (String) method.invoke(null);

			// 优先级
			Task.Priority priority = Task.Priority.valueOf(request.params(":priority"));

			// Create Holder
			ChromeTaskHolder holder = new ChromeTaskHolder(class_name, domain, username, init_map, step, priority);

			Map<String, Object> info = ChromeDriverDistributor.getInstance().submit(holder);

			return new Msg<Map<String, Object>>(Msg.SUCCESS, info);

		} catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};
}
