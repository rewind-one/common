package one.rewind.skeleton;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 简单队列消费者结构
 * @author scisaga@gmail.com
 * @param <E>
 */
public class Operator<E> extends Thread {
	
	public int queueLimit = 1000;
	
	public BlockingQueue<E> queue = new LinkedBlockingQueue<E>();

	public volatile boolean done = false;

	public Operator(int limit) {
		this.queueLimit = limit;
	}

	public Operator() {

	}

	/**
	 * @param entity
	 * @return
	 */
	public boolean add(E entity) {
		if (this.available()) {
			this.queue.add(entity);
			return true;
		} else {
			return false;
		}
	}

	/**
	 *
	 */
	public void proc(E entity) {

	}

	/**
	 * @return
	 */
	public boolean available() {
		if (this.queue.size() < this.queueLimit) {
			return true;
		}
		return false;
	}

	/**
	 *
	 */
	@Override
	public void run() {
		while (!this.done) {
			E entity = null;
			try {
				entity = this.queue.take();
				this.proc(entity);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
