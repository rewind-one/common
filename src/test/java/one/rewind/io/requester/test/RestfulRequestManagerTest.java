package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.HttpTaskSubmitter;
import one.rewind.io.requester.RestfulRequester;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RestfulRequestManagerTest {
	
	public static void main(String[] args){
		
		try {
			
			RestfulRequester.getInstance().updateUidAndPrivateKey("abc","abc");
			Map q = new HashMap<String, Object>();
			q.put("user", "user");
			String src = RestfulRequester.getInstance().request("http://tetra-data.com:59020/echo", RestfulRequester.RequestType.GET, "");
			System.err.println(src);

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test
	public void httpSubmit() throws Exception {

		ChromeDriverDistributor.getInstance();

		//HttpTaskPoster.getInstance().submit(TestChromeTask.T5.class.getName(), JSON.toJson(ImmutableMap.of("q" ,"ip")));

		HttpTaskSubmitter.getInstance().submit(TestChromeTask.T5.class, ImmutableMap.of("q" ,"ip"));

		Thread.sleep(10000000);
	}

}
