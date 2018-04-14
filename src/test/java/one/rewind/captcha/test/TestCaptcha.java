package one.rewind.captcha.test;

import one.rewind.captcha.CaptchaHandler;
import org.junit.Test;
import one.rewind.captcha.CaptchaHandler;

public class TestCaptcha {

	@Test
	public void bypass() {
		
		String key = "92506c6fadaa8d4d25c8b338dbea3220";
		String img_fn = "src/test/java/org/tfelab/captcha/pin.png";

		String pass = CaptchaHandler.bypass("src/test/java/org/tfelab/captcha/pin.png");
		System.err.println(pass);

//		System.out.println("Decoding");
//		ApiResult ret = BypassCaptchaApi.Submit(key, img_fn);
//		if (!ret.IsCallOk) {
//			System.out.println("Error: " + ret.Error);
//			return;
//		}
//
//		String value = ret.DecodedValue;
//		System.out.println("Using the decoded value: " + value);
//		System.out.println("Suppose it is correct.");
//		ret = BypassCaptchaApi.SendFeedBack(key, ret, true);
//		if (!ret.IsCallOk) {
//			System.out.println("Error: " + ret.Error);
//			return;
//		}
//
//		ret = BypassCaptchaApi.GetLeft(key);
//		if (!ret.IsCallOk) {
//			System.out.println("Error: " + ret.Error);
//			return;
//		}
//
//		System.out.println("There are " + ret.LeftCredits + " credits left on this key");
//		System.out.println("OK");
	}

}
