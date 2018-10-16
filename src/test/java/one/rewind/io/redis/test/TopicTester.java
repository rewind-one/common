package one.rewind.io.redis.test;

import one.rewind.db.RedissonAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RTopic;

public class TopicTester {

	private static final Logger logger = LogManager.getLogger(RedissonAdapter.class.getName());

	RTopic<String> topic = RedissonAdapter.redisson.getTopic("topic.test");

	RPatternTopic<String> topic_ = RedissonAdapter.redisson.getPatternTopic("topic.*");

	/**
	 *
	 */
	public TopicTester() {


		topic.addListener((channel, msg) -> {
			logger.info("{} :: {}", channel, msg);
		});

		int listenerId = topic_.addListener((pattern, channel, msg) -> {
			logger.info("{} :: {} -> {}", pattern, channel, msg);
		});
	}

	/**
	 *
	 */
	public void addMsg() {
		topic.publish("Msg1");
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {

		TopicTester tester = new TopicTester();
		tester.addMsg();
	}
}
