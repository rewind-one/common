package one.rewind.io.requester.chrome;

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

	/**
	 *
	 */
	public ConcurrentHashMap<String, ScheduledChromeTask> scheduledTasks = new ConcurrentHashMap<>();

	/**
	 *
	 * @param task
	 * @return
	 */
	public Map<String, Object> schedule(ScheduledChromeTask task) throws Exception {

		if(scheduledTasks.contains(task.id)) throw new Exception("Task:" + task.id + " already scheduled.");

		scheduledTasks.put(task.id, task);
		task.start();

		Map<String, Object> assignInfo = new HashMap<>();
		assignInfo.put("localIp", LOCAL_IP);
		assignInfo.put("domain", task.holder.domain);
		assignInfo.put("account", task.holder.username);

		return assignInfo;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public void unschedule(String id) throws Exception {

		if(!scheduledTasks.contains(id)) {
			throw new Exception("No such id:" + id + ".");
		}

		scheduledTasks.get(id).stop();
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
