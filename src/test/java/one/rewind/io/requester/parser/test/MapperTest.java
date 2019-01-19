package one.rewind.io.requester.parser.test;

import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ESIndex;
import one.rewind.db.model.Model;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.parser.*;
import one.rewind.io.requester.task.TaskHolder;
import org.bouncycastle.math.raw.Mod;
import org.junit.Test;

import java.util.List;

public class MapperTest {

	//caijing01-tpl_medias
	@Test
	public void testOriginUrl() throws Exception {

		int platformId = 1;
		String platform_shortName = "caijing01";

		Template tpl_medias = new Template(
				1, Builder.of("http://www.01caijing.com/personal/platformindex/getuser.json?userId={{tpl_medias}}"),
				new Mapper(MediaModelTest.class.getName(),
						new Field("id", null)
								.setEvalRule("md5(\"{{platform}}-{{nick}}\")"),
						new Field("platform_id", null, null, Field.Method.CssPath, Field.Type.Integer)
								.setDefaultString(String.valueOf(platformId)),
						new Field("platform", null)
								.setDefaultString(platform_shortName),

						new Field("avatar", "\"headPath\":\"(?<T>.+?)\",\"city\"")
								.addReplacement("^","https://file.01caijing.com/"),
						new Field("src_id", "\"sdfsdfuserid\":\"(?<T>.+?)\",\""),
						new Field("name", "\"userid\":\"(?<T>.+?)\",\"")
								.setDefaultString(platform_shortName),
						new Field("nick", "\"nickname\":\"(?<T>.+?)\",\"")
								.setDefaultString(platform_shortName),
						new Field("origin_url", "\"signature\":\"(?<T>.+?)\",\"commentNum\""),
						new Field("content", "\"signature\":\"(?<T>.+?)\",\"commentNum\""),
						new Field("fans_count", "\"fansNum\":(?<T>.+?),\"prestige", null, Field.Method.Reg, Field.Type.Integer)
				)
		);

		TemplateManager.getInstance().add(tpl_medias);

//		String url = "10004031";
		String url = "10014471";
		TaskHolder taskHolder = TemplateManager.getInstance().get(1).at(ImmutableMap.of("tpl_medias",url));

		BasicDistributor.getInstance().submit(taskHolder);
		Thread.sleep(10000);
	}
}
