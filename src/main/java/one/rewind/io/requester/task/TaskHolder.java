package one.rewind.io.requester.task;

import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.db.persister.JSONableMapPersister;
import one.rewind.io.requester.parser.TemplateManager;
import one.rewind.json.JSON;
import one.rewind.txt.StringUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Task 序列化对象
 * @author scisaga@gmail.com
 * @date 2018/08/03
 */
@DBName(value = "requester")
@DatabaseTable(tableName = "tasks")
public class TaskHolder extends ModelD implements Comparable<TaskHolder> {

	// 生成 Store 的 task_id 可以为空
	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String generate_id;

	// 如果由 ScheduledTask 生成 会包含这个ID
	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String scheduled_task_id;

	// 类名
	// 根据类名注册Builder 生成 Task时 会用到
	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String class_name;

	// 模板ID
	// 根据模板生成Task时使用
	@DatabaseField(dataType = DataType.INTEGER, width = 11)
	public int template_id;

	// 指纹信息 用于去重
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String fingerprint;

	// 相同指纹信息的任务 最小采集间隔
	@DatabaseField(dataType = DataType.LONG)
	public long min_interval = 0;

	// 任务标签
	@DatabaseField(persisterClass = JSONableFlagListPersister.class, width = 1024)
	public List<Task.Flag> flags = new ArrayList<>();

	// url
	@DatabaseField(dataType = DataType.STRING, width = 4096)
	public String url;

	// 域名
	@DatabaseField(dataType = DataType.STRING, width = 256)
	public String domain;

	// 是否为登录任务
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean need_login = false;

	// 用户名
	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String username;

	// 初始参数
	@DatabaseField(persisterClass = JSONableMapPersister.class, width = 8096)
	public Map<String, Object> vars;

	// 步长
	// 任务指定步骤 当 step = 1 时 不生成下一步任务
	// step = 0 不进行任何限制
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

	// 任务的重试次数
	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int retry_count = 0;

	// task_id trace
	@DatabaseField(persisterClass = JSONableListPersister.class, width = 1024)
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
	 * @param login_task
	 * @param username
	 * @param step
	 * @param priority
	 * @param min_interval
	 */
	public TaskHolder(
			String class_name, String domain, Map<String, Object> vars, boolean login_task, String username, int step, Task.Priority priority, long min_interval,
			Task.Flag... flags
	) {

		this(class_name, 0, domain, vars, login_task, username, step, priority, min_interval, null, null, null, flags);
	}

	/**
	 * 由已有Holder生成Holder场景调用
	 * @param class_name
	 * @param domain
	 * @param vars
	 * @param login_task
	 * @param username
	 * @param step
	 * @param priority
	 * @param min_interval
	 * @param generate_id
	 * @param scheduled_task_id
	 * @param trace
	 */
	public TaskHolder(
		String class_name, int template_id, String domain, Map<String, Object> vars, boolean login_task, String username, int step, Task.Priority priority,
		long min_interval,
		String generate_id,
		String scheduled_task_id,
		List<String> trace,
		Task.Flag... flags
	) {

		this.class_name = class_name;
		this.template_id = template_id;
		this.domain = domain;
		this.vars = vars;

		this.need_login = login_task;

		this.username = username;
		if(username != null && username.length() > 0) {
			this.need_login = true;
		}

		this.step = step;
		this.priority = priority;
		this.min_interval = min_interval;

		// 定义 ID
		this.genId();

		this.generate_id = generate_id;
		this.scheduled_task_id = scheduled_task_id;
		this.trace = trace;

		this.flags = Arrays.asList(flags);
	}

	/**
	 * 指纹和id的区别是 id带有时间戳
	 * @return Self
	 */
	private one.rewind.io.requester.task.TaskHolder genId() {

		Map<String, Object> vars_ = new HashMap<>();

		for(String key : vars.keySet()) {
			if(!key.equals("url"))
				vars_.put(key, vars.get(key));
		}

		String feature = this.class_name + ":" + this.template_id + ":" + this.domain + ":" + this.username + ":" + JSON.toJson(vars_);

		this.id = StringUtil.MD5(feature + "-" + System.currentTimeMillis());

		this.fingerprint = StringUtil.MD5(feature);

		return this;
	}

	/**
	 * 生成ChromeTask
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public Task build() throws Exception {
		return TemplateManager.getInstance().buildTask(this);
	}

	/**
	 * 优先级 比较
	 *
	 * @param another 另一个holder
	 * @return 是否优先
	 */
	public int compareTo(one.rewind.io.requester.task.TaskHolder another) {

		final Task.Priority me = this.priority;
		final Task.Priority it = another.priority;
		if (me.ordinal() == it.ordinal()) {
			return this.create_time.compareTo(another.create_time);
		} else {
			return it.ordinal() - me.ordinal();
		}
	}

	/**
	 * 若生成scheduledTask
	 * 其Id应调用此方法生成，该ID具有唯一性
	 * @return scheduledTaskId
	 */
	public String generateScheduledTaskId() {

		Map<String, Object> vars_ = new HashMap<>();

		for(String key : vars.keySet()) {
			if(!key.equals("url"))
				vars_.put(key, vars.get(key));
		}

		String feature = this.class_name + ":" + this.template_id + ":" + this.domain + ":" + this.username + ":" + JSON.toJson(vars_);

		return StringUtil.MD5(feature);
	}

	/**
	 * @return 是否前置处理
	 */
	public boolean preProc() {
		return flags.contains(Task.Flag.PRE_PROC);
	}

	/**
	 * @return 是否切换代理
	 */
	public boolean switchProxy() {
		return flags.contains(Task.Flag.SWITCH_PROXY);
	}

	/**
	 * @return 是否构建DOM
	 */
	public boolean buildDom() {
		return flags.contains(Task.Flag.BUILD_DOM);
	}

	/**
	 * @return 是否进行截屏
	 */
	public boolean shootScreen() {
		return flags.contains(Task.Flag.SHOOT_SCREEN);
	}

	/**
	 *
	 * @return
	 */
	public String toJSON() {
		return JSON.toJson(this);
	}

	/**
	 *
	 */
	public static class JSONableFlagListPersister extends StringType {

		private static final JSONableFlagListPersister INSTANCE = new JSONableFlagListPersister();

		private JSONableFlagListPersister() {
			super(SqlType.STRING, new Class<?>[] { List.class });
		}

		public static JSONableFlagListPersister getSingleton() {
			return INSTANCE;
		}

		@Override
		public Object javaToSqlArg(FieldType fieldType, Object javaObject) {

			List list = (List) javaObject;

			return list != null ? JSON.toJson(list) : null;
		}

		@Override
		public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {

			Type type = new TypeToken<List<Task.Flag>>() {}.getType();
			List<Task.Flag> list = JSON.fromJson((String)sqlArg, type);
			return sqlArg != null ? list : null;
		}
	}
}
