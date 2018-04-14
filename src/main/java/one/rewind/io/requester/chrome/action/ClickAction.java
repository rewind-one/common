package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import one.rewind.json.JSON;

import static one.rewind.io.requester.chrome.ChromeDriverAgent.logger;

/**
* 点击
* @author karajan@tfelab.org
* 2017年3月21日 下午8:47:18
*/
public class ClickAction extends ChromeAction {

	public String elementPath;

	public ClickAction() {}

	public ClickAction(String elementPath) {
		this.elementPath = elementPath;
	}

	public void run() {

		try {

			WebElement el = agent.getDriver().findElement(By.cssSelector(elementPath));

			if (el != null) {

				el.click();
				success = true;

			} else {

				logger.warn("{} not found.", elementPath);
			}

		} catch (Exception e) {
			logger.error("Exec click action error. ", e);
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}