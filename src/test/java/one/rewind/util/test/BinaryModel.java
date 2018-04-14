package one.rewind.util.test;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.util.FileUtil;
import one.rewind.db.DBName;
import one.rewind.db.OrmLiteDaoManager;
import one.rewind.db.Refacter;
import one.rewind.util.FileUtil;

import java.util.UUID;

@DatabaseTable(tableName = "bm")
@DBName(value = "test")
public class BinaryModel {
	
	@DatabaseField(id = true, dataType = DataType.STRING, width = 36, canBeNull = false)
	public String id;

	@DatabaseField(dataType = DataType.BYTE_ARRAY, columnDefinition = "LONGBLOB")
	public byte[] c;

	public static void main(String[] args) {
		
		try {
			
			Refacter.dropTable(BinaryModel.class);
			Refacter.createTable(BinaryModel.class);
			
			String id = UUID.randomUUID().toString();
			
			Dao<BinaryModel, String> dao = OrmLiteDaoManager.getDao(BinaryModel.class);
			BinaryModel bm = new BinaryModel();
			bm.id = id;
			bm.c = FileUtil.readBytesFromFile("D:\\test.jpg");
			dao.create(bm);
			
			BinaryModel bm2 = dao.queryForId(id);
			FileUtil.writeBytesToFile(bm2.c, "D:\\test1.jpg");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
