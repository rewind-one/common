package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.json.JSON;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * 鼠标悬浮
 */
public class MouseHoverAction extends Action {

	public String elementCssPath;

	public MouseHoverAction(){}

	public MouseHoverAction(String elementCssPath){

		this.elementCssPath = elementCssPath;
	}

	public boolean run( ChromeAgent agent ) {
		// 获取 WebDriver
		WebDriver driver = agent.getDriver();

		// 获取悬浮元素
		WebElement el = driver.findElement(By.cssSelector(this.elementCssPath));

		// 悬浮操作
		Actions builder = new Actions(driver);
		builder.moveToElement(el).build().perform();

		// 随机延时
		try {
			int randomNumber1 = (int)(Math.random() * 2 + 2);
			Thread.sleep(randomNumber1 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}

	public String toJSON() {
		return JSON.toJson(this);
	}

}
