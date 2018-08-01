package one.rewind.io.requester.task;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Hold the door
 * 序列化ChromeTask
 */
public class ChromeTaskHolder implements Comparable<ChromeTaskHolder>, JSONable<ChromeTaskHolder> {

	// 生成 Task 时 复用这个 id
	public String id;

	// 生成 Holder 的 task_id 可以为空
	public String generate_id;

	// 类名
	public String class_name;

	// 域名
	public String domain;

	// 是否为登录任务
	public boolean login_task = false;

	// 用户名
	public String username;

	// 初始参数
	public Map<String, Object> init_map;

	// 步长
	public int step;

	// 优先级
	public Task.Priority priority = Task.Priority.MEDIUM;

	// task_id trace
	public List<String> trace;

	// 创建时间
	public Date create_time = new Date();

	// 执行时间
	public Date exec_time;

	// 任务是否已经完成
	public boolean done = false;

	// 所有子任务是否已经完成
	public boolean all_done = false;


	public ChromeTaskHolder() {}

	/**
	 *
	 * @param class_name
	 * @param domain
	 * @param username
	 * @param init_map
	 * @param step
	 */
	public ChromeTaskHolder(String class_name, String domain, boolean login_task, String username, Map<String, Object> init_map, int step) {
		this(class_name, domain, login_task, username, init_map, step, Task.Priority.MEDIUM);
	}

	/**
	 *
	 * @param class_name
	 * @param domain
	 * @param username
	 * @param init_map
	 * @param step
	 * @param priority
	 */
	public ChromeTaskHolder(String class_name, String domain, boolean login_task, String username, Map<String, Object> init_map, int step, Task.Priority priority) {

		this.class_name = class_name;
		this.domain = domain;
		this.login_task = login_task;

		this.username = username;
		if(username != null && username.length() > 0) {
			this.login_task = true;
		}

		this.init_map = init_map;
		this.step = step;
		this.priority = priority;

		String init_map_json = JSON.toJson(init_map);

		// 定义Map
		this.id = StringUtil.MD5(class_name + "-" + init_map_json + "-" + System.currentTimeMillis());
	}

	/**
	 *
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public ChromeTask build() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		Class<?> clazz = Class.forName(class_name);

		/*for(Method m : threadClazz.getMethods()) {
			System.err.println(m.getName());
			for(Class<?> tc : m.getParameterTypes()) {
				System.err.println("\t" + tc.getName());
			}
		}*/

		Method method = clazz.getMethod("build", Class.class, Map.class, String.class, int.class, Task.Priority.class);

		ChromeTask task =
				(ChromeTask) method.invoke(null, clazz, init_map, username, step, priority);

		// 设定关联ID
		task.setId(id).setPossibleScheduledChromeTaskId(
			StringUtil.MD5(this.class_name + "-" + JSON.toJson(this.init_map))
		);

		return task;
	}

	/**
	 * 优先级比较
	 *
	 * @param another
	 * @return
	 */
	public int compareTo(ChromeTaskHolder another) {

		final Task.Priority me = this.priority;
		final Task.Priority it = another.priority;
		if (me.ordinal() == it.ordinal()) {
			return this.create_time.compareTo(another.create_time);
		} else {
			return it.ordinal() - me.ordinal();
		}
	}

	/**
	 *
	 * @return
	 */
	public String toJSON() {
		return JSON.toJson(this);
	}


}
