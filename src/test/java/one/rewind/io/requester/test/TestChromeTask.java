package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.task.ChromeTask;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class TestChromeTask {

	public static class T1 extends ChromeTask {

		static {
			registerBuilder(
					TestChromeTask.T1.class,
					"https://www.baidu.com/s?word={{q}}",
					ImmutableMap.of("q", String.class),
					ImmutableMap.of("q", "ip")
			);
		}

		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public T1(String url) throws MalformedURLException, URISyntaxException {

			super(url);

			this.addDoneCallback((t) -> {

				/*ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t).scheduledTaskId);*/

				System.err.println(t.getResponse().getText().length());
				System.err.println(this.getDomain());

			});
		}
	}

	public static class T2 extends ChromeTask {

		static {
			registerBuilder(
					TestChromeTask.T2.class,
					"https://www.zhihu.com/search?type=content&q={{q}}",
					ImmutableMap.of("q", String.class),
					ImmutableMap.of("q", "ip")
			);
		}

		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public T2(String url) throws MalformedURLException, URISyntaxException {

			super(url);

			this.addDoneCallback((t) -> {

				/*ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t).scheduledTaskId);*/

				System.err.println(t.getResponse().getText().length());
				System.err.println(this.getDomain());

			});
		}
	}
}
