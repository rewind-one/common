package one.rewind.io.sparkjava.test;

import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.io.server.Msg;
import one.rewind.io.server.MsgTransformer;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static spark.Spark.before;
import static spark.Spark.post;
import static spark.Spark.port;

public class SparkJavaTest {

	@Before
	public void setupServer() {
		port(80);
		postRouteTest();
	}

	public void postRouteTest() {

		before("/*", (req, res) -> {
			req.session(true);
			req.session().attribute("user_id", 1234567);
		});

		post("/1111", (req, res) -> {

			String s = req.body();
			System.err.println(s);

			System.err.println(req.pathInfo());

			System.err.println((int) 1000L);

			int user_id = req.session().attribute("user_id");

			System.err.println(user_id);

			return new Msg<>();

		}, new MsgTransformer());
	}

	@Test
	public void testPost() throws MalformedURLException, URISyntaxException {

		HashMap<String, String> headers = new HashMap();
		headers.put("A", "A");

		Task task = new Task("http://127.0.0.1/1111", headers, "{\"a\":\"a\"}", null, null);
		task.setPost();
		BasicRequester.getInstance().submit(task);
	}
}
