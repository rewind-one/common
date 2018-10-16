package one.rewind.io.test;

import one.rewind.io.server.Msg;
import one.rewind.io.server.MsgTransformer;
import org.junit.Test;
import spark.Request;
import spark.Response;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class SparkJavaTest {

	@Test
	public void testOverridePort() throws InterruptedException {


		get("/", (Request request, Response response) -> {
			return new Msg<>(Msg.SUCCESS);
		}, new MsgTransformer());


		port(8001);




		Thread.sleep(100000);

	}
}
