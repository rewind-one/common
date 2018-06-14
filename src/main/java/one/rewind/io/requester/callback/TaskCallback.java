package one.rewind.io.requester.callback;

import one.rewind.io.requester.task.Task;

public interface TaskCallback {
	void run(Task t) throws Exception;
}
