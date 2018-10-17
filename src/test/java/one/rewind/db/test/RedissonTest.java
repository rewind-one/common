package one.rewind.db.test;

import org.redisson.api.RBlockingQueue;

import java.util.HashMap;
import java.util.Map;

import static one.rewind.db.RedissonAdapter.redisson;

public class RedissonTest extends Thread {
	
	public void run() {
		
		RBlockingQueue<Map<String, String>> taskQueue = redisson.getBlockingQueue("crawler-tasks");
		
		while (true) {
			try {
				Map<String, String> map2 = taskQueue.take();
				System.out.println(map2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args){
		
		RBlockingQueue<Map<String, String>> taskQueue = redisson.getBlockingQueue("crawler-tasks");
		
		for(int i=0; i<10000; i++){
			Map<String, String> map = new HashMap<String, String>();
			map.put("1", String.valueOf(i));
			System.err.println(map);
			taskQueue.add(map);
		}
		

		RedissonTest rt = new RedissonTest();
		rt.start();
	}
}
