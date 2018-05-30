package one.rewind.opencv.test;

import one.rewind.opencv.OpenCVUtil;
import one.rewind.util.FileUtil;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

public class OpenCVTest {

	@Test
	public void testGetOffest() {

		String ts = "1525436803599-8d0aaa127a634c86b4d58d51367ec3df";

		String img_1_path = "tmp/geetest/geetest-1-" + ts + ".png";
		String img_2_path = "tmp/geetest/geetest-2-" + ts + ".png";

		int offset = OpenCVUtil.getOffset(img_1_path, img_2_path);

		System.err.println(offset);

	}

	@Test
	public void testGetDigits() {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		String path = "tmp/cellphone.png";

		Mat mat = Imgcodecs.imread(path);

		int x_max = mat.cols();

		int x = 0;
		int y = 3;
		int width = 6;
		int height = 9;

		int i = 0;
		while(x + width <= x_max) {

			Rect rectCrop = new Rect(x, y, width, height);

			Mat image_roi = new Mat(mat, rectCrop);

			MatOfByte mob = new MatOfByte();
			Imgcodecs.imencode(".jpg", image_roi, mob);
			byte ba[] = mob.toArray();

			FileUtil.writeBytesToFile(ba, "tmp/" + x + ".jpg");

			i++;
			x = x + 6 + (i / 4);
		}
	}

}
