package one.rewind.io.requester.callback;

import one.rewind.io.requester.task.Task;

public interface TaskCallback<T extends Task> {
	void run(T t) throws Exception;
}
