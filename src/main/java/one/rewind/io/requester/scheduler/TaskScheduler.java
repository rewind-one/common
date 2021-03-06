package one.rewind.io.requester.scheduler;

import it.sauronsoftware.cron4j.Scheduler;
import one.rewind.io.requester.Distributor;
import one.rewind.io.requester.task.TaskHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static one.rewind.io.requester.chrome.ChromeDistributor.LOCAL_IP;

/**
 * 调度器
 */
public class TaskScheduler {

	private static final Logger logger = LogManager.getLogger(TaskScheduler.class.getName());

	public Distributor distributor;

	public Scheduler scheduler;

	/**
	 * 使用Redis保存Task
	 */
	//public RMap<String, ScheduledTask> scheduledTasks;
	public ConcurrentHashMap<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

	/**
	 *
	 * @param distributor
	 */
	public TaskScheduler(Distributor distributor) {

		this.distributor = distributor;

		//scheduledTasks = RedissonAdapter.redisson.getMap("scheduled-tasks");

		// 重新调度周期性任务
		this.scheduler = new Scheduler();
		scheduler.start();

		/*for(ScheduledTask task : scheduledTasks.values()) {
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

		ScheduledTask task = scheduledTasks.get(id);
		task.degenerate();
	}

	/**
	 *
	 * @param task
	 * @return
	 */
	private synchronized Distributor.SubmitInfo schedule(ScheduledTask task) throws Exception {

		if(scheduledTasks.containsKey(task.id)) throw new Exception("Task:" + task.id + " already scheduled.");

		task.scheduleId = scheduler.schedule(task.cron, ()->{
			task.run();
		});

		scheduledTasks.put(task.id, task);

		return new Distributor.SubmitInfo(LOCAL_IP, task.holder.domain, task.holder.username, task.id, null);
	}

	/**
	 *
	 * @param holder
	 * @param cron
	 * @return
	 * @throws Exception
	 */
	public Distributor.SubmitInfo schedule(TaskHolder holder, String cron) throws Exception {

		ScheduledTask task = new ScheduledTask(this, holder, cron);
		return schedule(task);
	}

	/**
	 *
	 * @param holder
	 * @param crons
	 * @return
	 * @throws Exception
	 */
	public Distributor.SubmitInfo schedule(TaskHolder holder, List<String> crons) throws Exception {

		ScheduledTask task = new ScheduledTask(this, holder, crons);
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
	public ScheduledTask getScheduledTask(String id) {
		return scheduledTasks.get(id);
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public synchronized void unschedule(String id) throws Exception {

		if(!scheduledTasks.containsKey(id)) {

			throw new Exception("No such id:" + id + ".");
		}

		ScheduledTask task = scheduledTasks.get(id);

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
