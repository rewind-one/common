package one.rewind.io.requester.chrome;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.exception.TaskException;
import one.rewind.io.requester.parser.Builder;
import one.rewind.io.requester.parser.Template;
import one.rewind.io.requester.parser.TemplateManager;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;

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
	 * 注册Builder
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

			TemplateManager.getInstance().add(clazz, new Builder(
					url_template,
					init_map_class,
					init_map_defaults
			));

		} catch (Exception e) {
			ChromeAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
	}

	/**
	 * 注册Builder
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param min_interval
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			long min_interval
	){

		try {

			TemplateManager.getInstance().add(clazz, new Builder(
					url_template,
					init_map_class,
					init_map_defaults,
					min_interval
			));

		} catch (Exception e) {
			ChromeAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
	}

	/**
	 * 注册Builder
	 * @param clazz
	 * @param url_template
	 * @param init_map_class
	 * @param init_map_defaults
	 * @param need_login
	 * @param base_priority
	 * @param min_interval
	 */
	public static void registerBuilder(
			Class<? extends ChromeTask> clazz,
			String url_template,
			Map<String, Class> init_map_class,
			Map<String, Object> init_map_defaults,
			boolean need_login,
			Task.Priority base_priority,
			long min_interval
	){

		try {

			TemplateManager.getInstance().add(clazz, new Builder(
					url_template,
					null,
					init_map_class,
					init_map_defaults,
					min_interval,
					need_login,
					base_priority
			));

		} catch (Exception e) {
			ChromeAgent.logger.error("Register {} builder failed. ", clazz.getName(), e);
		}
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
	 * 生成新Holder 0
	 * 简化模式 A-1
	 * 使用场景：任意
	 *
	 * @param clazz 任务类
	 * @param init_map 初始化map
	 * @param username 用户名
	 * @param step 步骤数
	 * @return TaskHolder
	 * @throws Exception 异常
	 */
	public static TaskHolder at(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			String username,
			int step,
			Priority priority
	) throws Exception {

		return TemplateManager.getInstance().newHolder(clazz, 0, init_map, username, step, priority);
	}

	/**
	 * 生成新Holder 0
	 * 简化模式 A-2
	 * 使用场景：任意
	 *
	 * @param clazz 任务类
	 * @param init_map 初始化map
	 * @param username 用户名
	 * @return TaskHolder
	 * @throws Exception 异常
	 */
	public static TaskHolder at(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			String username
	) throws Exception {

		return at(clazz, init_map, username, 0, null);
	}

	/**
	 * 生成新Holder 0
	 * 简化模式 A-3
	 * 使用场景：任意
	 *
	 * @param clazz
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public static TaskHolder at(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map
	) throws Exception {

		return at(clazz, init_map, null, 0, null);
	}

	/**
	 * 生成Holder
	 * 任务执行过程中调用
	 * 新 holder 的 class 与原 holder 的 class 相同
	 * @param init_map
	 * @return
	 * @throws Exception
	 */
	public TaskHolder goon(
			Map<String, Object> init_map
	) throws Exception {

		return goon(this.getClass(), init_map);
	}

	/**
	 * 生成Holder
	 * 任务执行过程中调用
	 *
	 * @param clazz 新holder的class
	 * @param init_map 初始参数表
	 * @return
	 * @throws Exception
	 */
	public TaskHolder goon(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map
	) throws Exception {

		return goon(clazz, init_map, null);
	}

	/**
	 * 生成Holder
	 * 任务执行过程中调用
	 *
	 * @param clazz 新holder class
	 * @param init_map 初始参数表
	 * @param priority 优先级
	 * @return
	 * @throws Exception
	 */
	public TaskHolder goon(
			Class<? extends ChromeTask> clazz,
			Map<String, Object> init_map,
			Priority priority
	) throws Exception {

		int step = 0;

		if(holder.step == 1 || holder.step< 0) {
			throw new TaskException.NoMoreStepException();
		} else {
			step = holder.step - 1;
		}

		return TemplateManager.getInstance().newHolder(holder, clazz, 0, init_map, getUsername(), step, priority);
	}
}
