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

	List<Replacement> replacements = new ArrayList<>();

	public Field() {}

	public Field(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public Field(String name, String path, String type) {
		this.name = name;
		this.path = path;
		this.type = type;
	}

	public Field(String name, String path, Method method, String type) {
		this.name = name;
		this.path = path;
		this.method = method;
		this.type = type;
	}

	public Field addReplacement(String find, String replace) {
		this.replacements.add(new Replacement(find, replace));
		return this;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
