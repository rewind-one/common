package one.rewind.io.requester.scheduler;

import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.parser.TemplateManager;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScheduledTask implements JSONable<ScheduledTask>, Runnable {

	public static final Logger logger = LogManager.getLogger(ScheduledTask.class.getName());

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
	public ScheduledTask() {}

	/**
	 * 初始化一个计划采集任务
	 * @param holder
	 * @param cron
	 * @throws Exception
	 */
	public ScheduledTask(TaskHolder holder, String cron) throws Exception {

		this(holder, Arrays.asList(cron));
	}

	/**
	 * 初始化一个计划采集任务
	 * @param holder
	 * @param crons 递减 cron pattern
	 * @throws Exception
	 */
	public ScheduledTask(TaskHolder holder, List<String> crons) throws Exception {

		this.id = holder.generateScheduledTaskId();

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
		return TaskScheduler.getInstance().schedule(this);
	}

	/**
	 *
	 */
	public void degenerate() {

		int index = crons.indexOf(cron);
		if(index > -1 && index < crons.size() - 1) {
			cron = crons.get(index + 1);
		}

		TaskScheduler.getInstance().scheduler.reschedule(scheduleId, cron);
	}

	/**
	 *
	 */
	public void run() {

		try {

			TaskHolder new_holder = TemplateManager.getInstance().newHolder(holder);
			new_holder.scheduled_task_id = this.id;

			// TODO
			ChromeDistributor.getInstance().submit(holder);

		} catch (Exception e) {
			logger.error("Error submit scheduled task to distributor. ", e);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {

		TaskScheduler.getInstance().unschedule(id);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
