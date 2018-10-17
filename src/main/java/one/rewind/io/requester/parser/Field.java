package one.rewind.io.requester.parser;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.ArrayList;
import java.util.List;

public class Field implements JSONable<Field> {

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

	Method method = Method.Reg;

	String name;

	String type = String.class.getSimpleName();

	String path;

	String attribute;

	List<Replacement> replacements = new ArrayList<>();

	public Field() {}

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
