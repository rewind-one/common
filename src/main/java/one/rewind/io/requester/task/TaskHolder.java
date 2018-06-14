package one.rewind.io.requester.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Hold the door
 */
public class TaskHolder {

	String class_name;
	Map<String, Object> init_map;
	int step;

	/**
	 *
	 * @param class_name
	 * @param init_map
	 * @param step
	 */
	public TaskHolder(String class_name, Map<String, Object> init_map, int step) {
		this.class_name = class_name;
		this.init_map = init_map;
		this.step = step;
	}

	/**
	 *
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	public Task build() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		Class<?> threadClazz = Class.forName(class_name);

		Method method = threadClazz.getMethod("build", Map.class, int.class);

		SemiSerializableChromeTask task =
				(SemiSerializableChromeTask) method.invoke(null, init_map, step);

		return task;
	}
}
