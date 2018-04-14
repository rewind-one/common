package one.rewind.json;

import com.google.gson.*;
import one.rewind.io.requester.account.Account;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.proxy.ProxyWrapper;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

/**
 * JSON 辅助工具类
 */
public class JSON {

	public static GsonBuilder gb = new GsonBuilder()
			.disableHtmlEscaping()
			.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG)
			.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG)
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Exception.class, new ExceptionDeserializer())
			.registerTypeAdapter(ChromeAction.class, new InterfaceAdapter<ChromeAction>())
			.registerTypeAdapter(Account.class, new InterfaceAdapter<Account>())
			.registerTypeAdapter(ProxyWrapper.class, new InterfaceAdapter<ProxyWrapper>());

	public static Gson gson;

	public static Gson _gson;

	static {
		gson = gb.create();
		_gson = gb.setPrettyPrinting().create();
	}

	public static <T> T fromJson(String json, Type type) {
		return gson.fromJson(json, type);
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		return gson.fromJson(json, clazz);
	}

	public static String toJson(Object obj){
		return gson.toJson(obj);
	}

	public static String toPrettyJson(Object obj){
		return _gson.toJson(obj);
	}

	public static class DateDeserializer implements JsonDeserializer<Date> {

	    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        return new Date(json.getAsJsonPrimitive().getAsLong());
	    }
	}

	public static class DateSerializer implements JsonSerializer<Date> {
	    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
	        return new JsonPrimitive(src.getTime());
	    }
	}

	public static class ExceptionDeserializer implements JsonDeserializer<Exception> {
		 
	    public Exception deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        return new Exception(json.getAsJsonPrimitive().getAsString());
	    }
	}

	public static class ExceptionSerializer implements JsonSerializer<Exception> {
	    public JsonElement serialize(Exception src, Type typeOfSrc, JsonSerializationContext context) {
	        return new JsonPrimitive(src.getMessage());
	    }
	}
}