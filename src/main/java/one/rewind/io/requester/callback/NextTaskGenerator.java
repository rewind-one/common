package one.rewind.io.requester.callback;

import one.rewind.io.requester.task.Task;

import java.util.List;

public interface NextTaskGenerator<T extends Task>  {
	void run(T t, List<T> nts) throws Exception;
}
