package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.account.Account;
import one.rewind.json.JSON;

/**
 * 登录脚本 bypass GeeTest
 */
public class LoginWithGeetestAction extends LoginAction {

	GeetestAction action;

	public LoginWithGeetestAction() {}

	public LoginWithGeetestAction(Account account) {
		super(account);
	}

	public void run() {

		if(!fillUsernameAndPassword()) return;

		action = new GeetestAction();
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
