package one.rewind.io.requester.chrome.test;

import one.rewind.io.requester.chrome.action.LoginAction;
import one.rewind.io.requester.chrome.action.LoginWithGeetestAction;
import one.rewind.json.JSON;
import org.junit.Test;

public class LoginActionTest {

	@Test
	public void testSerializeAction() {

		LoginWithGeetestAction action = new LoginWithGeetestAction();

		String json = action.toJSON();

		LoginAction a1 = JSON.fromJson(json, LoginAction.class);

		System.err.println(a1.getClass().getSimpleName());

		if(a1.className != null && a1.className.equals(LoginWithGeetestAction.class.getName())) {
			a1 = JSON.fromJson(json, LoginWithGeetestAction.class);
		}

		System.err.println(a1.getClass().getSimpleName());
	}

}
