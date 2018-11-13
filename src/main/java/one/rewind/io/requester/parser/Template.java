package one.rewind.io.requester.parser;

import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.*;

/**
 *
 */
public class Template implements JSONable<Template> {

	// id
	public int id;

	// builder 生成任务用
	Builder builder;

	// mappers 列表
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

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
