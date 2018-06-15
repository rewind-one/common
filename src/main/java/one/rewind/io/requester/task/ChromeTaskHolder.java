package one.rewind.io.requester.task;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

/**
 * Hold the door
 */
public class ChromeTaskHolder implements Comparable<ChromeTaskHolder>, JSONable<ChromeTaskHolder> {

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

	// 步骤
	public int step;

	// 优先级
	public Task.Priority priority = Task.Priority.MEDIUM;

	// 创建时间
	public Date create_time = new Date();

	/**
	 *
	 * @param class_name
	 * @param domain
	 * @param init_map
	 * @param step
	 */
	public ChromeTaskHolder(String class_name, String domain, Map<String, Object> init_map, int step) {
		this.class_name = class_name;
		this.domain = domain;
		this.init_map = init_map;
		this.step = step;
	}

	/**
	 *
	 * @param class_name
	 * @param domain
	 * @param username
	 * @param init_map
	 * @param step
	 */
	public ChromeTaskHolder(String class_name, String domain, String username, Map<String, Object> init_map, int step) {
		this.class_name = class_name;
		this.domain = domain;
		this.username = username;
		if(username != null || username.length() > 0) {
			this.login_task = true;
		}
		this.init_map = init_map;
		this.step = step;
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
	public ChromeTaskHolder(String class_name, String domain, String username, Map<String, Object> init_map, int step, Task.Priority priority) {
		this.class_name = class_name;
		this.domain = domain;
		this.username = username;
		if(username != null || username.length() > 0) {
			this.login_task = true;
		}
		this.init_map = init_map;
		this.step = step;
		this.priority = priority;
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

		Class<?> threadClazz = Class.forName(class_name);

		Method method = threadClazz.getMethod("build", Map.class, int.class, Task.Priority.class);

		ChromeTask task =
				(ChromeTask) method.invoke(null, init_map, step, priority);

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
