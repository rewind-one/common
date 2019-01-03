package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.Refacter;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.txt.StringUtil;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class TemplateTest {

	@Test
	public void testCreateDB() throws Exception {
		Refacter.createTable(Essay.class);
	}

	/**
	 * 对可能出现问题的Field进行验证
	 */
	public Template template() throws Exception {
		Template tpl = new Template(
				1, Builder.of("{{url}}"),
				new Mapper(Essay.class.getName(),
						//path为空,设置defaultString,结果类型为默认
						new Field("id", null)
								.setDefaultString("123123"),
						//path为空,设置defaultString，结果类型自定义
						new Field("platform_id", null, "", Field.Method.CssPath, "Integer")
								.setDefaultString("3"),
						//path为空,设置evalRule,对拼接的字段进行MD5处理
						new Field("media_id", null)
								.setEvalRule("md5(\"{{platform_id}}-{{media_name}}\")"),
						//path选择不出数据时，设置结果值为defaultString
						new Field("media_name", "body > div.content > div > div > div.main-right-side > div.author-view > div > div > div.author-main > h3 > a", Field.Method.CssPath)
								.setDefaultString("界面"),
						//path选择不出数据时，若设置结果不能为null且没有defaultString，抛出异常
						new Field("title", "body > div.content > div > div > div.main-container > div.article-view > div.article-header > h1", Field.Method.CssPath)
								.setNotNullable(),
						/*new Field("title", "body > div.content > div > div > div.main-coarticle-view > div.article-header > h1", Field.Method.CssPath)
								.setNotNullable(),*/
						//查看images结果是否是正确
						new Field("content", "body > div.content > div > div > div.main-container > div.article-view > div.article-main", null, Field.Method.CssPath, "List"),
						new Field("images", "body > div.content > div > div > div.main-container > div.article-view > div.article-main", null, Field.Method.CssPath, "List"),
						//增加冗余Field，检查对入库结果是否构成影响
						new Field("content232", "body > div.content > div > div > div.main-container > div.article-view > div.article-main", Field.Method.CssPath)
				)
		);
		return tpl;
	}

	@Test
	public void testTemplate() throws Exception {

		/* String url = "https://www.jiemian.com/article/2723871.html";//有media*/
		String url = "https://www.jiemian.com/article/2715916.html";//无media

		Template tpl = template();
		TemplateManager.getInstance().add(tpl);

		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("url", url));
		BasicDistributor.getInstance().submit(taskHolder);

		Thread.sleep(5000);
	}

	/**
	 * 检查生成的下一级任务是否正确
	 */
	@Test
	public void testNTS() throws Exception {

		Template tpl1 = new Template(
				1, Builder.of("{{url}}"),
				new Mapper(2, true,
						new Field("uid", "body > header > div > div > nav.nav.pjax-nav > ul > li > div > a", "href")
				)
		);
		Template tpl2 = new Template(
				2, Builder.of("{{uid}}"),
				new Mapper(0,
						new Field("id", "#header-nav > h2 > a")
				)
		);

		String url = "https://www.jiemian.com/article/2715916.html";

		TemplateManager.getInstance().add(tpl1, tpl2);

		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("url", url));
		BasicDistributor.getInstance().submit(taskHolder);

		Thread.sleep(10000);
	}

	@Test
	public void testMD5() {
		String str = "3-界面";
		System.out.println(StringUtil.MD5(str));
	}

	@DBName("raw")
	@DatabaseTable(tableName = "essays")
	public static class Essay {

		@DatabaseField(dataType = DataType.INTEGER, width = 11, index = true)
		public int platform_id;

		@DatabaseField(dataType = DataType.STRING, width = 32)
		public String platform;

		@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
		public String media_id;

		@DatabaseField(dataType = DataType.STRING, width = 32)
		public String media_nick;

		@DatabaseField(dataType = DataType.STRING, width = 32)
		public String media_name;

		@DatabaseField(dataType = DataType.STRING, width = 32)
		public String media_src_id;

		@DatabaseField(dataType = DataType.STRING, width = 32)
		public String src_id;

		@DatabaseField(dataType = DataType.STRING, width = 256)
		public String title;

		@DatabaseField(dataType = DataType.STRING, width = 1024)
		public String meta_content;

		@DatabaseField(dataType = DataType.DATE, index = true)
		public Date pubdate = new Date();

		@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT")
		public List<String> images;

		@DatabaseField(dataType = DataType.STRING, columnDefinition = "MEDIUMTEXT")
		public String content;
	}
}
