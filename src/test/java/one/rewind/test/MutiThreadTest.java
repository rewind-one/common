package one.rewind.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import one.rewind.io.requester.RestfulRequester;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author gaohui
 * @date 2016/1/15
 */
public class MutiThreadTest {
	
	private static final Logger logger = LogManager.getLogger(RestfulRequester.class.getSimpleName());

	@Test
	public void test() {
		System.out.println("----程序开始运行----");
		Date date1 = new Date();

		int taskSize = 10000;
		// 创建一个线程池
		ExecutorService pool = Executors.newFixedThreadPool(taskSize);
		// 创建多个有返回值的任务
		List<Future> list = new ArrayList<Future>();
		for (int i = 0; i < taskSize; i++) {
			try {
				Callable c = new MyCallable(i + " ");
				// 执行任务并获取Future对象
				Future f = pool.submit(c);
				// System.out.println(">>>" + f.get().toString());
				list.add(f);
			} catch (Exception e) {
				logger.error(e);
				continue;
			}
		}
		// 关闭线程池
		pool.shutdown();

		// 获取所有并发任务的运行结果
		for (Future f : list) {
			try {
				// 从Future对象上获取任务的返回值，并输出到控制台
				logger.info(">>>" + f.get().toString());
			} catch (InterruptedException e) {
				logger.error(e);
				continue;
			} catch (ExecutionException e) {
				logger.error(e);
				continue;
			}
		}

		Date date2 = new Date();
		logger.info("----程序结束运行----，程序运行时间【" + (date2.getTime() - date1.getTime()) + "毫秒】");
	}

}

class MyCallable implements Callable<Object> {
	private static final Logger logger = LogManager.getLogger(RestfulRequester.class.getSimpleName());
	private String taskNum;

	MyCallable(String taskNum) {
		this.taskNum = taskNum;
		//test.queryByGetTest();
	}

	public Object call() throws Exception {
		logger.info(">>>" + taskNum + "任务启动");
		Date dateTmp1 = new Date();
		Thread.sleep(1000);
		Date dateTmp2 = new Date();
		long time = dateTmp2.getTime() - dateTmp1.getTime();
		logger.info(">>>" + taskNum + "任务");
		return taskNum + "任务返回运行结果,当前任务时间【" + time + "毫秒】";
	}

}
