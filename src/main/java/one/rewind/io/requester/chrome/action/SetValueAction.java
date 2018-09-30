package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;
import org.openqa.selenium.WebElement;

/**
* 输入框填值
* @author karajan@tfelab.org
* 2017年3月21日 下午8:47:31
*/
public class SetValueAction extends Action {

	public String inputCssPath;
	public String value;
	public long sleepTime = 0;

	public SetValueAction() {};

	public SetValueAction(String inputCssPath, String value) {
		this.inputCssPath = inputCssPath;
		this.value = value;
	}

	public SetValueAction(String inputCssPath, String value, long sleepTime) {
		this.inputCssPath = inputCssPath;
		this.value = value;
		this.sleepTime = sleepTime;
	}

	public boolean run(ChromeAgent agent) {

		try {

			WebElement el = agent.getElementWait(inputCssPath);

			if(el == null) {
				logger.warn("{} not found.", inputCssPath);
				return false;
			}

			el.clear();
			el.sendKeys(value);

			if(sleepTime > 0) {
				agent.getDriver().wait(sleepTime);
			}

			return true;

		} catch (Exception e) {
			logger.error("Set [{}]:[{}] error, ", inputCssPath, value, e);
			return false;
		}
	}
}