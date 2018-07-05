package one.rewind.io.requester.task;

import one.rewind.io.requester.chrome.ChromeTaskScheduler;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ScheduledChromeTask implements JSONable<ScheduledChromeTask> {

	private static final Logger logger = LogManager.getLogger(ScheduledChromeTask.class.getName());

	public String id;

	public String scheduleId;

	public String cron;

	public List<String> crons;

	public ChromeTaskHolder holder;

	/**
	 *
	 * @param holder
	 * @param cron
	 * @throws Exception
	 */
	public ScheduledChromeTask(ChromeTaskHolder holder, String cron) throws Exception {

		this.id = holder.id;

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

		this.id = holder.id;

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
	 */
	public void degenerate() {

		int index = crons.indexOf(cron);
		if(index > -1 && index < crons.size() - 1) {
			cron = crons.get(index + 1);
			System.err.println(cron);
		}

		ChromeTaskScheduler.getInstance().scheduler.reschedule(scheduleId, cron);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}


}
