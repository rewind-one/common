package one.rewind.io.requester.callback;

import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;

import java.util.List;

public interface NextTaskGenerator<T extends Task>  {
	void run(T t, List<TaskHolder> nts) throws Exception;
}
