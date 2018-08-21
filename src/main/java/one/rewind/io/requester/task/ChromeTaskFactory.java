package one.rewind.io.requester.task;

import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ChromeTaskFactory {

	//
	private static ChromeTaskFactory instance;

	//
	private static final Logger logger = LogManager.getLogger(ChromeTaskFactory.class.getName());

	// 保存所有ChromeTask Class 对应的TaskBuilder
	public Map<Class<? extends ChromeTask>, TaskBuilder> builders = new HashMap<>();

	/**
	 * 单例方法
	 * @return
	 */
	public static ChromeTaskFactory getInstance() {

		if (instance == null) {
			synchronized (ChromeDriverDistributor.class) {
				if (instance == null) {
					instance = new ChromeTaskFactory();
				}
			}
		}

		return instance;
	}

	/**
	 *
	 */
	private ChromeTaskFactory() {

	}

	/**
	 *
	 * @param clazz
	 * @return
	 */
	public Task.Priority getBasePriority(Class<? extends ChromeTask> clazz) {
		TaskBuilder builder = builders.get(clazz);
		if(builder != null) {
			return builder.base_priority;
		}
		else {
			return Task.Priority.MEDIUM;
		}
	}

	/**
	 *
	 * @param holder
	 * @return
	 */
	public ChromeTask buildTask(TaskHolder holder) throws Exception {

		Class<? extends ChromeTask> clazz = (Class<? extends ChromeTask>) Class.forName(holder.class_name);

		TaskBuilder builder = builders.get(clazz);

		if(builder == null) throw new Exception("Builder not exist for " + clazz.getName());

		// 生成URL


		// Call sub class constructor
		Constructor<?> cons = clazz.getConstructor(String.class);
		ChromeTask task = (ChromeTask) cons.newInstance(holder.url);
		task.holder = holder;

		// 验证 vars
		if(builder.need_login) task.setLoginTask();
		task.setUsername(holder.username);
		task.setStep(holder.step);
		task.setPriority(holder.priority);

		// 设定关联ID
		task.setId(holder.id);

		return task;
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
				holder.class_name, holder.domain, holder.vars, holder.url, holder.need_login, holder.username, holder.step, holder.priority,
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
		Map<String, Object> init_map,
		String username,
		int step,
		Task.Priority priority
	) throws Exception {

		TaskBuilder builder = builders.get(clazz);

		if(builder == null) throw new Exception(clazz.getName() + " builder not exist.");

		// 生成变量表
		Map<String, Object> vars = builder.validateInitMap(init_map);

		// 生成URL
		String url = builder.url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		// 定义优先级
		if(priority == null) {
			priority = builder.base_priority;
		}

		List<String> trace = holder.trace;
		if(trace == null) trace = new ArrayList<>();
		trace.add(holder.id);

		return new TaskHolder(
				clazz.getName(), builder.domain, vars, url, builder.need_login, username, step, priority,
				holder.id, holder.scheduled_task_id, trace);
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
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		String username,
		int step,
		Task.Priority priority
	) throws Exception {

		TaskBuilder builder = builders.get(clazz);

		if(builder == null) throw new Exception(clazz.getName() + " builder not exist.");

		// 生成变量表
		Map<String, Object> vars = builder.validateInitMap(init_map);

		// 生成URL
		String url = builder.url_template;
		for(String key : vars.keySet()) {
			url = url.replace("{{" + key + "}}", String.valueOf(vars.get(key)));
		}

		// 定义优先级
		if(priority == null) {
			priority = builder.base_priority;
		}

		return new TaskHolder(clazz.getName(), builder.domain, vars, url, builder.need_login, username, step, priority);
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

		return newHolder(clazz, init_map, username, step, null);
	}

	/**
	 * 起始创建任务调用
	 * @param clazz
	 * @param username
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder newHolder(
		Class<? extends ChromeTask> clazz,
		String username,
		Map<String, Object> init_map
	) throws Exception {

		return newHolder(clazz, init_map, username, 0);
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

}
