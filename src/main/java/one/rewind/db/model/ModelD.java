package one.rewind.db.model;

import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.DaoManager;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * 只有ModelD 存在版本管理概念
 */
public abstract class ModelD extends Model{

	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	public String id;

	public boolean upsert() {

		try {

			Dao dao = DaoManager.getDao(this.getClass());

			ModelD oldVersion = (ModelD) dao.queryForId(this.id);

			// 没有旧版本
			if(oldVersion == null) {

				if(super.insert()) {
					createSnapshot(this); // 第一次采集 也需要创建快照
					return true;
				}
			}
			// 存在旧版本
			else {

				if(diff(oldVersion)) {

					createSnapshot(oldVersion); // 创建快照
					oldVersion.copy(this); // 新值覆盖旧值
					return oldVersion.update();
				}
			}

			return false;
		}
		catch (SQLException e) {
			logger.error("Model {} insert error. ", this.toJSON(), e);
		}
		catch (Exception e) {
			logger.error(e);
		}

		return false;
	}

	/**
	 * 判断内容是否相同
	 * @param model
	 * @return
	 * @throws Exception
	 */
	public boolean diff(Model model) throws Exception {

		if(!model.getClass().equals(this.getClass())) {
			throw new Exception("Model class is not equal");
		}

		Field[] fieldList = model.getClass().getDeclaredFields();

		for(Field f : fieldList) {

			if (f.get(model) != null
					&& f.get(this) != null
					&& !f.getName().equals("insert_time")
					&& !f.getName().equals("update_time")) {

				if( ! f.get(model).toString().equals(f.get(this).toString()) ){
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 将model中的字段拷贝到this
	 * @param model
	 * @throws Exception
	 */
	public void copy(Model model) throws Exception {

		if(!model.getClass().equals(this.getClass())) {
			throw new Exception("Can't copy fields from different model class.");
		}

		Field[] fieldList = model.getClass().getDeclaredFields();

		for(Field f : fieldList) {

			try {
				if (f.get(model) != null
					&& !f.getName().equals("insert_time")
					&& !f.getName().equals("update_time")) {

					Field f_ = this.getClass().getField(f.getName());
					f_.set(this, f.get(model));
				}
			} catch (Exception e) {
				logger.error("Error copy model field:{}. ", f.getName(), e);
			}
		}
	}

	/**
	 * 子类重载，写具体的保存快照方法
	 * @param oldVersion
	 * @throws Exception
	 */
	public void createSnapshot(Model oldVersion) throws Exception {

	}
}
