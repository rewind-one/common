package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.HttpTaskSubmitter;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.json.JSON;
import org.junit.Test;

/**
 * @author scisaga@gmail.com
 * @date 2018/10/18
 */
public class HttpTaskSubmitterTest {

	@Test
	public void httpSubmit() throws Exception {

		ChromeDistributor.getInstance();

		HttpTaskSubmitter.getInstance().submit(TestChromeTask.T5.class.getName(), JSON.toJson(ImmutableMap.of("q" ,"ip")));

		Thread.sleep(10000000);
	}
}
