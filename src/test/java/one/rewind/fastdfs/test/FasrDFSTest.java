package one.rewind.fastdfs.test;

import one.rewind.db.FastDFSAdapter;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FasrDFSTest {

	@Test
	public void test() throws Exception {

		ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue());

		List<String> list = new ArrayList<>();
		list.add("tmp/fdfs1.jpg");
		list.add("tmp/1.png");
		list.add("tmp/2.png");

		long t1 = System.currentTimeMillis();
		int cycle = 100;

		CountDownLatch countDownLatch = new CountDownLatch(list.size() * cycle);

		for(int i=0; i<cycle; i++) {
			for (String s :list) {
				executor.submit(()-> {

					try {

						File file = new File(s);
						FileInputStream inputStream = new FileInputStream(file);
						byte[] bytes = new byte[inputStream.available()];

						String[] sArray = FastDFSAdapter.getInstance().upload_file(bytes, null, null);
						System.out.println(sArray[0] + "->" + sArray[1]);

						boolean f = FastDFSAdapter.getInstance().deleteFile(sArray[0], sArray[1]);
						if(f) System.out.println("delete");


					} catch (Exception e) {
						e.printStackTrace();
					}

					countDownLatch.countDown();
				});
			}
		}

		countDownLatch.await();
		System.out.println(System.currentTimeMillis() - t1);

	}
}
