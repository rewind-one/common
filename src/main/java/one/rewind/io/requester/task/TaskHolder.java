package one.rewind.io.requester.task;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Hold the door
 * 序列化ChromeTask
 */
public class TaskHolder implements Comparable<TaskHolder>, JSONable<TaskHolder> {

	// 生成 Task 时 复用这个 id
	public String id;

	// 生成 Holder 的 task_id 可以为空
	public String generate_task_id;

	// 如果由 ScheduledChromeTask 生成 会包含这个ID
	public String scheduled_task_id;

	// 类名
	public String class_name;

	// 域名
	public String domain;

	// 是否为登录任务
	public boolean need_login = false;

	// 用户名
	public String username;

	// 初始参数
	public Map<String, Object> vars;

	public String url;

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

	public TaskHolder() {}

	public TaskHolder(
			String class_name, String domain, Map<String, Object> vars, String url, boolean login_task, String username, int step, Task.Priority priority
	) {

		this(class_name, domain, vars, url, login_task, username, step, priority, null, null, null);
	}

	/**
	 *
	 * @param class_name
	 * @param domain
	 * @param vars
	 * @param url
	 * @param login_task
	 * @param username
	 * @param step
	 * @param priority
	 * @param generate_task_id
	 * @param scheduled_task_id
	 * @param trace
	 */
	public TaskHolder(
		String class_name, String domain, Map<String, Object> vars, String url, boolean login_task, String username, int step, Task.Priority priority,
		String generate_task_id,
		String scheduled_task_id,
		List<String> trace
	) {

		this.class_name = class_name;
		this.domain = domain;
		this.vars = vars;
		this.url = url;

		this.need_login = login_task;

		this.username = username;
		if(username != null && username.length() > 0) {
			this.need_login = true;
		}

		this.step = step;
		this.priority = priority;

		// 定义 ID
		this.id = StringUtil.MD5(class_name + "-" + JSON.toJson(vars) + "-" + System.currentTimeMillis());

		this.generate_task_id = generate_task_id;
		this.scheduled_task_id = scheduled_task_id;
		this.trace = trace;
	}

	/**
	 *
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public ChromeTask build() throws Exception {
		return ChromeTaskFactory.getInstance().buildTask(this);
	}

	/**
	 * 优先级比较
	 *
	 * @param another
	 * @return
	 */
	public int compareTo(TaskHolder another) {

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
