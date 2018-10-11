package one.rewind.io.requester.parser;

import one.rewind.io.requester.chrome.ChromeTask;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author scisaga@gmail.com
 * @date 2018/10/8
 */
public class TemplateManager {

	public static final Logger logger = LogManager.getLogger(TemplateManager.class.getName());

	public static TemplateManager instance;

	/**
	 * 单例方法
	 * @return
	 */
	public static TemplateManager getInstance() {

		if (instance == null) {

			synchronized(TemplateManager.class) {
				if (instance == null) {
					instance = new TemplateManager();
				}
			}
		}

		return instance;
	}

	private ConcurrentHashMap<Integer, Template> templates = new ConcurrentHashMap<>();

	// 保存所有ChromeTask Class 对应的TaskBuilder
	private Map<Class<? extends ChromeTask>, Builder> chrome_task_builders = new HashMap<>();

	private TemplateManager() {}

	/**
	 *
	 * @param tpl
	 * @return
	 */
	public TemplateManager addTemplate(Template tpl) {
		templates.put(tpl.id, tpl);
		return this;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public Template getTemplate(int id) {
		return templates.get(id);
	}

	public TemplateManager addBuilder(Class<? extends ChromeTask> clazz, Builder builder) {
		chrome_task_builders.put(clazz, builder);
		return this;
	}

	/**
	 *
	 * @param clazz
	 * @return
	 */
	public Builder getBuilder(Class<? extends ChromeTask> clazz) {
		return chrome_task_builders.get(clazz);
	}

	/**
	 *
	 * @param holder
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			TaskHolder holder
	) {

		List<String> trace = holder.trace;
		if(trace == null) trace = new ArrayList<>();
		trace.add(holder.id);

		return new TaskHolder(
				holder.class_name, 0, holder.domain, holder.vars, holder.need_login, holder.username, holder.step, holder.priority,
				holder.id, holder.scheduled_task_id, trace);
	}

	/**
	 *
	 * @param holder
	 * @param clazz
	 * @param init_map
	 * @param username
	 * @param step
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			TaskHolder holder,
			Class<? extends ChromeTask> clazz,
			int template_id,
			Map<String, Object> init_map,
			String username,
			int step,
			Task.Priority priority
	) throws Exception {

		TaskHolder new_holder = newHolder(clazz, template_id, init_map, username, step, priority);

		if(holder != null) {

			List<String> trace = holder.trace;
			if(trace == null) trace = new ArrayList<>();
			trace.add(holder.id);

			new_holder.generate_task_id = holder.id;
			new_holder.scheduled_task_id = holder.scheduled_task_id;
			new_holder.trace = trace;
		}

		return new_holder;
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @param username
	 * @param step
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Class<? extends Task> clazz,
			int template_id,
			Map<String, Object> init_map,
			String username,
			int step,
			Task.Priority priority
	) throws Exception {

		Builder builder;

		if(clazz.getSimpleName().equals(Task.class.getSimpleName())) {

			// get template
			Template tpl = this.getTemplate(template_id);

			if(tpl == null) {
				throw new Exception("Template not exist: " + template_id);
			}

			builder = tpl.builder;

		}
		// Chrome Task
		else if(clazz.equals(ChromeTask.class) || clazz.isAssignableFrom(ChromeTask.class)){

			// get builder
			builder = chrome_task_builders.get(clazz);

			if(builder == null) throw new Exception(clazz.getName() + " builder not exists");
		}
		else {
			throw new Exception("Unsupported class: " + clazz.getName());
		}

		// 生成变量表
		Map<String, Object> vars = builder.validateInitMap(init_map);

		// 定义优先级
		if(priority == null) {
			priority = builder.base_priority;
		}

		return new TaskHolder(clazz.getName(), builder.domain, vars, builder.need_login, username, step, priority);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param username
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			String username,
			int step
	) throws Exception {

		return newHolder(clazz, 0, init_map, username, step, null);
	}

	/**
	 *
	 * @param template_id
	 * @param init_map
	 * @param username
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			int template_id,
			Map<String, Object> init_map,
			String username,
			int step
	) throws Exception {

		return newHolder(Task.class, template_id, init_map, username, step, null);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			String username
	) throws Exception {

		return newHolder(clazz, init_map, username, 0);
	}

	/**
	 *
	 * @param template_id
	 * @param init_map
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			int template_id,
			Map<String, Object> init_map,
			String username
	) throws Exception {

		return newHolder(Task.class, template_id, init_map, username, 0, null);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @param step
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			int step
	) throws Exception {

		return newHolder(clazz, init_map, null, step);
	}

	public TaskHolder newHolder(
			int template_id,
			Map<String, Object> init_map,
			int step
	) throws Exception {

		return newHolder(template_id, init_map, null, step);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map
	) throws Exception {

		return newHolder(clazz, init_map, null, 0);
	}

	/**
	 *
	 * @param template_id
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
			int template_id,
			Map<String, Object> init_map
	) throws Exception {

		return newHolder(template_id, init_map, 0);
	}

	/**
	 *
	 * @param holder
	 * @return
	 */
	public Task buildTask(TaskHolder holder) throws Exception {

		Builder builder;
		Class<? extends Task> clazz;
		Template tpl = null;

		if(holder.class_name.equals(Task.class.getSimpleName())) {

			clazz = Task.class;

			tpl = this.getTemplate(holder.template_id);

			if(tpl == null) {
				throw new Exception("Template not exist: " + holder.toJSON());
			}

			builder = tpl.builder;
		}
		// Chrome Task
		else {

			clazz = (Class<? extends ChromeTask>) Class.forName(holder.class_name);

			// get builder
			builder = chrome_task_builders.get(clazz);
			if(builder == null) throw new Exception(clazz.getName() + " builder not exists");
		}

		// 生成URL
		String url = builder.url_template;
		for(String key : holder.vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(holder.vars.get(key)));
		}
		// 生成POST_DATA
		String post_data = builder.post_data_template;
		if(post_data != null) {
			for (String key : holder.vars.keySet()) {
				post_data = post_data.replace("{{" + key + "}}", String.valueOf(holder.vars.get(key)));
			}
		}

		// Call sub class constructor
		Constructor<?> cons = clazz.getConstructor(String.class);
		Task task = (ChromeTask) cons.newInstance(url);
		task.holder = holder;

		// 验证 vars
		if(builder.need_login) task.setLoginTask();
		task.setUsername(holder.username);
		task.setStep(holder.step);
		task.setPriority(holder.priority);

		// 设定关联ID
		task.id = holder.id;

		// 如果holder有对应的template 则添加template mapper处理方法
		if(tpl != null) {

			Template finalTpl = tpl;

			task.addNextTaskGenerator((t, nts) -> {

				Parser parser = new Parser(t.getResponse().getText(), t.getResponse().getDoc());

				for(Mapper mapper : finalTpl.mappers) {
					Map<String, Object> data = parser.parse(mapper);
					nts.addAll(mapper.eval(data));
				}
			});
		}

		return task;
	}
}
