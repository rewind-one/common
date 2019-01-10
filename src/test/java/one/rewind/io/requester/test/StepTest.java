package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class StepTest {

	@Test
	public void testSparkPageStep() throws Exception {
		Template tpl = new Template(
				1, Builder.of("{{tpl_pageUrl}}"),
				new Mapper(null, 0, true, "body", Field.Method.CssPath,
						new Field("title", "div > div.m-list > ul > li > a", Field.Method.CssPath),
						new Field("url", "div > div.m-list > ul > li > a", "href", Field.Method.CssPath, "String")
				).setForbidDuplicateContent(),
				new Mapper(null, 1, true, "body", Field.Method.CssPath,
						new Field("tpl_pageUrl", "div.m-pagination > div > span > a", "href", Field.Method.CssPath, "String")
				).setForbidDuplicateContent()
		);

		TemplateManager.getInstance().add(tpl);
		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("tpl_pageUrl", "http://localhost:4567/pages/1"), null, 3);
		BasicDistributor.getInstance().submit(taskHolder);
/*
		for (int i=0; i <10000; i++){

			Thread.sleep(0);

			String url = "https://www.baidu.com/s?wd=" + i;
			TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("tpl_pageUrl", url), null, 3);
			BasicDistributor.getInstance().submit(taskHolder);
		}
*/

		Thread.sleep(5000);
	}

	@Test
	public  void testSrc() throws MalformedURLException, URISyntaxException {
		Task<Task> t = new Task("http://localhost:4567/pages/1");
		 t.flags.add(Task.Flag.PRE_PROC);

		BasicRequester.getInstance().submit(t, 30000);
		if(t.exception != null) {
			t.exception.printStackTrace();
		}

		System.err.println(t.getDuration() + "\n" + t.getResponse().getText());
	}
}
