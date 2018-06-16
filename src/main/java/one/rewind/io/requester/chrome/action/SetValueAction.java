package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import org.openqa.selenium.WebElement;

/**
* 输入框填值
* @author karajan@tfelab.org
* 2017年3月21日 下午8:47:31
*/
public class SetValueAction extends Action {

   public String inputCssPath;
   public String value;

   public SetValueAction() {};

   public SetValueAction(String inputCssPath, String value) {
	   this.inputCssPath = inputCssPath;
	   this.value = value;
   }

   public boolean run(ChromeDriverAgent agent) {

	   try {

		   WebElement el = agent.getElementWait(inputCssPath);

		   if(el == null) {
			   logger.warn("{} not found.", inputCssPath);
			   return false;
		   }

		   el.clear();
		   el.sendKeys(value);
		   return true;

	   } catch (Exception e) {
			logger.error("Set [{}]:[{}] error, ", inputCssPath, value, e);
			return false;
	   }
   }
}