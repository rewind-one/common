package one.rewind.io.requester.parser;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.ArrayList;
import java.util.List;

public class Field implements JSONable<Field> {

	/**
	 * 解析方法
	 */
	public enum Method {
		Reg,
		CssPath
	}

	/**
	 * 替换规则
	 */
	public class Replacement implements JSONable<Replacement> {

		String find;
		String replace;

		public Replacement() {}

		public Replacement(String find, String replace) {
			this.find = find;
			this.replace = replace;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	// 具体解析方法
	Method method = Method.Reg;

	// 字段名
	String name;

	// 字段类型
	String type = String.class.getSimpleName();

	// 默认字符类型值
	String defaultString;

	// 是否可为空
	boolean nullable = true;

	// 后置运算规则 JS
	String evalRule;

	// 解析路径
	String path;

	// 当 method = CssPath 时，可以通过path解析到DOM对象，通过attribute得到对象属性
	String attribute;

	// 内容清洗规则
	List<Replacement> replacements = new ArrayList<>();

	public Field() {}

	/**
	 * JS 字段验证脚本
	 * @param evalRule
	 * @return
	 */
	public Field setEvalRule(String evalRule) {
		this.evalRule = evalRule;
		return this;
	}

	/**
	 *
	 * @return
	 */
	public Field setNotNullable() {
		this.nullable = false;
		return this;
	}

	/**
	 *
	 * @param defaultString
	 * @return
	 */
	public Field setDefaultString(String defaultString) {

		this.defaultString = defaultString;
		return this;
	}

	/**
	 * 正则 结果都是String
	 * @param name
	 * @param path
	 */
	public Field(String name, String path) {
		this.name = name;
		this.path = path;
	}

	/**
	 *
	 * @param name
	 * @param path
	 * @param method
	 */
	public Field(String name, String path, Method method) {
		this.name = name;
		this.path = path;
		this.method = method;
	}

	/**
	 *
	 * @param name
	 * @param path
	 * @param attribute
	 */
	public Field(String name, String path, String attribute) {
		this.name = name;
		this.path = path;
		this.method = Method.CssPath;
		this.attribute = attribute;
	}

	/**
	 *
	 * @param name
	 * @param path
	 * @param attribute
	 * @param method
	 * @param type
	 */
	public Field(String name, String path, String attribute, Method method, String type) {
		this.name = name;
		this.path = path;
		this.attribute = attribute;
		this.method = method;
		this.type = type;
	}

	/**
	 *
	 * @param find
	 * @param replace
	 * @return
	 */
	public Field addReplacement(String find, String replace) {
		this.replacements.add(new Replacement(find, replace));
		return this;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
