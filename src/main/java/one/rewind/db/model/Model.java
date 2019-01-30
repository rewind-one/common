package one.rewind.db.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.Daos;
import one.rewind.db.callback.ModelCallback;
import one.rewind.db.exception.DBInitException;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 *
 */
public abstract class Model<T extends Model> implements JSONable<T> {

	public static final Logger logger = LogManager.getLogger(Model.class.getName());

	public static ModelCallback createCallback;

	public static ModelCallback updateCallback;

	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE, index = true)
	public Date update_time = new Date();

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws DBInitException, SQLException {

		Dao dao = Daos.get(this.getClass());

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
	public boolean update() throws DBInitException, SQLException {

		Dao dao = Daos.get(this.getClass());

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
	public static <T> T getById(Class<T> clazz, String id) throws DBInitException, SQLException {

		Dao dao = Daos.get(clazz);

		return (T) dao.queryForId(id);
	}

	/**
	 *
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static <T> List<T> getAll(Class<T> clazz) throws DBInitException, SQLException {

		Dao dao = Daos.get(clazz);

		return (List<T>) dao.queryForAll();
	}

	/**
	 *
	 * @param id
	 * @throws Exception
	 */
	public static <T> void deleteById(Class<T> clazz, String id) throws DBInitException, SQLException {
		Dao<T, String> dao = Daos.get(clazz);
		T model = dao.queryForId(id);
		dao.deleteById(id);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
