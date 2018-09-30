package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.chrome.ChromeAgent;
import org.openqa.selenium.WebElement;

/**
 * 登录脚本
 */
public class LoginAction extends Action {

	public ChromeAgent agent;

	public String className;

	//public String url = "https://login.zbj.com/login";

	public String usernameCssPath = "#username";
	public String passwordCssPath = "#password";
	public String loginButtonCssPath = "#login > div.j-login-by.login-by-username.login-by-active > div.zbj-form-item.login-form-button > button";

	public String errorMsgReg = "账号或密码错误";

	public transient boolean success = false;
	public transient Account account;

	public LoginAction() {}

	public LoginAction setAccount(Account account) {
		this.account = account;
		return this;
	}

	public Account getAccount() {
		return this.account;
	}

	/**
	 *
	 * @return
	 */
	boolean fillUsernameAndPassword() {

		try {

			/*agent.getUrl(url);

			agent.waitPageLoad(url);*/

			// 输入账号
			WebElement usernameInput = agent.getElementWait(usernameCssPath);
			usernameInput.clear();
			usernameInput.sendKeys(account.getUsername());

			// 输入密码
			WebElement passwordInput = agent.getElementWait(passwordCssPath);
			passwordInput.sendKeys(account.getPassword());

			Thread.sleep(1000);
			return true;

		} catch (Exception e) {

			logger.error(e);
			return false;
		}
	}

	boolean clickSubmitButton() {

		try {
			// 点击登录框
			agent.getElementWait(loginButtonCssPath).click();

			Thread.sleep(5000);

		} catch (Exception e) {
			logger.error(e);
			return false;
		}

		if (agent.getDriver().getPageSource().matches(errorMsgReg)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean run(ChromeAgent agent) {

		this.agent = agent;

		if(!fillUsernameAndPassword()) return false;

		if(clickSubmitButton()) {
			return true;
		}

		return false;
	}
}