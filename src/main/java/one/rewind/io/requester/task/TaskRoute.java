package one.rewind.io.requester.task;

import one.rewind.io.server.Msg;
import spark.Request;
import spark.Response;
import spark.Route;

public class TaskRoute {

	public static Route runTask = (Request request, Response response) -> {

		String id = request.params(":id");

		return new Msg<>(Msg.SUCCESS);
	};
}
