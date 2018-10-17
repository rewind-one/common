package one.rewind.io.requester.test;

import one.rewind.util.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsoupTest {

	@Test
	public void testBuildDom() {

		Document doc = Jsoup.parse(FileUtil.readFileByLines("tmp/screen_dump.xml"));

		int h = 0;
		// 找到消息列表框架
		Elements list_views = doc.getElementsByAttributeValue("class", "android.widget.ListView");

		for(Element el : list_views) {

			if(el.attr("bounds").matches("\\[0,.+?\\[1440,.+?")) {

				Pattern pattern = Pattern.compile("(?<h1>\\d+)\\]\\[\\d+,(?<h2>\\d+)\\]");
				Matcher m = pattern.matcher(el.attr("bounds"));

				if (m.find()) {
					h = Integer.valueOf(m.group("h2")) - Integer.valueOf(m.group("h1"));
				}
			}
		}


		Elements els = doc.getElementsByAttributeValue("class", "android.widget.RelativeLayout");

		for(Element el : els) {

			String bounds = el.attr("bounds");

			// 找到一行消息
			if(bounds.matches("\\[0,.+?\\[1440,.+?")) {

				System.err.println(bounds);

				Elements imageEls = el.getElementsByAttributeValue("class", "android.widget.ImageView");
				Elements viewEls = el.getElementsByAttributeValue("class", "android.view.View");

				// 图片数量
				// 0 文本消息 头像未展示 应该第一个消息 或最后一个消息 由于特殊的显示位置不能有效显示
				// 2 文本消息 头像在界面上有展示 不一定展示全 需要拿 第一个ImageView的bounds 解析宽度高度 判定是否完整显示
				// 4 图片类型消息

				// 如果是最后一个消息，图片数量为2或4 需要判断内容是否展示全
				//

				System.err.println("images " + imageEls.size() + " views " + viewEls.size());
			}

		}
	}
}
