package one.rewind.db.test;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.PooledDataSource;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelL;
import one.rewind.db.util.Refactor;
import one.rewind.json.JSON;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/1
 */
@DBName(value = "demo")
@DatabaseTable(tableName = "mbs")
public class ModelB extends ModelL {

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false, index = true)
	public int num = 0;

	@DatabaseField(columnName = "ma_id", canBeNull = false, foreign = true, foreignAutoRefresh = true)
	public ModelA ma;

	@Test
	public void createTables() throws DBInitException, SQLException {

		Refactor.dropTable(ModelB.class);
		Refactor.dropTable(ModelA.class);

		Refactor.createTable(ModelB.class);
		Refactor.createTable(ModelA.class);

		for(int i=0; i<10; i++) {
			ModelA ma = new ModelA(i);
			ma.insert();

			ModelB mb = new ModelB();
			mb.ma = ma;
			mb.insert();
		}
	}

	@Test
	public void testQuery() throws DBInitException, SQLException, InterruptedException {

		QueryBuilder<ModelB, String> qb = Daos.get(ModelB.class).queryBuilder();
		QueryBuilder<ModelA, String> qa = Daos.get(ModelA.class).queryBuilder();

		qa.setWhere(qa.where().eq("num", 1));
		qb.setWhere(qb.where().eq("num", 0));

		List<ModelB> mbs = qb.join(qa).query();

		mbs.stream().forEach(mb -> {
			System.err.println(JSON.toPrettyJson(mb));
		});
	}

	@Test
	public void testTransactions() {

		try {
			TransactionManager.callInTransaction(
					Daos.get(ModelB.class).getConnectionSource(),
					(Callable<Void>) () -> {

						ModelA ma = new ModelA(1);
						ma.insert();

						ModelB mb = new ModelB();
						mb.ma = ma;
						mb.insert();



						return null;
					}
			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}



	}
}
