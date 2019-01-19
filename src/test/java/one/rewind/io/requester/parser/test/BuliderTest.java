package one.rewind.io.requester.parser.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.junit.Test;

public class BuliderTest {

	@Test
	public void testBuilder1() throws Exception {
		Template tpl = new Template(
				1, Builder.of(Task.Flag.BUILD_DOM),
				new Mapper(null, 0, true, "body", Field.Method.CssPath,
						new Field("title", "div > div.m-list > ul > li > a", Field.Method.CssPath),
						new Field("url", "div > div.m-list > ul > li > a", "href", Field.Method.CssPath, Field.Type.String)
				)
		);

		TemplateManager.getInstance().add(tpl);

		String url = "http://localhost:4567/pages/1";
		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("{{url}}", url, "{{post_data}}", "sdfsd"));

		BasicDistributor.getInstance().submit(taskHolder);
		Thread.sleep(5000);
		System.err.println(taskHolder.build());
	}

	@Test
	public void testBuilder2() throws Exception {
		Template tpl = new Template(
				1, Builder.of("{{url}}"),
				new Mapper(null, 0, true, "body", Field.Method.CssPath,
						new Field("title", "div > div.m-list > ul > li > a", Field.Method.CssPath),
						new Field("url", "div > div.m-list > ul > li > a", "href", Field.Method.CssPath, Field.Type.String)
				)
		);

		TemplateManager.getInstance().add(tpl);

		String url = "http://localhost:4567/pages/1";
		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("{{url}}", url));

		BasicDistributor.getInstance().submit(taskHolder);
		Thread.sleep(5000);
		System.err.println(taskHolder.build());
	}
}
