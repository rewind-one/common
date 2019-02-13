package one.rewind.io.server;

import net.bytebuddy.implementation.bytecode.Throw;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Msg<T> implements JSONable<T> {

	public static final int SUCCESS = 1;
	public static final int FAILURE = 0;

	public static final int KERNEL_SUCCESS = 11;
	public static final int INSERT_SUCCESS = 111;
	public static final int UPDATE_SUCCESS = 112;
	public static final int DELETE_SUCCESS = 113;

	public static final int KERNEL_NO_RESPONSE = 20;
	public static final int KERNEL_FAILURE = 21;
	public static final int OBJECT_NOT_FOUND = 211;
	public static final int ILLEGAL_REQUEST = 212;
	public static final int ILLEGAL_PARAMETERS = 213;
	public static final int TASK_QUEUE_FULL = 214;
	public static final int INSERT_FAILURE = 221;
	public static final int DELETE_FAILURE = 222;
	public static final int UPDATE_FAILURE = 223;
	public static final int INVALID_PROXY = 23;
	public static final int INVALID_TEMPLATE = 24;
	public static final int EXTERNAL_SERVICE_DOWN = 28;
	public static final int UNKNOWN_ERROR = 299;

	public static final int AUTHORIZE_SUCCESS = 301;
	public static final int AUTHORIZE_FAILURE = 401;
	public static final int URI_NOT_FOUNT = 404;

	public int code;
	public String msg;
	public T data;
	
	private static Map<Integer, String> codes = new HashMap<>();
	static {
		for(Field f: Msg.class.getFields()){
			if(f.getModifiers() == 25 && f.getType() == int.class){
				try {
					codes.put(f.getInt(null), f.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String getCodeString(int code){
		return codes.get(code);
	}

	/**
	 *
	 */
	public Msg(){
		this.code = SUCCESS;
		this.msg = codes.get(this.code);
	}

	/**
	 *
	 * @param data
	 */
	public Msg(T data){
		this.code = SUCCESS;
		this.msg = codes.get(this.code);
		this.data = data;
	}

	/**
	 *
	 * @param e
	 */
	public Msg(Throwable e) {
		this(e, null);
	}

	/**
	 *
	 * @param e
	 * @param data
	 */
	public Msg(Throwable e, T data) {
		this.code = FAILURE;
		this.msg = e.getClass().getSimpleName() + "::" + e.getMessage();
		this.data = data;
	}

	@Deprecated
	public Msg(int code) {
		this.code = code;
		this.msg = codes.get(code);
	}

	@Deprecated
	public Msg(int code, T data) {
		this.code = code;
		this.msg = codes.get(code);
		this.data = data;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
