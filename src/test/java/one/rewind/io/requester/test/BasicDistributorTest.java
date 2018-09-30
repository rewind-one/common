package one.rewind.io.requester.test;

import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.task.Task;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class BasicDistributorTest {

	@Before
	public void setup() {
		Proxy proxy = null;
		//proxy = new ProxyImpl("reid.red", 60103, null, null);
		BasicDistributor.getInstance().setProxy(proxy);
	}

	@Test
	public void testFingerprint() throws InterruptedException, MalformedURLException, URISyntaxException {

		for(int i=0; i<10; i++) {
			Task t = new Task("https://www.baidu.com");
			t.param("Search", "1");
			t.addDoneCallback((t_) -> {
				System.err.println(t.getResponse().getSrc().length);
			});
			BasicDistributor.getInstance().submit(t);
		}
	}

	@Test
	public void testNextTask() throws InterruptedException, MalformedURLException, URISyntaxException {

		for(int i=0; i<1; i++) {

			Task t = new Task("https://www.baidu.com/s?wd=" + i);
			t.setBuildDom();
			t.param("wd", i);

			t.addDoneCallback((t_) -> {
				System.err.println(t.getResponse().getSrc().length);
			});
			t.addNextTaskGenerator((t_, nts) -> {
				t.getResponse().getDoc().select("h3 a").forEach(el -> {
					String url = el.attr("href");
					try {

						Task nt = new Task(url);
						nt.addDoneCallback((nt_) -> {
							System.err.println(t.getResponse().getSrc().length);
						});

						nts.add(nt);
					} catch (MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}
				});
			});

			BasicDistributor.getInstance().submit(t);
		}
	}

	@Test
	public void testRecursiveTask() throws InterruptedException, MalformedURLException, URISyntaxException {

		for(int i=0; i<1; i++) {

			Task t = new Task("https://www.baidu.com/s?wd=" + i);
			t.setSwitchProxy();
			t.setBuildDom();
			t.param("wd", i);
			t.setHeaders(BasicDistributor.genHeaders("www.baidu.com"));

			t.addDoneCallback((t_) -> {
				System.err.println(t.getResponse().getSrc().length);
			});
			t.addNextTaskGenerator((t_, nts) -> {

				int j = 0;

				for(Element el : t.getResponse().getDoc().select("h3 a")) {
					String url = el.attr("href");


					int finalJ = j;

					try {

						Task nt = new Task(url);
						nt.setHeaders(BasicDistributor.genHeaders("www.baidu.com"));

						if(j % 2 == 0) {
							nt.setSwitchProxy();
							nt.addDoneCallback((nt_) -> {
								System.err.println("AAA" + finalJ);
							});
						} else {
							nt.addDoneCallback((nt_) -> {
								System.err.println("BBB" + finalJ);
							});
						}


						nts.add(nt);

					} catch (MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}
					j++;
				}
			});

			BasicDistributor.getInstance().submit(t);
		}
	}

	@Test
	public void testMultiLevelTask() throws InterruptedException, MalformedURLException, URISyntaxException {

		for(int i=0; i<2; i++) {

			Task t = new Task("https://www.baidu.com/s?wd=" + i);
			t.setSwitchProxy();
			t.setBuildDom();
			t.param("wd", i);
			t.setHeaders(BasicDistributor.genHeaders("www.baidu.com"));

			int finalI = i;

			t.addNextTaskGenerator((t_, nts) -> {

				for(int j=0; j<2; j++) {

					Task t1 = new Task("https://www.baidu.com/s?wd=" + finalI + String.valueOf(j));
					t1.setBuildDom();
					t1.param("wd", finalI + String.valueOf(j));
					t1.setHeaders(BasicDistributor.genHeaders("www.baidu.com"));

					t1.addNextTaskGenerator((t__, nts_) -> {

						for (Element el : t__.getResponse().getDoc().select("h3 a")) {

							String url = el.attr("href");

							try {

								Task nt = new Task(url);
								nt.setHeaders(BasicDistributor.genHeaders("www.baidu.com"));

								nts_.add(nt);

							} catch (MalformedURLException | URISyntaxException e) {
								e.printStackTrace();
							}

						}
					});

					nts.add(t1);

				}

			});

			BasicDistributor.getInstance().submit(t);
		}
	}

	@Test
	public void testTaskGroup() throws InterruptedException, MalformedURLException, URISyntaxException {

		for(int i=0; i<2; i++) {

			Task t = new Task("https://www.baidu.com/s?wd=" + i);
			t.setBuildDom();
			t.param("wd", i);
			t.setSwitchProxy();

			t.addDoneCallback((t_) -> {
				System.err.println(t.getResponse().getSrc().length);
			});

			int finalI = i;
			t.addNextTaskGenerator((t_, nts) -> {

				for(int j=1; j<10; j++) {

					try {

						Task nt = new Task("https://www.baidu.com/s?wd=" + finalI + String.valueOf(j));
						nt.param("wd", finalI + String.valueOf(j));

						nt.addDoneCallback((nt_) -> {
							System.err.println(t.getResponse().getSrc().length);
						});

						nts.add(nt);
					} catch (MalformedURLException | URISyntaxException e) {
						e.printStackTrace();
					}
				}
			});

			BasicDistributor.getInstance().submit(t);
		}
	}

	@After
	public void close() throws InterruptedException {
		Thread.sleep(60000);
	}
}
