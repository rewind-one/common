package one.rewind.io.requester.callback;

import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ProxyException;

public interface TaskValidator {

	/**
	 *
	 * @param task
	 * @throws ProxyException.Failed
	 * @throws AccountException.Failed
	 * @throws AccountException.Frozen
	 */
	void run(Task task) throws Exception;
}
