package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.Refacter;
import one.rewind.db.model.Model;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RBlockingQueue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class BasicDistributorTest {

	/**
	 * 设置执行器
	 * 定义基本模板
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {

		try {
			Proxy proxy = null;
			// proxy = new ProxyImpl("reid.red", 60103, null, null);
			BasicDistributor.getInstance().addOperator(proxy);

			// 定义模板时 需要手工制定ID
			/*Template tpl_1 = new Template(
					1, Builder.of("https://www.baidu.com/s?wd={{q}}"),
					new Mapper(2, true, new Field("url", "h3 a", "href"))
			);*/

			Template tpl_1 = new Template(
					1, Builder.of("https://www.baidu.com/s?wd={{q}}"),
					new Mapper(3, true, new Field("w", "http://baike\\.baidu\\.com/item/(?<T>\\d+)/\\d+"))
			);

			Template tpl_2 = Template.dummy(2);

			Template tpl_3 = new Template(
					3, Builder.of("https://baike.baidu.com/item/{{w}}"),
					new Mapper(TestDModel.class.getName(),
							new Field("title", "h1", Field.Method.CssPath),
							new Field("content", "div.para", Field.Method.CssPath)
									.addReplacement("<.+?>", "")
									.addReplacement("\r?\n", "")
					)
			);

			Template tpl_4 = new Template(
					4, Builder.of("https://baike.baidu.com/item/{{w}}"),
					new Mapper(TestDModel.class.getName(),
							new Field("title", "h1", Field.Method.CssPath),
							new Field("content", "div.para", Field.Method.CssPath)
									.addReplacement("<.+?>", "")
									.addReplacement("\r?\n", "")
					)
			).setValidator(new Validator().addContain("百度"));

			TemplateManager.getInstance().add(tpl_1, tpl_2, tpl_3, tpl_4);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSerializeTemplate() {
		String json = TemplateManager.getInstance().get(1).toJSON();
		System.err.println(json);
	}

	@Test
	public void simpleTest() throws Exception {

		for(int i=0; i<10; i++) {

			TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("q", String.valueOf(i)));

			BasicDistributor.getInstance().submit(taskHolder);

		}

		Thread.sleep(20000);
	}

	@Test
	public void setupDB() throws Exception {

		Refacter.dropTable(TestModel.class);
		Refacter.dropTable(TestDModel.class);
		Refacter.createTable(TestModel.class);
		Refacter.createTable(TestDModel.class);
	}

	@Test
	public void testBaike() throws Exception{

		for(int i=0; i<1; i++) {

			TaskHolder taskHolder = TemplateManager.getInstance().get(3).at(ImmutableMap.of("w", String.valueOf(i)));

			BasicDistributor.getInstance().submit(taskHolder);

		}

		Thread.sleep(10000);
	}


	@Test
	public void testValidator() throws Exception{

		for(int i=0; i<1; i++) {

			TaskHolder taskHolder = TemplateManager.getInstance().get(4).at(ImmutableMap.of("w", String.valueOf(i)));

			BasicDistributor.getInstance().submit(taskHolder);

		}

		Thread.sleep(10000);
	}

	@Test
	public void testModelL() {
		TestModel tm = new TestModel();
		System.err.println(tm.id);
	}

	@Test
	public void testModelD() throws Exception {
		TestDModel m = new TestDModel();
		m.id = "1111";
		m.title = "title";
		m.content = "aaa";
		m.pubdate = new Date();
		System.err.println(m.pubdate);
		m.insert();
		System.err.println(m.pubdate);
	}

	@Test
	public void testReadModelD() throws Exception {
		RBlockingQueue<TestDModel> queue = RedissonAdapter.redisson.getBlockingQueue("test-queue");
		/*TestDModel m = (TestDModel) Model.getById(TestDModel.class, "1111");
		System.err.println(m.toJSON());
		System.err.println(m.pubdate);
		RBlockingQueue<TestDModel> queue = RedissonAdapter.redisson.getBlockingQueue("test-queue");
		queue.offer(m);*/
		TestDModel m = queue.poll(1000, TimeUnit.MILLISECONDS);
		System.err.println(m.pubdate);
	}



/*	@After
	public void close() throws InterruptedException {
		Thread.sleep(60000);
	}*/
}
