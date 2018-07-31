package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeDriverAgent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * 模拟点击评论评论，并加载更多
 *
 * @author zhangseng@315free.com
 * @data 2018/5/30
 */
public class LoadMoreContentAction extends Action {

   	String morePath ;

	public LoadMoreContentAction(String morePath) {
		this.morePath = morePath;
	}

    public boolean run(ChromeDriverAgent agent) {

	    //循环验证点击加载更多，加载全部页面
	    int clickCount = 0;

	    WebElement clc;

	    do {
		    try {
			    Thread.sleep(1000);
			    clc = agent.getDriver().findElement(By.cssSelector(morePath));

			    if( clc == null){
				    break;
			    }
			    clc.click();
			    clickCount ++;
		    }
		    catch (Exception e){

		    	if(clickCount > 0 && e instanceof org.openqa.selenium.NoSuchElementException) {

			    } else {
				    logger.error("Error find morePath", e);
			    }

			    break;
		    }
	    } while ( clc != null );

	    clickCount = 0;
	    return true;
    }
}
