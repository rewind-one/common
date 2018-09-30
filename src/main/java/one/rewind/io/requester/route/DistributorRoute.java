package one.rewind.io.requester.route;

import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.scheduler.TaskScheduler;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Map;

public class DistributorRoute {

	private static final Logger logger = LogManager.getLogger(ChromeTaskRoute.class.getName());

	// 获取请求器状态信息
	public static Route getInfo = (Request request, Response response) -> {

		try {

			Map<String, Object> info = ChromeDistributor.getInstance().getInfo();

			return new Msg<Map<String, Object>>(Msg.SUCCESS, info);
		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};

	// 获取请求器状态信息
	public static Route getSchedulerInfo = (Request request, Response response) -> {

		try {

			Map<String, ?> info = TaskScheduler.getInstance().getInfo();

			return new Msg<Map<String, ?>>(Msg.SUCCESS, info);
		}
		catch (Exception e) {

			logger.error("Error create/assign task. ", e);
			return new Msg<>(Msg.ILLGEAL_PARAMETERS);
		}
	};
}
