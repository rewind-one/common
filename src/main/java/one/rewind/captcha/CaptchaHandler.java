/**
 *
 */
package one.rewind.captcha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.util.Configs;

import java.io.InputStream;

/**
 * Captcha bypass 主类
 *
 * @author scisaga@gmail.com
 * @version Oct 2, 2014 10:37:44 PM
 */
public class CaptchaHandler {

	private static final Logger logger = LogManager.getLogger(CaptchaHandler.class.getSimpleName());
	private static String key = "";

	static {
		key = Configs.getConfig(CaptchaHandler.class).getString("capchaKey");
	}

	/**
	 * 获取图片文件流中的验证码文本
	 * @param verifyInputStream 输入流
	 * @return
	 */
	public static String bypass(InputStream verifyInputStream) {
		ApiResult ret = BypassCaptchaApi.Submit(key, verifyInputStream);
		if (!ret.IsCallOk) {
			logger.error("Error parsing captcha: " + ret.Error);
		}

		String value = ret.DecodedValue;
		logger.debug("Decoded captcha is: " + value);
		return value;
	}

	/**
	 * 获取图片文件中的验证码文本
	 * @param verifyPath 图片文件地址
	 * @return
	 */
	public static String bypass(String verifyPath) {
		ApiResult ret = BypassCaptchaApi.Submit(key, verifyPath);
		if (!ret.IsCallOk) {
			logger.error("Error parsing captcha: " + ret.Error);
		}

		String value = ret.DecodedValue;
		logger.debug("Decoded captcha is: " + value);
		return value;
	}

	/**
	 * 获取剩余可用credits
	 * @return
	 */
	public static int getLeftCredits() {
		ApiResult ret = BypassCaptchaApi.GetLeft(key);
		if (!ret.IsCallOk) {
			System.out.println("Error: " + ret.Error);
			return -1;
		}
		logger.info("Captcha credits left on this key: " + ret.LeftCredits);
		return Integer.parseInt(ret.LeftCredits);
	}

}
