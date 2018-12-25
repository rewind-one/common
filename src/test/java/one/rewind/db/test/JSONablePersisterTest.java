package one.rewind.db.test;

import one.rewind.db.Refacter;
import one.rewind.db.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JSONablePersisterTest {

	TM tm;

	/**
	 * 重建表
	 * @throws Exception
	 */
	@Before
	public void test1() throws Exception {
		Refacter.dropTable(TM.class);
		Refacter.createTable(TM.class);
	}

	/**
	 * 创建对象 存储记录
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {

		tm = new TM("SSSS", 1.2F);
		tm.insert();
	}

	/**
	 * 读取记录 还原对象
	 * @throws Exception
	 */
	@After
	public void test3() throws Exception {

		Model m = Model.getById(TM.class, String.valueOf(tm.id));
		TM tm = (TM) m;
		System.err.println(tm.toJSON());
	}
}
