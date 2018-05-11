package one.rewind.opencv.test;

import one.rewind.opencv.OpenCVUtil;
import org.junit.Test;
import one.rewind.opencv.OpenCVUtil;

public class OpenCVTest {

	@Test
	public void testGetOffest() {

		String ts = "1525761151984-d0cad68bb1524b33939c7b4c4b63444c";

		String img_1_path = "tmp/geetest/geetest-1-" + ts + ".png";
		String img_2_path = "tmp/geetest/geetest-2-" + ts + ".png";

		int offset = OpenCVUtil.getOffset(img_1_path, img_2_path);

		System.err.println(offset);

	}



}
