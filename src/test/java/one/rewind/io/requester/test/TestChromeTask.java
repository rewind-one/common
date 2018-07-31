package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.ScheduledChromeTask;
import one.rewind.json.JSON;
import one.rewind.txt.DateFormatUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestChromeTask {

	public static class T1 extends ChromeTask {

		public static long MIN_INTERVAL = 60000;

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

				/*ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t)._scheduledTaskId);*/

				System.err.println(t.getResponse().getText().length());
				System.err.println(this.getDomain());

			});
		}
	}

	/**
	 *
	 */
	public static class T2 extends ChromeTask {

		static {
			registerBuilder(
					TestChromeTask.T2.class,
					"https://www.zhihu.com/search?type=content&q={{k}}",
					ImmutableMap.of("k", String.class),
					ImmutableMap.of("k", "ip")
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

				/*ChromeTaskScheduler.getInstance().degenerate(((ChromeTask) t)._scheduledTaskId);*/

				System.err.println(t.getResponse().getText().length());
				System.err.println(this.getDomain());

			});
		}
	}

	public static class T3 extends ChromeTask {

		static {
			registerBuilder(
					TestChromeTask.T3.class,
					"https://www.zhihu.com/search?type=content&q={{k}}",
					ImmutableMap.of("k", String.class),
					ImmutableMap.of("k", "ip")
			);
		}

		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public T3(String url) throws MalformedURLException, URISyntaxException {

			super(url);

			this.addDoneCallback((t) -> {

				ChromeDriverDistributor.getInstance().submit(this.getHolder(T1.class, ImmutableMap.of("q", "1000")));

			});
		}
	}

	public static class T4 extends ChromeTask {

		public static long MIN_INTERVAL = 60000;

		public static List<String> crons = Arrays.asList("* * * * *", "*/2 * * * *", "*/4 * * * *");

		static {
			registerBuilder(
					TestChromeTask.T4.class,
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
		public T4(String url) throws MalformedURLException, URISyntaxException {

			super(url);

			this.addDoneCallback((t) -> {

				System.err.println(this.getDomain() + "\t" + System.currentTimeMillis() + "\t" + t.getResponse().getText().length());

				ScheduledChromeTask st = t.getScheduledChromeTask();

				if(st == null) {

					st = new ScheduledChromeTask(t.getHolder(this.init_map), crons);
					st.start();
					//ChromeTaskScheduler.getInstance().schedule(t.getHolder(this.init_map), crons);
				}
				else {
					//
					if(System.currentTimeMillis() < DateFormatUtil.parseTime("2018-07-27 16:35:20").getTime()) {
						st.degenerate();
					} else {
						st.stop();
					}
				}
			});
		}
	}

	public static class T5 extends ChromeTask {

		public static long MIN_INTERVAL = 60000;

		static {
			registerBuilder(
					TestChromeTask.T5.class,
					"https://www.baidu.com/s?word={{q}}&pn={{pn}}",
					ImmutableMap.of("q", String.class,"pn", Integer.class, "max_page", Integer.class),
					ImmutableMap.of("q", "ip", "pn", 0, "max_page", 100)
			);
		}

		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public T5(String url) throws MalformedURLException, URISyntaxException {

			super(url);

			this.addDoneCallback((t) -> {

				System.err.println(this.getDomain() + "\t" + System.currentTimeMillis() + "\t" + t.getResponse().getText().length());

				int max_page = t.getIntFromInitMap("max_page");
				int current_page = t.getIntFromInitMap("pn");

				List<ChromeTaskHolder> holders = new ArrayList<>();

				for(int i=current_page+10; i<=max_page; i=i+10) {

					Map<String, Object> init_map = t.getNewInitMap(ImmutableMap.of("pn", i, "max_page", 0));
					ChromeTaskHolder holder = t.getHolder(t.getClass(), init_map);
					holders.add(holder);
				}

				for(ChromeTaskHolder holder : holders) {

					System.err.println(JSON.toPrettyJson(holder));
					ChromeDriverDistributor.getInstance().submit(holder);
				}

			});
		}
	}
}
