package one.rewind.db.test;

import one.rewind.db.FastDFSAdapter;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FastDFSAdapterTest {

    /**
     *  测试上传速度
     * @throws Exception
     */
    @Test
    public void testFastDFSAdapterUpFile() throws Exception{

        ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue());

        List<String> fileList = new ArrayList<>();
        for( int i = 1 ; i< 7 ; i++ ){
            fileList.add("tmp/zbj-geetest-bg-windows/" + i + ".png");
        }

        long t1 = System.currentTimeMillis();
        int cycle = 1;
        CountDownLatch countDownLatch = new CountDownLatch(fileList.size() * cycle);

        for(int i=0; i<cycle; i++) {
            for (String s : fileList) {

                InputStream is =  new FileInputStream(s);// pathStr 文件路径
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                byte[] b = new byte[1024];
                int n;
                while ((n = is.read(b)) != -1) {
                    out.write(b, 0, n);
                }

                executor.submit(()-> {
                    try {

                        String[] src = FastDFSAdapter.getInstance().upload_file(b, null, null);
                        File csv = new File("tmp/test.csv"); // CSV数据文件
                        BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true)); // 附加
                        bw.write( src[0] + "," + src[1]); // 添加新的数据行
                        bw.newLine();
                        bw.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    countDownLatch.countDown();
                });
            }
        }

        countDownLatch.await();
        System.out.print(System.currentTimeMillis() - t1);
    }
}
