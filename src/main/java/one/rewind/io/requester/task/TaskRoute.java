package one.rewind.io.requester.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.rewind.io.requester.chrome.ChromeDriverRequester;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TaskRoute {

	private static final Logger logger = LogManager.getLogger(TaskRoute.class.getName());

	// 执行任务，返回任务分派信息
	public static Route runTask = (Request request, Response response) -> {

		try {

			String class_name = request.params(":class_name");
			String init_map_str = request.params(":init_map");
			int step = Integer.valueOf(request.params(":step"));

			ObjectMapper mapper = new ObjectMapper();
			TypeReference<HashMap<String, Object>> typeRef
					= new TypeReference<HashMap<String, Object>>() {};

			HashMap<String, Object> init_map = mapper.readValue(init_map_str, typeRef);

			TaskHolder holder = new TaskHolder(class_name, init_map, step);

			Task task = holder.build();

			Map<String, Object> info = ChromeDriverRequester.getInstance().submit(task);

			return new Msg<Map<String, Object>>(Msg.SUCCESS, info);

		} catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};
}
