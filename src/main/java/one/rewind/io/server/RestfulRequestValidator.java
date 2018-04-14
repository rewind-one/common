package one.rewind.io.server;

import one.rewind.io.requester.RestfulRequester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.io.requester.RestfulRequester;
import spark.Request;

import java.util.Map;

import static spark.Spark.halt;

/**
 * Restful请求简单验证机制
 * @author scisaga@gmail.com
 */
public class RestfulRequestValidator {
	
	private static final Logger logger = LogManager.getLogger(RestfulRequestValidator.class.getSimpleName());
	
	public static void validate(Request request, Map<String, ? extends User> users) {
		
		/*
		 * 用户验证
		 */
		String _u = request.queryParams("_u");
		if(_u == null) halt(401, "Unauthorized");

		User u = users.get(_u);
		if(u == null) halt(401, "Unauthorized");
		if(!u.isValid() || !u.isEnabled()) halt(401, "Unauthorized");
		
		/*
		 * 时间戳验证
		 */
		if(request.queryParams("_t") == null){
			halt(401, "Bad Request");
		}
		
		long _t = 0;
		long t = System.currentTimeMillis();
		try {
			_t = Long.valueOf(request.queryParams("_t"));
		} catch (Exception e){
			halt(401, "Bad Request");
		}
		
		if(Math.abs(t - _t) > 15 * 1000) {
			halt(400, "Bad Request");
		}
		
		/*
		 * 哈希验证
		 */
		String _h = request.queryParams("_h");
		
		if(_h == null){
			halt(401, "Bad Request");
		}
		
		String _q = null;
		_q = request.queryParams("_q");
		if(_q == null){
			_q = "";
		}
		
		String __h = null;
		try {
			__h = RestfulRequester.encode(_q, _t, u.getPrivateKey());
		} catch (Exception e){
			halt(500, "Internal Error");
		}
		
		if(! __h.equals(_h)){
			halt(403, "Forbidden");
		}
	}
}
