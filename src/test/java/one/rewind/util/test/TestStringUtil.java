/**
 * 
 */
package one.rewind.util.test;

import one.rewind.txt.StringUtil;
import org.junit.Test;
import one.rewind.txt.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestStringUtil {
	
	@Test
	public void testUnicodeConverter() {
		
	}

	@Test
	public void testSplitFirstChar() {
		String string = "af=fanwek.igfv=faoewigm";
		String spliter = "=";

		String[] kv = StringUtil.splitFirstChar(string, spliter);
		assertTrue(kv != null);
		assertEquals(kv.length, 2);
		assertEquals(kv[0], "af");
		assertEquals(kv[1], "fanwek.igfv=faoewigm");

		spliter = "fan";
		kv = StringUtil.splitFirstChar(string, spliter);
		assertTrue(kv != null);
		assertEquals(kv.length, 2);
		assertEquals(kv[0], "af=");
		assertEquals(kv[1], "wek.igfv=faoewigm");

	}

	@Test
	public void pattern() {

		String s= "http://shop.zbj.com/evaluation/evallist-uid-0000000-category-1-isLazyload-0-page-11111.html";

		Pattern pattern = Pattern.compile("http://shop.zbj.com/evaluation/evallist-uid-(?<userId>.+?)-category-1-isLazyload-0-page-(?<page>.+?).html");

		Matcher matcher = pattern.matcher(s);

		while (matcher.find()) {
			System.err.println(matcher.group("userId"));
			System.err.println(matcher.group("page"));
		}
	}

}
