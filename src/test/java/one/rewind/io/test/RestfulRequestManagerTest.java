package one.rewind.io.test;

import one.rewind.io.requester.RestfulRequester;

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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
