package one.rewind.test;

import com.google.common.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.*;

public class ListenableFutureTest {

	public static final Logger logger = LogManager.getLogger(ListenableFutureTest.class.getName());

	// Executor Queue
	LinkedBlockingQueue queue = new LinkedBlockingQueue<Runnable>();

	// Executor
	ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);

	ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);

	public ListenableFutureTest() {

		for( int i=0; i<1; i++ ){
			test(i);
		}
	}

	public void test(int i) {

		logger.info("{} begin test:{}.", Thread.currentThread().getName(), i);

		ListenableFuture<Void> initFuture = executorService.submit(new TestCall());

		Futures.addCallback(initFuture, new FutureCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				logger.info("{} success.", Thread.currentThread().getName());
				System.err.println("onSuccess end.");
			}

			@Override
			public void onFailure(Throwable t) {
				logger.error(t);
				System.err.println("onFailure end.");
			}

		}, executorService);

		try {

			initFuture.get(2000L, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			logger.error(e);
		}
		catch (ExecutionException e) {
			// 可以不去处理
			logger.error(e);
		}
		catch (TimeoutException e) {
			// 此处异常不用处理，直接停止future
			logger.error(e);
			initFuture.cancel(true);
		}

		System.err.println("Func end.");

		/*try {

			Thread.sleep(2000L);

			initFuture.get(2000L, TimeUnit.MILLISECONDS);
		}
		// 未执行完中断
		catch (InterruptedException | TimeoutException e) {
			logger.error(e);
		}
		// 执行异常
		catch (ExecutionException e) {
			logger.error(e.getCause());
		}*/
	}

	/**
	 *
	 */
	public class TestCall implements Callable<Void> {

		long seed = 0;

		public TestCall() {
			seed = 1000 + new Random().nextInt(2000);
		}

		@Override
		public Void call() throws Exception {

			logger.info("{} try to sleep: {} @ {}", Thread.currentThread().getName(), seed, System.currentTimeMillis());
/*			if(seed < 2000) {
				throw new Exception("Test");
			}*/

			Thread.sleep(seed);
			logger.info("{} sleep done.", Thread.currentThread().getName());
			return null;
		}
	}

	public static void main(String[] args) {
		new ListenableFutureTest();
	}
}
