package one.rewind.io.requester.chrome;

import it.sauronsoftware.cron4j.Scheduler;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.task.ScheduledChromeTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
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
	 *
	 */
	public ConcurrentHashMap<String, ScheduledChromeTask> scheduledTasks = new ConcurrentHashMap<>();

	/**
	 *
	 */
	public ChromeTaskScheduler() {
		this.scheduler = new Scheduler();
		scheduler.start();
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
