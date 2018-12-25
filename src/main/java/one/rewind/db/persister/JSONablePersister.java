package one.rewind.db.persister;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;


public class JSONablePersister extends StringType {

	private static final JSONablePersister INSTANCE = new JSONablePersister();

	private JSONablePersister() {
		super(SqlType.STRING, new Class<?>[] { List.class });
	}

	public static JSONablePersister getSingleton() {
		return INSTANCE;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {

		JSONable obj = (JSONable) javaObject;

		return obj != null ? JSON.toJson(obj) : null;
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {

		Type type = new TypeToken<Map<String, Object>>(){}.getType();

		Object obj = JSON.fromJson((String) sqlArg, fieldType.getType());

		return sqlArg != null ? obj : null;
	}
}