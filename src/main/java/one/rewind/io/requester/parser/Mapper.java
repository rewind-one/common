package one.rewind.io.requester.parser;

import com.sun.istack.internal.NotNull;
import one.rewind.db.model.ESIndex;
import one.rewind.db.model.Model;
import one.rewind.db.model.ModelD;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import one.rewind.util.ReflectModelUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Mapper implements JSONable<Mapper> {

	String modelClassName;

	int templateId;

	String path;

	Field.Method method = Field.Method.Reg;

	boolean multi = false;

	// 字段解析规则
	List<Field> fields = new ArrayList<>();

	/**
	 * 获取默认 Mapper
	 * @param templateId
	 * @return
	 */
	public static Mapper of(
			int templateId,
			@NotNull String urlReg
	){
		return of(templateId, urlReg, null);
	}

	/**
	 * 获取默认 Mapper
	 * @param templateId
	 * @return
	 */
	public static Mapper of(
			int templateId,
			@NotNull String urlReg,
			String postDataReg
	){

		Mapper mapper = null;

		try {

			mapper = new Mapper(null, templateId, false, null, Field.Method.Reg);
			mapper.addField(new Field("url", urlReg));
			if(postDataReg != null) {
				mapper.addField(new Field("post_data", postDataReg));
			}

		} catch (Exception e) {
			TemplateManager.logger.error("You shouldn't be here", e);
		}

		return mapper;
	}

	/**
	 *
	 */
	public Mapper() {}

	/**
	 *
	 * @param modelClassName
	 * @throws Exception
	 */
	public Mapper(String modelClassName, Field... fields) throws Exception {

		this(modelClassName, 0, false, null, Field.Method.Reg, fields);
	}

	/**
	 *
	 * @param templateId
	 * @throws Exception
	 */
	public Mapper(int templateId, Field... fields) throws Exception {

		this(null, templateId, false, null, Field.Method.Reg, fields);
	}

	/**
	 *
	 * @param templateId
	 * @param multi
	 * @param fields
	 * @throws Exception
	 */
	public Mapper(int templateId, boolean multi, Field... fields) throws Exception {

		this(null, templateId, multi, null, Field.Method.Reg, fields);
	}

	/**
	 *
	 * @param modelClassName
	 * @throws Exception
	 */
	public Mapper(String modelClassName, int templateId, boolean multi, String path, Field.Method method, Field... fields) throws Exception {

		if(modelClassName != null && modelClassName.length() > 0) {
			Class<?> clazz = Class.forName(modelClassName);
			if (!Model.class.isAssignableFrom(clazz)) throw new Exception("Model class name unrecognizable");
			this.modelClassName = modelClassName;
		}

		this.templateId = templateId;
		this.multi = multi;
		this.path = path;
		this.method = method;

		for(Field field : fields) {
			this.fields.add(field);
		}
	}

	/**
	 *
	 * @param field
	 * @return
	 */
	public Mapper addField(Field field) {
		this.fields.add(field);
		return this;
	}

	/**
	 *
	 * @return
	 */
	public Mapper setMulti() {
		this.multi = true;
		return this;
	}

	/**
	 *
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public List<TaskHolder> eval(Map<String, Object> data) throws Exception {

		List<TaskHolder> nts = new ArrayList<>();

		// 解析数据并保存
		if(modelClassName != null) {

			Class<?> clazz = Class.forName(modelClassName);

			// 需要id赋值
			if(ESIndex.class.isAssignableFrom(clazz) || ModelD.class.isAssignableFrom(clazz)) {
				String id = StringUtil.MD5(modelClassName + "::" + JSON.toJson(data));
				System.err.println("----------"+data);
				data.put("id", id);
			}

			Object model = ReflectModelUtil.toObj(data, clazz);

			// TODO 新增数据 insert
			// TODO 已有数据 update
			((Model) model).insert();
		}
		// 生成下一级任务
		else if(templateId != 0) {

			Template tpl = TemplateManager.getInstance().get(templateId);
			nts.add(tpl.at(data));
		}
		// 测试用
		else {
			TemplateManager.logger.info(JSON.toPrettyJson(data));
		}

		return nts;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
