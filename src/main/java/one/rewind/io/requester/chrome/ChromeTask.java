package one.rewind.io.requester.chrome;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.scheduler.TaskScheduler;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.exception.TaskException;
import one.rewind.io.requester.task.ScheduledTask;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskBuilder;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.txt.StringUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 */
public class ChromeTask extends Task<ChromeTask> {

	public TaskHolder holder;

	// 执行动作列表
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<ChromeAction> actions = new ArrayList<>();

	// 标志位 是否采集图片
	public boolean noFetchImages = false;

	/**
	 *
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults){

		try {

			ChromeTaskFactory.getInstance().builders.put(clazz, new TaskBuilder(
					url_template,
					init_map_class,
					init_map_defaults
			));

		} catch (Exception e) {
			ChromeAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
	}

	/**
	 *
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param need_login
	 * @param base_priority
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			boolean need_login,
			Task.Priority base_priority
	){

		try {

			ChromeTaskFactory.getInstance().builders.put(clazz, new TaskBuilder(
					url_template,
					init_map_class,
					init_map_defaults,
					need_login,
					base_priority
			));

		} catch (Exception e) {
			ChromeAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
	}

	private static <T> T cast(Object o, Class<T> clazz) {
		return clazz != null && clazz.isInstance(o) ? clazz.cast(o) : null;
	}

	// init_map中支持的类型 int long float double String boolean
	public int getIntFromVars(String key) {
		return cast(holder.vars.get(key), Integer.class);
	}

	public Long getLongFromVars(String key) {
		return cast(holder.vars.get(key), Long.class);
	}

	public float getFloatFromVars(String key) {
		return cast(holder.vars.get(key), Float.class);
	}

	public double getDoubleFromVars(String key) {
		return cast(holder.vars.get(key), Double.class);
	}

	public String getStringFromVars(String key) {
		return cast(holder.vars.get(key), String.class);
	}

	public boolean getBooleanFromVars(String key) {
		return cast(holder.vars.get(key), Boolean.class);
	}

	/**
	 *
	 * @param params
	 * @return
	 */
	public Map<String, Object> newVars(Map<String, Object> params) {
		Map<String, Object> map = new HashMap<>();
		map.putAll(holder.vars);
		map.putAll(params);
		return map;
	}

	/**
	 *
	 * @return
	 */
	public String getFingerprint() {

		if(holder == null || holder.vars == null || holder.vars.keySet().size() == 0) {
			return id;
		}
		else {

			String src = "[" + holder.domain + ":" + holder.username + "];" + this.getClass().getSimpleName() + ";";

			for(String key : holder.vars.keySet()) {
				if(!key.equals("url"))
					src += key + ":" + holder.vars.get(key) + ";";
			}

			return StringUtil.MD5(src);
		}
	}

	/**
	 * 手动设定id
	 * @param id
	 */
	public ChromeTask setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * 从Scheduler 找对应的 ScheduledTask
	 * @return
	 */
	public ScheduledTask getScheduledChromeTask() {
		return holder == null ? null : TaskScheduler.getInstance().getScheduledTask(holder.generateScheduledChromeTaskId());
	}

	/**
	 *
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public ChromeTask(String url) throws MalformedURLException, URISyntaxException {
		super(url);
	}

	/**
	 * 获取前置操作
	 * ChromeAgent 专用
	 * @return
	 */
	public List<ChromeAction> getActions() {
		return actions;
	}

	/**
	 * TODO 与 setRequestFilter 和 setResponseFilter 方法冲突
	 * 对图片请求进行过滤，图片只请求第一次
	 * @return
	 */
	public ChromeTask setNoFetchImages() {
		noFetchImages = true;
		return this;
	}

	/**
	 * 添加前置操作
	 * ChromeAgent 专用
	 * @param action
	 * @return
	 */
	public ChromeTask addAction(ChromeAction action) {
		this.actions.add(action);
		return this;
	}

	/**
	 * 增加完成回调
	 * @param callback
	 * @return
	 */
	public Task addDoneCallback(TaskCallback<ChromeTask> callback) {
		if (this.doneCallbacks == null) this.doneCallbacks = new LinkedList<>();
		this.doneCallbacks.add(callback);
		return this;
	}

	/**
	 * 添加采集异常回调
	 * @param callback
	 * @return
	 */
	public Task addExceptionCallback(TaskCallback<ChromeTask> callback) {
		if (this.exceptionCallbacks == null) this.exceptionCallbacks = new LinkedList<>();
		this.exceptionCallbacks.add(callback);
		return this;
	}



	/**
	 * 返回当前的Holder
	 * @return
	 */
	public TaskHolder getHolder() {
		return holder;
	}

	/**
	 *
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder getHolder(
			Map<String, Object> init_map
	) throws Exception {

		return getHolder(this.getClass(), init_map, getPriority());
	}

	/**
	 * 任务执行过程中调用
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder getHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map
	) throws Exception {

		return getHolder(clazz, init_map, getPriority());
	}

	/**
	 * 任务执行过程中调用
	 * @param clazz
	 * @param init_map
	 * @param priority
	 * @return
	 * @throws Exception
	 */
	public TaskHolder getHolder(
		Class<? extends ChromeTask> clazz,
		Map<String, Object> init_map,
		Priority priority
	) throws Exception {

		int step = 0;

		if(getStep() == 1 || getStep() < 0) {
			throw new TaskException.NoMoreStepException();
		} else {
			step = getStep() - 1;
		}

		return ChromeTaskFactory.getInstance().newHolder(holder, clazz, init_map, getUsername(), step, priority);
	}
}
