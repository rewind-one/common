package one.rewind.db.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public abstract class ModelL extends Model{

	@DatabaseField(dataType = DataType.INTEGER, index = true, generatedId = true)
	public int id;
}
