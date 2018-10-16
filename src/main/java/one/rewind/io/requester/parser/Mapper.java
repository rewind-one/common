package one.rewind.io.requester.parser;

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

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	public Mapper() {}

	/**
	 *
	 * @param modelClassName
	 * @throws Exception
	 */
	public Mapper(String modelClassName, boolean multi, String path, Field.Method method) throws Exception {

		Class<?> clazz = Class.forName(modelClassName);
		if(! clazz.isAssignableFrom(Model.class)) throw new Exception("Model class name unrecognizable");
		this.multi = multi;
		this.path = path;
		this.method = method;
	}

	/**
	 *
	 * @param templateId
	 */
	public Mapper(int templateId, boolean multi, String path, Field.Method method) {
		this.templateId = templateId;
		this.multi = multi;
		this.path = path;
		this.method = method;
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

			// 需要id赋值
			if(modelClassName.equals(ESIndex.class.getName()) || modelClassName.equals(ModelD.class.getName())) {
				String id = StringUtil.MD5(modelClassName + "::" + JSON.toJson(data));
				data.put("id", id);
			}

			Class<?> clazz = Class.forName(modelClassName);
			Object model = ReflectModelUtil.toObj(data, clazz);

			((Model) model).insert();
		}
		// 生成下一级任务
		else if(templateId != 0) {

			Template tpl = TemplateManager.getInstance().getTemplate(templateId);
			nts.add(tpl.newHolder(data));
		}
		// 测试用
		else {
			TemplateManager.logger.info(JSON.toPrettyJson(data));
		}

		return nts;
	}
}
