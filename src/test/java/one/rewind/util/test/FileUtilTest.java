package one.rewind.util.test;

import one.rewind.util.FileUtil;
import org.junit.Test;

public class FileUtilTest {

	@Test
	public void testAppendLine() {
		String line = "abc";
		FileUtil.appendLineToFile(line, "tmp/tmp.txt");
	}
}
