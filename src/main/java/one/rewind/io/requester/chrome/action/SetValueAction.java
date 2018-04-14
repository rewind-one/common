package one.rewind.io.requester.chrome.action;

import one.rewind.json.JSON;
import org.openqa.selenium.WebElement;
import one.rewind.json.JSON;

/**
* 输入框填值
* @author karajan@tfelab.org
* 2017年3月21日 下午8:47:31
*/
public class SetValueAction extends ChromeAction {

   public String inputCssPath;
   public String value;

   public SetValueAction() {};

   public SetValueAction(String inputCssPath, String value) {
	   this.inputCssPath = inputCssPath;
	   this.value = value;
   }

   public void run() {

	   try {

		   WebElement el = agent.getElementWait(inputCssPath);

		   if(el == null) {
			   logger.warn("{} not found.", inputCssPath);
			   return;
		   }

		   el.clear();
		   el.sendKeys(value);
		   success = false;

	   } catch (Exception e) {
			logger.error(e);
	   }
   }

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}