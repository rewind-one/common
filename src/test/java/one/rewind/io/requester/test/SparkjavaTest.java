package one.rewind.io.requester.test;

import org.junit.Test;

import static spark.Spark.port;

public class SparkjavaTest {

	/**
	 *
	 */
	@Test
	public void test() {
		port(80);
		port(1024);
	}
}
