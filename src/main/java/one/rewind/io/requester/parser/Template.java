package one.rewind.io.requester.parser;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.Model;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.db.persister.JSONablePersister;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;

import java.util.*;

/**
 *
 */
@DatabaseTable(tableName = "templates")
@DBName(value = "tpl")
public class Template extends Model {

	// id
	@DatabaseField(persisterClass = JSONablePersister.class, dataType = DataType.INTEGER, index = true)
	public int id;

	// builder 生成任务用
	@DatabaseField(persisterClass = JSONablePersister.class, columnDefinition = "MEDIUMTEXT")
	Builder builder;

	@DatabaseField(persisterClass = JSONablePersister.class, columnDefinition = "MEDIUMTEXT")
	Validator validator;

	// mappers 列表
	@DatabaseField(persisterClass = JSONableMapperListPersister.class, columnDefinition = "MEDIUMTEXT")
	List<Mapper> mappers = new ArrayList<>();

	/**
	 *
	 * @param id
	 * @param nextId
	 * @param urlReg
	 * @return
	 */
	public static Template of(int id, int nextId, String urlReg) throws Exception {

		Template template = new Template(id, Builder.of(), Mapper.of(nextId, urlReg));

		return template;
	}

	/**
	 *
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static Template dummy(int id) throws Exception {

		Template template = new Template(id, Builder.of(Task.Flag.BUILD_DOM), new Mapper());

		return template;
	}

	/**
	 *
	 */
	public Template() {}

	/**
	 *
	 * @param builder
	 */
	public Template(int id, Builder builder, Mapper... mappers) {

		this.id = id;
		this.builder = builder;

		for(Mapper mapper : mappers) {
			this.mappers.add(mapper);
		}
	}

	/**
	 *
	 * @param mapper
	 * @return
	 */
	public Template addMapper(Mapper mapper) {
		this.mappers.add(mapper);
		return this;
	}

	/**
	 *
	 * @return
	 */
	public List<Mapper> getMappers() {
		return this.mappers;
	}

	/**
	 * 设置验证器
	 * @param validator
	 * @return
	 */
	public Template setValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	public Template setMinInterval(long minInterval) {
		this.builder.min_interval = minInterval;
		return this;
	}
	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder at(
			Map<String, Object> init_map
	) throws Exception {

		return at(init_map, null,0);
	}

	/**
	 * 生成新Holder 0
	 * 简化模式B
	 * 使用场景：任意
	 *
	 * @param init_map 初始化map
	 * @param username
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public TaskHolder at(
			Map<String, Object> init_map,
			String username,
			int step
	) throws Exception {

		return TemplateManager.getInstance().newHolder(Task.class, id, init_map, username, step, null);
	}

	/**
	 * 生成新Holder 0
	 * 简化模式D
	 * 使用场景：任意
	 *
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public TaskHolder at(
			Map<String, Object> init_map,
			String username
	) throws Exception {

		return TemplateManager.getInstance().newHolder(Task.class, id, init_map, username, 0, null);
	}

	/**
	 * ormlite框架下的list<Mapper>持久化实现
	 */
	public static class JSONableMapperListPersister extends JSONableListPersister {

		private static final Template.JSONableMapperListPersister INSTANCE = new Template.JSONableMapperListPersister();

		public static Template.JSONableMapperListPersister getSingleton() {
			return INSTANCE;
		}

		private JSONableMapperListPersister() {
			super();
		}

		@Override
		public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {

			List list = JSON.fromJson((String) sqlArg, new TypeToken<List<Mapper>>() {}.getType());
			return sqlArg != null ? list : null;
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
