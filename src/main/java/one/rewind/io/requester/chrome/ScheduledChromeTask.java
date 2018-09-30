package one.rewind.io.requester.chrome;

import one.rewind.io.requester.task.ScheduledTask;
import one.rewind.io.requester.task.TaskHolder;

public class ScheduledChromeTask extends ScheduledTask {

	public void run() {

		try {

			TaskHolder new_holder = ChromeTaskFactory.getInstance().newHolder(holder);
			new_holder.scheduled_task_id = this.id;
			ChromeDistributor.getInstance().submit(holder);

		} catch (Exception e) {
			logger.error("Error submit scheduled task to distributor. ", e);
		}
	}

}
