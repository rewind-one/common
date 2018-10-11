package one.rewind.io.requester.parser;

import one.rewind.db.model.ESIndex;
import one.rewind.db.model.Model;
import one.rewind.db.model.ModelD;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import one.rewind.util.ReflectModelUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mapper implements JSONable<Mapper> {

	String modelClassName;

	int templateId;

	// 字段解析规则
	List<Field> fields = new ArrayList<>();

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	public Mapper() {}

	public Mapper(String modelClassName) throws Exception {

		Class<?> clazz = Class.forName(modelClassName);
		if(! clazz.isAssignableFrom(Model.class)) throw new Exception("Model class name unrecognizable");
	}

	public Mapper(int templateId) {
		this.templateId = templateId;
	}

	/**
	 *
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public List<TaskHolder> eval(Map<String, Object> data) throws Exception {

		List<TaskHolder> nts = new ArrayList<>();

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
		else if(templateId != 0) {

			Template tpl = TemplateManager.getInstance().getTemplate(templateId);
			nts.add(tpl.newHolder(data));
		}

		return nts;
	}
}
