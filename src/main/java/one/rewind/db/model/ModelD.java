package one.rewind.db.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public abstract class ModelD extends Model{

	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	public String id;
}
