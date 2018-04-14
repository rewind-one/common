package one.rewind.io.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class JsoupTest {

	@Test
	public void testBuildDom() {
		Document doc = Jsoup.parse("<html></html>");
		System.err.println(doc.body());
	}
}
