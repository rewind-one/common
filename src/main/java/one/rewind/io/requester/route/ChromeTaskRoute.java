package one.rewind.io.requester.route;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.ScheduledChromeTask;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ChromeTaskRoute {

	private static final Logger logger = LogManager.getLogger(ChromeTaskRoute.class.getName());

	// 执行任务，返回任务分派信息
	public static Route submit = (Request request, Response response) -> {

		try {

			// 任务类名
			String class_name = request.queryParams("class_name");
			Class<? extends ChromeTask> clazz = (Class<? extends ChromeTask>) Class.forName(class_name);

			// 初始参数
			String init_map_str = request.queryParams("init_map");

			ObjectMapper mapper = new ObjectMapper();
			TypeReference<HashMap<String, Object>> typeRef
					= new TypeReference<HashMap<String, Object>>() {};

			Map<String, Object> init_map = mapper.readValue(init_map_str, typeRef);

			// 用户名
			String username = request.queryParams("username");

			// 步骤数
			int step = 0;
			if(request.queryParams("step") != null) {
				step = Integer.valueOf(request.queryParams("step"));
			}

			ChromeTask.Builder builder = ChromeTask.Builders.get(class_name);

			// Create Holder
			ChromeTaskHolder holder = ChromeTask.buildHolder(clazz, init_map, username, step);

			String[] cron = request.queryParamsValues("cron");

			Map<String, Object> info = null;

			// A 周期性任务
			// 加载到Scheduler
			if(cron != null) {
				if (cron.length ==1) {
					ScheduledChromeTask st = new ScheduledChromeTask(holder, cron[0]);
					info = ChromeTaskScheduler.getInstance().schedule(st);
				}
				else if (cron.length > 1) {
					ScheduledChromeTask st =  new ScheduledChromeTask(holder, Arrays.asList(cron));
					info = ChromeTaskScheduler.getInstance().schedule(st);
				}
			}
			// B 单步任务 Submit Holder
			else {
				holder.step = 1;
				info = ChromeDriverDistributor.getInstance().submit(holder);
			}

			// Return info
			return new Msg<Map<String, Object>>(Msg.SUCCESS, info);

		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.FAILURE);
		}
	};

	// 取消已经schedule的任务
	public static Route unschedule = (Request request, Response response) -> {

		try {

			// 任务类名
			String id = request.params(":id");

			ChromeTaskScheduler.getInstance().unschedule(id);

			return new Msg<>(Msg.SUCCESS);
		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};
}
