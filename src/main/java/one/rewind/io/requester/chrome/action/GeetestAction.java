package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.io.requester.chrome.RemoteMouseEventSimulator;
import one.rewind.opencv.OpenCVUtil;
import one.rewind.simulator.mouse.MouseEventModeler;
import one.rewind.simulator.mouse.MouseEventSimulator;
import one.rewind.txt.StringUtil;
import one.rewind.util.EnvUtil;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GeeTest bypass
 */
public class GeetestAction extends Action {

	static List<Mat> bg_list = new ArrayList<>();

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (EnvUtil.isHostLinux()) {
			// TODO 从文件中读取 生成bg_list
			for (int i= 1; i<7; i++) {
				bg_list.add(Imgcodecs.imread("data/zbj-geetest-bg-linux/"+ i+".png"));
			}

		} else {
			// TODO 从文件中读取 生成bg_list
			for (int i= 1; i<7; i++) {
				bg_list.add(Imgcodecs.imread("data/zbj-geetest-bg-windows/"+ i+".png"));
			}
		}
	}

	// 点击验证
	public String geetestContentCssPath = ".geetest_radar_tip";

	// 窗口
	public String geetestWindowCssPath = ".geetest_window";

	// 滑块
	public String geetestSliderButtonCssPath = ".geetest_slider_button";

	// 验证通过
	public String geetestSuccessMsgCssPath = ".geetest_success_radar_tip_content";

	// 重置
	public String geetestResetTipCssPath = "#password-captcha-box > div.geetest_holder.geetest_wind.geetest_radar_error > div.geetest_btn > div.geetest_radar_btn > div.geetest_radar_tip > span.geetest_reset_tip_content";

	// 刷新
	public String geetestRefreshButtonCssPath = "a.geetest_refresh_1";
	//#password-captcha-box > div.geetest_holder.geetest_wind.geetest_radar_error > div.geetest_btn > div.geetest_radar_btn > div.geetest_radar_tip > span.geetest_radar_error_code

	// 验证过多
	public String geetestRefreshTooManyErrorCssPath = "span.geetest_radar_error_code";

	transient int geetest_retry_count = 0;

	class ByPassErrorException extends Exception {}

	public GeetestAction() {}

	public int getOffset(ChromeAgent agent) throws IOException, InterruptedException {

		String ts = System.currentTimeMillis() + "-" + StringUtil.uuid();

		String img_1_path = "tmp/geetest/geetest-1-" + ts + ".png";
		String img_2_path = "tmp/geetest/geetest-2-" + ts + ".png";

		// 等待图片加载
		Thread.sleep(5000);
		// 拖拽前截图
		byte[] img_1 = agent.shoot(geetestWindowCssPath);
		//FileUtil.writeBytesToFile(img_1, img_1_path);
		Mat mat_1 = Imgcodecs.imdecode(new MatOfByte(img_1), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

		// 点击滑块，向右拖5px
		// TODO 此方法在未来可能被GeeTest屏蔽
		new Actions(agent.getDriver())
				.dragAndDropBy(agent.getElementWait(geetestSliderButtonCssPath), 5, 0)
				.build().perform();

		// 等待图片加载
		Thread.sleep(5000);

		// 简单拖拽后截图，截图中会包含目标拖拽位置
		byte[] img_2 = agent.shoot(geetestWindowCssPath);
		// FileUtil.writeBytesToFile(img_2, img_2_path);
		Mat mat_2 = Imgcodecs.imdecode(new MatOfByte(img_2), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

		// TODO 判断 mat_1 和 mat_2 是否相同
		if (OpenCVUtil.areEqual(mat_1, mat_2)) {

			logger.info("Choose a img!");

			// TODO 如果相同 从6个备选图片中选择最像的那个  mat_1
			Mat bg_mat = OpenCVUtil.mostSimilar(bg_list, mat_1);

			if (bg_list == null && bg_list.size() < 1) {
				logger.error("The system does not load the data/zbj-geetest-bg-***");
			}

			if (bg_mat == null) {
				logger.info("Don`t get choose img, Retry login!");
				return -1;
			}

			return OpenCVUtil.getOffset(mat_1, bg_mat);

		} else {
			// 生成位移
			return OpenCVUtil.getOffset(mat_1, mat_2);
		}
	}

	/**
	 * 鼠标操作
	 * @param offset 像素差
	 * @param sys_error_x 误差
	 * @throws Exception
	 */
	private void mouseManipulate(ChromeAgent agent, int offset, int sys_error_x) throws Exception {

		if (offset != -1) {
			// TODO
			int x_init = agent.getElementWait(geetestSliderButtonCssPath).getLocation().x
					+ 15 + new Random().nextInt(20);

			int y_init = agent.getElementWait(geetestSliderButtonCssPath).getLocation().y
					+ agent.getDriver().manage().window().getPosition().y + 105 + 10 + new Random().nextInt(20);

		/*Robot bot = new Robot();
		bot.mouseMove(x_init, y_init);*/

			logger.info("x_init:{}, y_init:{}, offset:{}", x_init, y_init, offset);

			// Build Actions
			List<one.rewind.simulator.mouse.Action> actions = MouseEventModeler.getInstance().getActions(x_init, y_init, offset + sys_error_x);

			// 初始化 RemoteMouseEventSimulator
			MouseEventSimulator simulator;
			if (agent.remoteShell != null) {
				logger.info(agent.remoteAddress);
				simulator = new RemoteMouseEventSimulator(actions, agent.remoteShell);
			} else {
				simulator = new MouseEventSimulator(actions);
			}

			// 执行事件
			simulator.procActions();
		}
	}

	/**
	 * 滑块验证
	 * @throws Exception
	 */
	private void bypass(ChromeAgent agent) throws Exception {

		logger.info("Retry:{}", geetest_retry_count);

		// 第一次
		if(geetest_retry_count == 0) {

			Thread.sleep(5000);

			// 点击识别框
			try {
				agent.getElementWait(geetestContentCssPath).click();
			}
			// 如果页面上没有GeeTest识别框
			catch (Exception e) {
				// 并且没有滑块，直接返回
				try {
					agent.getDriver().findElement(By.cssSelector(geetestSliderButtonCssPath));
				}
				catch (Exception ex) {
					logger.warn("Geetest content [{}] not found.", geetestSliderButtonCssPath, ex);
					return;
				}
			}
		}
		// 第 2 - N 次
		else {

			try {
				// 验证DIV未关闭
				WebElement sliderButton = agent.getDriver().findElement(By.cssSelector(geetestSliderButtonCssPath));
				if(sliderButton.isDisplayed()) {
					// 点击刷新按钮
					logger.info("Try to click refresh button.");
					agent.getElementWait(geetestRefreshButtonCssPath).click();
				} else {
					throw new NoSuchElementException("Slider button hidden.");
				}
			}
			// 验证DIV已经关闭
			catch (NoSuchElementException e) {
				// 主页面出现 尝试过多 请点击重试 01
				if(agent.getElementWait(geetestRefreshTooManyErrorCssPath).getText().equals("01")
						|| agent.getElementWait(geetestRefreshTooManyErrorCssPath).getText().equals("12")) {
					// 点击重试连接
					logger.info("Try to click reset link.");
					agent.getElementWait(geetestResetTipCssPath).click();
				}
				else {
					//TODO
					logger.info("Wired Situation...");
				}
			}

		}

		// Thread.sleep(3000);

		// 此时验证DIV已经打开

		mouseManipulate(agent, getOffset(agent), 0);

		geetest_retry_count++;

		try {
			// 识别成功
			agent.getElementWait(geetestSuccessMsgCssPath);
			geetest_retry_count = 0;
		} catch (org.openqa.selenium.TimeoutException e) {
			// 重试
			if(geetest_retry_count < 100) {
				bypass(agent);
			} else {
				throw new ByPassErrorException();
			}
		}
	}

	public boolean run(ChromeAgent agent) {

		try {
			bypass(agent);
			return true;
		} catch (Exception e) {
			geetest_retry_count = 0;
			logger.error("GeeTest bypass error, ", e);
			return false;
		}
	}

}
