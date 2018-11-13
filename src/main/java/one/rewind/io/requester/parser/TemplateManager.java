package one.rewind.io.requester.parser;

import one.rewind.io.requester.chrome.ChromeTask;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

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

	/**
	 *
	 */
	private TemplateManager() {}

	/**
	 *
	 * @param tpls
	 * @return
	 */
	public TemplateManager add(Template... tpls) {

		for(Template tpl : tpls) {
			templates.put(tpl.id, tpl);
		}

		return this;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public Template get(int id) {
		return templates.get(id);
	}

	/**
	 *
	 * @param clazz
	 * @param builder
	 * @return
	 */
	public TemplateManager add(Class<? extends ChromeTask> clazz, Builder builder) {
		chrome_task_builders.put(clazz, builder);
		return this;
	}

	/**
	 *
	 * @param clazz
	 * @return
	 */
	public Builder get(Class<? extends ChromeTask> clazz) {
		return chrome_task_builders.get(clazz);
	}

	/**
	 * 生成新Holder I
	 *
	 * 使用场景：周期任务生成
	 *     ScheduledTask中包含了TaskHolder 生成新 TaskHolder时使用
	 *     基本上就是复制原有Holder
	 *
	 * @param holder 被复制的Holder
	 * @return 新的Holder
	 */
	public TaskHolder newHolder(
			TaskHolder holder
	) {

		List<String> trace = holder.trace;
		if(trace == null) trace = new ArrayList<>();
		trace.add(holder.id);

		Task.Flag[] flag_array = new Task.Flag[holder.flags.size()];

		return new TaskHolder(
				holder.class_name, holder.template_id, holder.domain, holder.vars, holder.need_login, holder.username, holder.step, holder.priority, holder.min_interval,
				holder.id, holder.scheduled_task_id, trace,
				holder.flags.toArray(flag_array));
	}

	/**
	 * 生成新Holder II
	 *
	 * 使用场景：任务执行中，生成的新的Holder
	 *     新的Holder 要包含原Holder 的 generate_task_id, scheduled_task_id, trace
	 *
	 * @param holder
	 * @param clazz
	 * @param init_map
	 * @param username
	 * @param step
	 * @param priority
	 * @return
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
	 * 生成新Holder 0
	 * 使用场景：任意
	 *
	 * @param clazz 任务类
	 * @param init_map 初始化map
	 * @param username 用户名
	 * @param step 最大可执行步骤
	 * @param priority 优先级
	 * @return TaskHolder
	 * @throws Exception 异常
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
			Template tpl = this.get(template_id);

			if(tpl == null) {
				throw new Exception("Template not exist: " + template_id);
			}

			builder = tpl.builder;

		}
		// Chrome Task
		else if(clazz.equals(ChromeTask.class) || ChromeTask.class.isAssignableFrom(clazz)){

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

		Task.Flag[] flag_array = new Task.Flag[builder.flags.size()];

		return new TaskHolder(clazz.getName(), template_id, builder.domain, vars, builder.need_login, username, step, priority, builder.min_interval, null, null, null, builder.flags.toArray(flag_array));
	}

	/**
	 * Holder --> Task
	 *
	 * @param holder
	 * @return
	 */
	public Task buildTask(TaskHolder holder) throws Exception {

		Builder builder;
		Class<? extends Task> clazz;
		Template tpl = null;

		if(holder.class_name.equals(Task.class.getName())) {

			clazz = Task.class;

			tpl = this.get(holder.template_id);

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
		Task task = (Task) cons.newInstance(url);
		task.holder = holder;

		// 验证 vars
		if(builder.need_login) task.setLoginTask();
		task.setUsername(holder.username);

		// 设定关联ID
		task.id = holder.id;

		// 如果holder有对应的template 则添加template mapper处理方法
		if(tpl != null) {

			Template finalTpl = tpl;

			task.addNextTaskGenerator((t, nts) -> {

				Parser parser = new Parser(t.getResponse().getText(), t.getResponse().getDoc());

				for(Mapper mapper : finalTpl.mappers) {

					// mapper 解析数据
					for( Map<String, Object> data : parser.parse(mapper) ) {

						// 对解析的数据进行后续处理
						nts.addAll(mapper.eval(data));
					}
				}
			});
		}

		return task;
	}
}
