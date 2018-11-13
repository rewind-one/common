package one.rewind.io.requester.test;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

/**
 * @author scisaga@gmail.com
 * @date 2018/11/13
 */

@DBName(value = "demo")
@DatabaseTable(tableName = "test_models")
public class TestModel extends ModelL {

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String title;

	@DatabaseField(columnDefinition = "MEDIUMTEXT")
	public String content;

}
