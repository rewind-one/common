package one.rewind.io.requester.chrome;

import it.sauronsoftware.cron4j.Scheduler;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.task.ChromeTaskHolder;
import one.rewind.io.requester.task.ScheduledChromeTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static one.rewind.io.requester.chrome.ChromeDriverDistributor.LOCAL_IP;

/**
 * 调度器
 */
public class ChromeTaskScheduler {

	private static final Logger logger = LogManager.getLogger(BasicRequester.class.getName());

	protected static ChromeTaskScheduler instance;

	public static ChromeTaskScheduler getInstance() {

		if (instance == null) {
			synchronized (BasicRequester.class) {
				if (instance == null) {
					instance = new ChromeTaskScheduler();
				}
			}
		}

		return instance;
	}

	public Scheduler scheduler;

	/**
	 * 使用Redis保存Task
	 */
	//public RMap<String, ScheduledChromeTask> scheduledTasks;
	public ConcurrentHashMap<String, ScheduledChromeTask> scheduledTasks = new ConcurrentHashMap<>();

	/**
	 *
	 */
	private ChromeTaskScheduler() {

		//scheduledTasks = RedissonAdapter.redisson.getMap("scheduled-tasks");

		// 重新调度周期性任务
		this.scheduler = new Scheduler();
		scheduler.start();

		/*for(ScheduledChromeTask task : scheduledTasks.values()) {
			try {
				this.schedule(task);
			} catch (Exception e) {
				logger.error("Error schedule task: {}. ", task.toJSON(), e);
			}
		}*/
	}

	/**
	 *
	 * @param id
	 * @throws Exception
	 */
	public void degenerate(String id) throws Exception {

		if(!scheduledTasks.containsKey(id)) {

			throw new Exception("No such id:" + id + ".");
		}

		ScheduledChromeTask task = scheduledTasks.get(id);
		task.degenerate();
	}

	/**
	 *
	 * @param task
	 * @return
	 */
	public Map<String, Object> schedule(ScheduledChromeTask task) throws Exception {

		if(scheduledTasks.containsKey(task.id)) throw new Exception("Task:" + task.id + " already scheduled.");

		task.scheduleId = scheduler.schedule(task.cron, ()->{
			try {
				ChromeDriverDistributor.getInstance().submit(task.holder);
			} catch (Exception e) {
				logger.error("Error submit scheduled task to distributor. ", e);
			}
		});

		scheduledTasks.put(task.id, task);

		Map<String, Object> assignInfo = new HashMap<>();
		assignInfo.put("localIp", LOCAL_IP);
		assignInfo.put("domain", task.holder.domain);
		assignInfo.put("account", task.holder.username);
		assignInfo.put("id", task.id);

		return assignInfo;
	}

	/**
	 *
	 * @param holder
	 * @param cron
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> schedule(ChromeTaskHolder holder, String cron) throws Exception {

		ScheduledChromeTask task = new ScheduledChromeTask(holder, cron);
		return schedule(task);
	}

	/**
	 *
	 * @param holder
	 * @param crons
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> schedule(ChromeTaskHolder holder, List<String> crons) throws Exception {

		ScheduledChromeTask task = new ScheduledChromeTask(holder, crons);
		return schedule(task);
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public boolean registered(String id) {
		if(scheduledTasks.containsKey(id)) return true;
		return false;
	}

	/**
	 * 根据id 返回已scheduled task
	 * @param id
	 * @return
	 */
	public ScheduledChromeTask getScheduledTask(String id) {
		return scheduledTasks.get(id);
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public void unschedule(String id) throws Exception {

		if(!scheduledTasks.containsKey(id)) {

			throw new Exception("No such id:" + id + ".");
		}

		ScheduledChromeTask task = scheduledTasks.get(id);

		scheduler.deschedule(task.scheduleId);

		scheduledTasks.remove(id);
	}

	/**
	 *
	 * @return
	 */
	public Map<String, ?> getInfo() {
		return scheduledTasks;
	}
}
