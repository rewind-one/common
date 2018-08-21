package one.rewind.io.requester.task;

import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScheduledChromeTask implements JSONable<ScheduledChromeTask>, Runnable {

	private static final Logger logger = LogManager.getLogger(ScheduledChromeTask.class.getName());

	// holder.class_name 和 holder.vars 定义
	public String id;

	// cron4j 给出
	public String scheduleId;

	// cron pattern
	public String cron;

	// 递减 cron pattern
	public List<String> crons;

	// 生成 holder
	public TaskHolder holder;

	/**
	 *
	 */
	public ScheduledChromeTask() {}

	/**
	 *
	 * @param holder
	 * @param cron
	 * @throws Exception
	 */
	public ScheduledChromeTask(TaskHolder holder, String cron) throws Exception {

		this(holder, Arrays.asList(cron));
	}

	/**
	 *
	 * @param holder
	 * @param crons
	 * @throws Exception
	 */
	public ScheduledChromeTask(TaskHolder holder, List<String> crons) throws Exception {

		this.id = holder.generateScheduledChromeTaskId();

		for (String cron_ : crons) {
			if (!StringUtil.validCron(cron_)) {
				throw new Exception("Cron pattern invaild.");
			}
		}

		if (crons.size() == 0) throw new Exception("Cron pattern invalid.");

		cron = crons.get(0);

		if(crons.size() > 1) {
			this.crons = crons;
		}

		this.holder = holder;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> start() throws Exception {
		return ChromeTaskScheduler.getInstance().schedule(this);
	}

	/**
	 *
	 */
	public void degenerate() {

		int index = crons.indexOf(cron);
		if(index > -1 && index < crons.size() - 1) {
			cron = crons.get(index + 1);
		}

		ChromeTaskScheduler.getInstance().scheduler.reschedule(scheduleId, cron);
	}

	/**
	 *
	 */
	public void run() {

		try {
			TaskHolder new_holder = ChromeTaskFactory.getInstance().newHolder(holder);
			new_holder.scheduled_task_id = this.id;
			ChromeDriverDistributor.getInstance().submit(holder);
		} catch (Exception e) {
			logger.error("Error submit scheduled task to distributor. ", e);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {

		ChromeTaskScheduler.getInstance().unschedule(id);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
