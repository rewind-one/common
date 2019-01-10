package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.task.TaskHolder;
import org.junit.Test;

public class AutoPageTest {

	@Test
	public void testNextPage() throws Exception {

		Template tpl = new Template(
				1, Builder.of("{{tpl_pageUrl}}"),
				new Mapper(null, 0, true, "body", Field.Method.CssPath,
						new Field("title", "div > div.m-list > ul > li > a", Field.Method.CssPath),
						new Field("url", "div > div.m-list > ul > li > a", "href", Field.Method.CssPath, "String")
				),
				new Mapper(null, 1, true, "body", Field.Method.CssPath,
						new Field("tpl_pageUrl", "div.m-pagination > div > span > a", "href", Field.Method.CssPath, "String")
				)
				/*.setForbidDuplicateContent()*/
		);

		TemplateManager.getInstance().add(tpl);

		String url = "http://localhost:4567/pages/1";
		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("tpl_pageUrl", url));

		BasicDistributor.getInstance().submit(taskHolder);
		Thread.sleep(10000);
	}
}
