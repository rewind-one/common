package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

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
			//proxy = new ProxyImpl("reid.red", 60103, null, null);
			BasicDistributor.getInstance().addOperator(proxy);

			System.err.println(Builder.of("https://www.baidu.com/s?wd={{q}}").toJSON());


			// 定义模板时 需要手工制定ID
			Template tpl_1 = new Template(
					1, Builder.of("https://www.baidu.com/s?wd={{q}}"),
					new Mapper(2, new Field("url", "h3 a", "href"))
			);

			Template tpl_2 = Template.dummy(2);

			TemplateManager.getInstance().add(tpl_1, tpl_2);
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
	}

/*	@After
	public void close() throws InterruptedException {
		Thread.sleep(60000);
	}*/
}
