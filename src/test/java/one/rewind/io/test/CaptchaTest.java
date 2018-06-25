package one.rewind.io.test;

import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.util.FileUtil;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CaptchaTest {

	@Test
	public void testGetCaptchaImage() throws MalformedURLException, URISyntaxException, InterruptedException {

		List<String> captchas = new ArrayList<>();

		while(captchas.size() < 448000) {

			List<Integer> pool = new ArrayList<>();

			for (int i = 48; i < 58; i++) {
				pool.add(i);
			}

			for (int i = 65; i < 91; i++) {
				pool.add(i);
			}

			String key = "";
			for (int i = 0; i < 4; i++) {
				int j = new Random().nextInt(pool.size());
				char c = (char) (int) pool.get(j);
				key += String.valueOf(c);
			}

			if (!captchas.contains(key)) {
				captchas.add(key);
			}

			if(captchas.size() % 1000 == 0) {
				System.err.println(captchas.size());
			}
		}

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				40,
				40,
				0, TimeUnit.MICROSECONDS,
				new LinkedBlockingQueue<>()
		);

		CountDownLatch downLatch = new CountDownLatch(captchas.size());

		for(String key : captchas) {

			executor.submit(() -> {

				try {

					String url = "http://47.93.180.1:8965/gate/sdyk-user/user/v200/imgByCode?code=" + key;
					Task t = new Task(url);
					t.setPost();

					BasicRequester.getInstance().submit(t);

					String base = t.getResponse().getText()
							.replaceAll("^.+?base64,", "")
							.replaceAll("\",\"imageCode.+?$", "");

					byte[] data = Base64.decodeBase64(base);

					FileUtil.writeBytesToFile(data, "tmp/captcha/" + key + ".jpg");

					downLatch.countDown();

				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		downLatch.await();
	}
}
