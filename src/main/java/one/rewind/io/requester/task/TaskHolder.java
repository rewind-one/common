package one.rewind.io.requester.task;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ChromeTask 序列化对象
 * @author scisaga@gmail.com
 * @date 2018/08/03
 */
@DBName(value = "requester")
@DatabaseTable(tableName = "tasks")
public class TaskHolder implements Comparable<TaskHolder>, JSONable<TaskHolder> {

	// 生成 Task 时 复用这个 id
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, id = true)
	public String id;

	// 生成 Holder 的 task_id 可以为空
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, index = true)
	public String generate_task_id;

	// 如果由 ScheduledChromeTask 生成 会包含这个ID
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, index = true)
	public String scheduled_task_id;

	// 类名
	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String class_name;

	// 域名
	@DatabaseField(dataType = DataType.STRING, width = 256, canBeNull = false)
	public String domain;

	// 是否为登录任务
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean need_login = false;

	// 用户名
	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String username;

	// 初始参数
	public Map<String, Object> vars;

	// URL
	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String url;

	// 步长
	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int step = 0;

	// 优先级
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32, canBeNull = false)
	public Task.Priority priority = Task.Priority.MEDIUM;


	// 任务是否已经完成
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean done = false;

	// 所有子任务是否已经完成
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean all_done = false;

	// task_id trace
	public List<String> trace;

	// 创建时间
	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date create_time = new Date();

	// 执行时间
	@DatabaseField(dataType = DataType.DATE)
	public Date exec_time;

	// 任务完成时间
	@DatabaseField(dataType = DataType.DATE)
	public Date done_time;

	// 全部后续任务 完成时间
	@DatabaseField(dataType = DataType.DATE)
	public Date all_done_time;

	/**
	 *
	 */
	public TaskHolder() {}

	/**
	 * 新Holder场景调用
	 * @param class_name
	 * @param domain
	 * @param vars
	 * @param url
	 * @param login_task
	 * @param username
	 * @param step
	 * @param priority
	 */
	public TaskHolder(
			String class_name, String domain, Map<String, Object> vars, String url, boolean login_task, String username, int step, Task.Priority priority
	) {

		this(class_name, domain, vars, url, login_task, username, step, priority, null, null, null);
	}

	/**
	 * 由已有Holder生成Holder场景调用
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
	 * 插入新代理记录
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 更新代理记录
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception{

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 生成ChromeTask
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
	 * 优先级 比较
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
	 * 若生成scheduledChromeTask
	 * 其Id应调用此方法生成
	 * @return
	 */
	public String generateScheduledChromeTaskId() {

		return StringUtil.MD5(this.class_name + "-" + JSON.toJson(this.vars));
	}

	/**
	 *
	 * @return
	 */
	public String toJSON() {
		return JSON.toJson(this);
	}

}
