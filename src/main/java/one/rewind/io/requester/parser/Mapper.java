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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Mapper implements JSONable<Mapper> {

	// 数据类型 如果需要解析数据
	String modelClassName;

	// 模板id
	int templateId;

	// 预筛选条件
	String path;

	// 预筛选方法
	Field.Method method = Field.Method.Reg;

	// 是否多值匹配
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
	public Model eval(Map<String, Object> data, String url, String post_data, List<TaskHolder> nts) throws Exception {

		Model model = null;

		// 解析数据并保存
		if(modelClassName != null) {

			Class<? extends Model> clazz = (Class<? extends Model>) Class.forName(modelClassName);

			// 需要id赋值
			if(ModelD.class.isAssignableFrom(clazz)) {

				//判断id是否需要自定义
				if (!data.containsKey("id")){

					String id = StringUtil.MD5(modelClassName + "::" + url + "::" + (post_data == null? "" : post_data));
					// String id = StringUtil.MD5(modelClassName + "::" + JSON.toJson(data));
					data.put("id", id);
				}
			}

			// TODO 验证
			// 如果 model 中包含属性 va，data 中 没有key va ==> va = null
			// model中没有属性 vb，data中 有key vb ==> vb 被忽略，不会报错
			model = (Model) ReflectModelUtil.toObj(data, clazz);
		}

		// 生成下一级任务
		else if(templateId != 0) {

			Template tpl = TemplateManager.getInstance().get(templateId);

			// TODO 此处应该判断当前的TaskHolder的 step，生成的新 TH step 应 --
			nts.add(tpl.at(data));

		}
		return model;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	/**
	 * 生成唯一Hash特征
	 * @return
	 */
	public String getHash() {
		return StringUtil.MD5(toJSON());
	}
}
