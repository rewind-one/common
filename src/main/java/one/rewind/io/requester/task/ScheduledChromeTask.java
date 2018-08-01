package one.rewind.io.requester.task;

import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class ScheduledChromeTask implements JSONable<ScheduledChromeTask> {

	private static final Logger logger = LogManager.getLogger(ScheduledChromeTask.class.getName());

	// holder.class_name 和 holder.init_map 定义
	public String id;

	// cron4j 给出
	public String scheduleId;

	// cron pattern
	public String cron;

	// 递减 cron pattern
	public List<String> crons;

	// 生成 holder
	public ChromeTaskHolder holder;

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
	public ScheduledChromeTask(ChromeTaskHolder holder, String cron) throws Exception {

		this.id = StringUtil.MD5(holder.class_name + "-" + JSON.toJson(holder.init_map));

		if(StringUtil.validCron(cron)) {
			this.cron = cron;
		} else {
			throw new Exception("Cron pattern invaild.");
		}

		this.holder = holder;
	}

	/**
	 *
	 * @param holder
	 * @param crons
	 * @throws Exception
	 */
	public ScheduledChromeTask(ChromeTaskHolder holder, List<String> crons) throws Exception {

		this.id = StringUtil.MD5(holder.class_name + "-" + JSON.toJson(holder.init_map));

		for (String cron_ : crons) {
			if (!StringUtil.validCron(cron_)) {
				throw new Exception("Cron pattern invaild.");
			}
		}

		if (crons.size() == 0) throw new Exception("Cron pattern invaild.");

		cron = crons.get(0);

		this.crons = crons;

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
