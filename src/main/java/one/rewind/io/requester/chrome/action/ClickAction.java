package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
* 点击
* @author karajan@tfelab.org
* 2017年3月21日 下午8:47:18
*/
public class ClickAction extends Action {

	public String elementPath;

	public long sleepTime = 0;

	public ClickAction() {}

	public ClickAction(String elementPath) {
		this.elementPath = elementPath;
	}

	public ClickAction(String elementPath, long sleepTime) {
		this.elementPath = elementPath;
		this.sleepTime = sleepTime;
	}

	public boolean run(ChromeDriverAgent agent) {

		try {

			WebElement el = agent.getDriver().findElement(By.cssSelector(elementPath));

			if (el != null) {

				el.click();

				if (sleepTime > 0L) {
					//agent.getDriver().wait(this.sleepTime);
					Thread.sleep(sleepTime);
				}

				return true;

			} else {
				logger.warn("{} not found.", elementPath);
			}

		} catch (Exception e) {
			logger.error("Exec click action error. ", e);
		}

		return false;
	}
}