package one.rewind.db.test;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.Model;
import one.rewind.db.model.ModelL;

import java.util.UUID;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/1
 */
@DBName(value = "demo")
@DatabaseTable(tableName = "mas")
public class ModelA extends ModelL {

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int num = 0;

	@DatabaseField(dataType = DataType.STRING, width = 36, canBeNull = false, index = true)
	public String text;

	public ModelA() {}

	public ModelA(int num) {
		this.num = num;
		this.text = UUID.randomUUID().toString();
	}

}
