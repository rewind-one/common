package one.rewind.db.model;

import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.DaoManager;
import one.rewind.db.ModelCreateCallback;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class Model implements JSONable<Model> {

	static final Logger logger = LogManager.getLogger(Model.class.getName());

	public static ModelCreateCallback createCallback;

	public static ModelCreateCallback updateCallback;

	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE, index = true)
	public Date update_time = new Date();

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.create(this) == 1) {

			if(createCallback != null) createCallback.run(this);

			return true;
		}

		return false;
	}

	/**
	 * 更新数据
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		this.update_time = new Date();

		if (dao.update(this) == 1) {

			if(updateCallback != null) updateCallback.run(this);
			return true;
		}

		return false;
	}

	/**
	 *
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static Model getById(Class clazz, String id) throws Exception {

		Dao dao = DaoManager.getDao(clazz);

		return (Model) dao.queryForId(id);
	}

	/**
	 *
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static List<Model> getAll(Class clazz) throws Exception {

		Dao dao = DaoManager.getDao(clazz);

		return (List<Model>) dao.queryForAll();
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
