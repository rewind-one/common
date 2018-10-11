package one.rewind.io.requester.route;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.chrome.ChromeTask;
import one.rewind.io.requester.scheduler.ScheduledTask;
import one.rewind.io.requester.scheduler.TaskScheduler;
import one.rewind.io.requester.task.*;
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

	public interface Filter {
		void run(String class_name, String username) throws Exception;
	}

	public static Filter filter;

	public static void setFilter(Filter filter) {
		ChromeTaskRoute.filter = filter;
	}

	// 执行任务，返回任务分派信息
	public static Route submit = (Request request, Response response) -> {

		try {

			// 任务类名
			String class_name = request.queryParams("class_name");
			Class<? extends ChromeTask> clazz = (Class<? extends ChromeTask>) Class.forName(class_name);

			// 初始参数
			String init_map_str = request.queryParams("vars");

			ObjectMapper mapper = new ObjectMapper();
			TypeReference<HashMap<String, Object>> typeRef
					= new TypeReference<HashMap<String, Object>>() {};

			Map<String, Object> init_map = mapper.readValue(init_map_str, typeRef);

			// 用户名
			String username = request.queryParams("username");

			if(filter != null)
				filter.run(class_name, username);

			// 步骤数
			int step = 0;
			if(request.queryParams("step") != null) {
				step = Integer.valueOf(request.queryParams("step"));
			}

			// Create Holder
			TaskHolder holder = ChromeTaskFactory.getInstance().newHolder(clazz, init_map, username, step);

			String[] cron = request.queryParamsValues("cron");

			Map<String, Object> info = null;

			// A 周期性任务
			// 加载到Scheduler
			if(cron != null) {
				if (cron.length ==1) {
					ScheduledTask st = new ScheduledTask(holder, cron[0]);
					info = TaskScheduler.getInstance().schedule(st);
				}
				else if (cron.length > 1) {
					ScheduledTask st =  new ScheduledTask(holder, Arrays.asList(cron));
					info = TaskScheduler.getInstance().schedule(st);
				}
			}
			// B 单步任务 Submit Holder
			else {
				holder.step = 1;
				info = ChromeDistributor.getInstance().submit(holder);

			}

			// Return holder
			return new Msg<Map<String, Object>>(Msg.SUCCESS, info);

		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<String>(Msg.FAILURE, e.getMessage());
		}
	};

	// 取消已经schedule的任务
	public static Route unschedule = (Request request, Response response) -> {

		try {

			// 任务类名
			String id = request.params(":id");

			TaskScheduler.getInstance().unschedule(id);

			return new Msg<>(Msg.SUCCESS);
		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};
}
