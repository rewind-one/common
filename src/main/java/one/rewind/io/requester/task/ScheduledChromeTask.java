package one.rewind.io.requester.task;

import it.sauronsoftware.cron4j.Scheduler;
import one.rewind.io.requester.chrome.ChromeDriverDistributor;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScheduledChromeTask implements JSONable<ScheduledChromeTask> {

	private static final Logger logger = LogManager.getLogger(ScheduledChromeTask.class.getName());

	public String id;

	public String cron;

	public transient Scheduler scheduler;

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

		scheduler = new Scheduler();

		scheduler.schedule(cron, ()->{

			try {

				ChromeDriverDistributor.getInstance().submit(holder);
			} catch (Exception e) {
				logger.error("Error submit scheduled task to distributor. ", e);
			}
		});
	}

	public void start() {
		scheduler.start();
	}

	public void stop() {
		scheduler.stop();
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
