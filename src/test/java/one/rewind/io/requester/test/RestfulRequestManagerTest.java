package one.rewind.io.requester.test;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.HttpTaskSubmitter;
import one.rewind.io.requester.RestfulRequester;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.json.JSON;
import org.junit.Test;

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
}
