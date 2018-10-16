package one.rewind.io.requester.parser;

import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.*;

public class Template implements JSONable<Template> {

	public int id;

	Builder builder;

	List<Mapper> mappers = new ArrayList<>();

	public Template() {}

	public Template(Builder builder) {
		this.builder = builder;
	}

	public Template addMapper(Mapper mapper) {
		this.mappers.add(mapper);
		return this;
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Map<String, Object> init_map
	) throws Exception {

		return TemplateManager.getInstance().newHolder(id, init_map, 0);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
