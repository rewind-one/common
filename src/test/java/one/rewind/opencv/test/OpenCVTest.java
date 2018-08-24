package one.rewind.opencv.test;

import one.rewind.io.requester.account.AccountImpl;
import one.rewind.io.requester.chrome.ChromeDriverAgent;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.io.requester.exception.ChromeDriverException;
import one.rewind.io.requester.task.ChromeTask;
import one.rewind.io.requester.task.Task;
import one.rewind.opencv.OpenCVUtil;
import one.rewind.util.FileUtil;
import org.aspectj.apache.bcel.classfile.Module;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static one.rewind.opencv.OpenCVUtil.ChangeBlock;
import static one.rewind.opencv.OpenCVUtil.getList;

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

		String path = "tmp/1_8.jpg";



		// 灰度模式读取
		Mat mat = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

		List<Integer> h_line_positions = new ArrayList<>();

		for (int w = 0; w < mat.width(); w++) {

			boolean h_line = true;
			for (int h = 0; h < mat.height(); h++) {

				double[] pixel = mat.get(h, w);
				h_line = h_line && (pixel[0] > 205);
			}

			if(h_line) {

				h_line_positions.add(w);

				for(int h=0; h<mat.height(); h++) {

					mat.put(h, w,128);
				}
			}
		}

		/**
		 *
		 */
		for(int i=0; i<h_line_positions.size(); i++) {

			if(i == h_line_positions.size() - 2 && ( mat.width() - h_line_positions.get(i) ) < 3) {
				return;
			}
			else if(i == h_line_positions.size() - 1) {
				return;
			}
			else {

				int x = h_line_positions.get(i) + 1;
				int y = 0;
				int width = h_line_positions.get(i+1) - x;
				int height = mat.height();

				Rect rectCrop = new Rect(x, y, width, height);
				Mat image_roi = new Mat(mat, rectCrop);

				MatOfByte mob = new MatOfByte();
				Imgcodecs.imencode(".jpg", image_roi, mob);
				byte ba[] = mob.toArray();

				FileUtil.writeBytesToFile(ba, "tmp/1_8_" + x + ".jpg");
			}
		}
/*
		MatOfByte mob = new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, mob);
		byte ba[] = mob.toArray();

		FileUtil.writeBytesToFile(ba, "tmp/1_8_.jpg");*/

		/*int x_max = mat.cols();

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
		}*/
	}

	@Test
	public void zbjLoginImgTest() throws MalformedURLException, URISyntaxException, ChromeDriverException.IllegalStatusException, InterruptedException {

		ChromeDriverAgent chromeDriverAgent = new ChromeDriverAgent();
		chromeDriverAgent.start();

		AccountImpl account = new AccountImpl("zbj.com", "17600668061", "gcy116149");

		ChromeTask task = new ChromeTask("https://login.zbj.com/login").addAction(new LoginWithGeetestAction().setAccount(account));

		chromeDriverAgent.submit(task);

		Thread.sleep(10000);

	}

	@Test
	public void ImgTest() throws MalformedURLException, URISyntaxException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		Mat mat_1 = Imgcodecs.imread("tmp/geetest/geetest-1-1525771475985-02db356c486a4f3f893e0f7802069272.png");
		Mat mat_2 = Imgcodecs.imread("tmp/geetest/geetest-2-1524048915299-1dce8b94f0cc4a0d9b807e7be1cd9254.png");
		List<Mat> list = new ArrayList<>();

		for (int i= 1; i<7; i++) {
			list.add(Imgcodecs.imread("tmp/zbj-geetest-bg/"+ i+".png"));
		}

		Imgcodecs.imwrite("tmp/zbj-geetest-bg/test1.png", mat_1);
		Imgcodecs.imwrite("tmp/zbj-geetest-bg/test.png", OpenCVUtil.mostSimilar(list, mat_1));


		System.err.println(OpenCVUtil.areEqual(mat_1, mat_2));

	}

	@Test
	public void imageContrastTest() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat mat_1 = Imgcodecs.imread("tmp/zbj-geetest-bg/t2.png");
		Mat mat_2 = Imgcodecs.imread("tmp/zbj-geetest-bg/t1.png");

		Mat mat = OpenCVUtil.imageContrast(mat_1, mat_2);
		Imgcodecs.imwrite("tmp/zbj-geetest-bg/mat.png", mat);

		List<OpenCVUtil.Coordinate> list_b = getList(mat);

		Mat rMat = ChangeBlock(list_b, mat);


		Imgcodecs.imwrite("tmp/zbj-geetest-bg/mat1.png", rMat);
	}

}
