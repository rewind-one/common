package one.rewind.db.test;

import one.rewind.db.FastDFSAdapter;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.*;

public class FastDFSAdapterTest {

	/**
	 *  测试上传功能及上传速度
	 * @throws Exception
	 */
	@Test
	public void testPut() throws Exception{

		ExecutorService executor = Executors.newFixedThreadPool(4);

		List<String> fileList = new ArrayList<>();
		for( int i=1 ; i<11 ; i++ ){
			fileList.add("tmp/" + i + ".jpg");
		}

		long t1 = System.currentTimeMillis();

		int cycle = 1;
		CountDownLatch countDownLatch = new CountDownLatch(fileList.size() * cycle);

		for(int i=0; i<cycle; i++) {

			for (String s : fileList) {

				byte[] src = FileUtil.readBytesFromFile(s);

				executor.submit(()-> {

					String[] info = new String[0];

					try {

						info = FastDFSAdapter.getInstance().put(src, "jpg", new LinkedHashMap<>());
						System.err.println(info[0] + "::" + info[1]); // 组名 + 路径

						countDownLatch.countDown();

					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				});
			}
		}

		countDownLatch.await();

		System.out.print(System.currentTimeMillis() - t1);
	}

	@Test
	public void testGet() throws InterruptedException {

		byte[] src = FastDFSAdapter.getInstance().get("group1", "M00/00/00/CgAAFlxiy8-ADpm4ABnN5J4Ccyc751.jpg");

		FileUtil.writeBytesToFile(src, "tmp/1_1.jpg");

	}

	@Test
	public void testGetStat() throws IOException {

		System.err.println(JSON.toPrettyJson(FastDFSAdapter.getInstance().getStat()));

	}
}
