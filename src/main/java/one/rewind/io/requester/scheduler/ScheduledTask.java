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

	public TaskScheduler taskScheduler;

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
	ScheduledTask(TaskScheduler taskScheduler, TaskHolder holder, String cron) throws Exception {

		this(taskScheduler, holder, Arrays.asList(cron));
	}

	/**
	 * 初始化一个计划采集任务
	 * @param holder
	 * @param crons 递减 cron pattern
	 * @throws Exception
	 */
	ScheduledTask(TaskScheduler taskScheduler, TaskHolder holder, List<String> crons) throws Exception {

		this.taskScheduler = taskScheduler;

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
	 */
	public void degenerate() {

		try {

			int index = crons.indexOf(cron);
			if (index > -1 && index < crons.size() - 1) {
				cron = crons.get(index + 1);
			}

			taskScheduler.scheduler.reschedule(scheduleId, cron);

		} catch (Exception e) {
			logger.error("Error reschedule: {}", scheduleId);
		}
	}

	/**
	 *
	 */
	public void run() {

		try {

			TaskHolder new_holder = TemplateManager.getInstance().newHolder(holder);
			new_holder.scheduled_task_id = this.id;

			taskScheduler.distributor.submit(new_holder);

		} catch (Exception e) {
			logger.error("Error submit scheduled task to distributor. ", e);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {

		taskScheduler.unschedule(id);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

}
