package one.rewind.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

public abstract class Model implements JSONable, Serializable {

	public static final Logger logger = LogManager.getLogger(Model.class.getName());

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, id = true)
	public String id;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date update_time = new Date();

	public String toJSON() {
		return JSON.toJson(this);
	}

	/**
	 * 插入新记录
	 * @return 是否插入成功
	 * @throws Exception JDBC异常
	 */
	@SuppressWarnings("unchecked")
	public boolean insert() throws Exception{

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 更新记录
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public boolean update() throws Exception{

		Dao dao = DaoManager.getDao(this.getClass());

		this.update_time = new Date();

		if (dao.update(this) == 1) {
			return true;
		}
		return false;
	}

	/**
	 * 删除记录
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public boolean delete() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.deleteById(id) == 1) {
			return true;
		}
		return false;
	}
}
