/**
 * 
 */
package one.rewind.util.test;

import one.rewind.txt.StringUtil;
import org.junit.Test;
import one.rewind.txt.StringUtil;

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

}
