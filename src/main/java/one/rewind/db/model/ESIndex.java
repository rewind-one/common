package one.rewind.db.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public abstract class ESIndex extends ModelD {

	public abstract String getIndex();

	public abstract String getType();

	public abstract String getMappingFile();
}
