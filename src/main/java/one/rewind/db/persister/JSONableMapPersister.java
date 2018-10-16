package one.rewind.db.persister;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import one.rewind.json.JSON;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JSONableMapPersister extends StringType {

	private static final JSONableMapPersister INSTANCE = new JSONableMapPersister();

	private JSONableMapPersister() {
		super(SqlType.STRING, new Class<?>[] { List.class });
	}

	public static JSONableMapPersister getSingleton() {
		return INSTANCE;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {

		Map<String, Object> map = (Map<String, Object>) javaObject;

		return map != null ? JSON.toJson(map) : null;
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {

		Type type = new TypeToken<Map<String, Object>>(){}.getType();

		Map<String, Object> map = JSON.fromJson((String) sqlArg, type);

		return sqlArg != null ? map : null;
	}
}
