package one.rewind.txt.test;

import one.rewind.txt.StringUtil;
import org.junit.Test;
import one.rewind.txt.StringUtil;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {

	@Test
	public void test() {
		
		System.out.println("www.cdcd.com/ddsdf/dfsdf".matches(".+?://.+?"));
		String s1 = "测试测试测试";
		String s2 = StringUtil.removeCtrlChars("测试\u001f测试\u007f测试");
		assertEquals(s1, s2);

		String s3 = "a|b\"|\'()";
		String s4 = "a\\|b\"\\|\'()";
		System.out.println(StringUtil.removeCtrlChars(s3));
		//assertEquals(s4, JsonHelper.removeCtrlChars(s3));
		
		System.out.println(":".matches(":"));
	}
}
