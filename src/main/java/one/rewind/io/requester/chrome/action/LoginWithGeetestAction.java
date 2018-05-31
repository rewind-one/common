package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.account.Account;
import one.rewind.json.JSON;

/**
 * 登录脚本 bypass GeeTest
 */
public class LoginWithGeetestAction extends LoginAction {

	GeetestAction action;

	public String geetestContentCssPath = ".geetest_radar_tip";

	public String geetestWindowCssPath = ".geetest_window";
	public String geetestSliderButtonCssPath = ".geetest_slider_button";
	public String geetestSuccessMsgCssPath = ".geetest_success_radar_tip_content";

	public String geetestResetTipCssPath = "#password-captcha-box > div.geetest_holder.geetest_wind.geetest_radar_error > div.geetest_btn > div.geetest_radar_btn > div.geetest_radar_tip > span.geetest_reset_tip_content";
	public String geetestRefreshButtonCssPath = "a.geetest_refresh_1";
	//#password-captcha-box > div.geetest_holder.geetest_wind.geetest_radar_error > div.geetest_btn > div.geetest_radar_btn > div.geetest_radar_tip > span.geetest_radar_error_code
	public String geetestRefreshTooManyErrorCssPath = "span.geetest_radar_error_code";

	public LoginWithGeetestAction() {}

	public LoginWithGeetestAction(Account account) {
		super(account);
	}

	public void run() {

		if(!fillUsernameAndPassword()) return;

		action = new GeetestAction();
		action.geetestContentCssPath = geetestContentCssPath;
		action.geetestWindowCssPath = geetestWindowCssPath;
		action.geetestSliderButtonCssPath = geetestSliderButtonCssPath;
		action.geetestSuccessMsgCssPath = geetestSuccessMsgCssPath;
		action.geetestResetTipCssPath = geetestResetTipCssPath;
		action.geetestRefreshButtonCssPath = geetestRefreshButtonCssPath;
		action.run();

		if(clickSubmitButton()) {
			this.success = true;
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
