package one.rewind.db.persister;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import one.rewind.json.JSON;

import java.util.List;

public class JSONableListPersister extends StringType {

	private static final JSONableListPersister INSTANCE = new JSONableListPersister();

	protected JSONableListPersister() {
		super(SqlType.STRING, new Class<?>[] { List.class });
	}

	public static JSONableListPersister getSingleton() {
		return INSTANCE;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {

		List list = (List) javaObject;

		return list != null ? JSON.toJson(list) : null;
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {

		List list = JSON.fromJson((String)sqlArg, List.class);
		return sqlArg != null ? list : null;
	}
}
