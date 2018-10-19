package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.chrome.ChromeTask;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.io.requester.scheduler.ScheduledTask;
import one.rewind.io.requester.task.TaskHolder;
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
					ImmutableMap.of("q", "String"),
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

				/*TaskScheduler.getInstance().degenerate(((ChromeTask) t)._scheduledTaskId);*/

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
					ImmutableMap.of("k", "String"),
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

				/*TaskScheduler.getInstance().degenerate(((ChromeTask) t)._scheduledTaskId);*/

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
					ImmutableMap.of("k", "String"),
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

				ChromeDistributor.getInstance().submit(this.ext(T1.class, ImmutableMap.of("q", "1000")));

			});
		}
	}

	/**
	 *
	 */
	public static class T4 extends ChromeTask {

		public static long MIN_INTERVAL = 60000;

		public static List<String> crons = Arrays.asList("* * * * *", "*/2 * * * *", "*/4 * * * *");

		static {
			registerBuilder(
					TestChromeTask.T4.class,
					"https://www.baidu.com/s?word={{q}}",
					ImmutableMap.of("q", "String"),
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

				if(!ChromeDistributor.getInstance().getScheduler().registered(t.holder.generateScheduledTaskId())) {

					ChromeDistributor.getInstance().schedule(t.holder, crons);

					ChromeDistributor.logger.info("new");
				}
				else {

					ScheduledTask st = ChromeDistributor.getInstance()
							.getScheduler()
							.getScheduledTask(t.holder.generateScheduledTaskId());

					if(System.currentTimeMillis() < DateFormatUtil.parseTime("2018-10-19 18:30:20").getTime()) {

						st.degenerate();
						ChromeDistributor.logger.info("degenerate");

					} else {

						st.stop();
						ChromeDistributor.logger.info("stop");

					}
				}
			});
		}
	}

	/**
	 * 测试翻页
	 */
	public static class T5 extends ChromeTask {

		public static long MIN_INTERVAL = 60000;

		static {
			registerBuilder(
					TestChromeTask.T5.class,
					"https://www.baidu.com/s?word={{q}}&pn={{pn}}",
					ImmutableMap.of("q", "String","pn", "Integer", "max_page", "Integer"),
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

			this.addNextTaskGenerator((t, nths) -> {

				System.err.println(this.getDomain() + "\t" + System.currentTimeMillis() + "\t" + t.getResponse().getText().length());

				int max_page = t.getIntFromVars("max_page");
				int current_page = t.getIntFromVars("pn");


				for(int i=current_page+10; i<=max_page; i=i+10) {

					// max_page 设为0 不用再翻页
					Map<String, Object> init_map = t.newVars(ImmutableMap.of("pn", i, "max_page", 0));

					nths.add(t.ext(init_map));
				}
			});
		}
	}

	static class TF extends ChromeTask {

		static {

			registerBuilder(
					TF.class,
					"http://www.baidu.com/s?wd={{q}}",
					ImmutableMap.of("q", "String"),
					ImmutableMap.of("q", "ip")
			);
		}

		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public TF(String url) throws MalformedURLException, URISyntaxException {
			super(url);

			this.setValidator((a, t) -> {

				//throw new UnreachableBrowserException("Test");
				throw new ProxyException.Failed(a.proxy);
			/*Account account1 = a.accounts.get(t.getDomain());
			throw new AccountException.Failed(account1);*/
				//throw new AccountException.Failed(a.accounts.get(t.getDomain()));

			});

			this.addDoneCallback((t) -> {
				System.err.println(t.getResponse().getText().length());
			});
		}
	}

	static class TPF extends ChromeTask {

		static {

			registerBuilder(
					TPF.class,
					"http://www.baidu.com/s?word={{q}}",
					ImmutableMap.of("q", "String"),
					ImmutableMap.of("q", "ip")
			);
		}
		/**
		 * @param url
		 * @throws MalformedURLException
		 * @throws URISyntaxException
		 */
		public TPF(String url) throws MalformedURLException, URISyntaxException {
			super(url);

			this.setValidator((a, t) -> {
				throw new ProxyException.Failed(a.proxy);
				//throw new ProxyException();

				//throw new AccountException.Failed();
			});

			this.addDoneCallback((t) -> {
				System.err.println(t.getResponse().getText().length());
			});
		}
	}
}
