/**
 * EmailSenderTest.java
 * @author karajan
 * @date 下午2:43:14
 * 
 */
package one.rewind.io.test;

import one.rewind.io.server.Msg;
import one.rewind.io.server.MsgTransformer;
import org.junit.Test;
import one.rewind.io.EmailSender;
import spark.Request;
import spark.Response;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import static spark.Spark.get;
import static spark.Spark.port;

/**
 * @author karajan
 * @date 2015年2月25日 下午2:43:14
 * 
 */
public class EmailSenderTest {

	@Test
	public void testEmailSender() {
		
		try {
			EmailSender.getInstance().send("scisaga@qq.com", "这是一封测试邮件", "<h1>Hello!</h1>");
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testOverridePort() throws InterruptedException {


		get("/", (Request request, Response response) -> {
			return new Msg<>(Msg.SUCCESS);
		}, new MsgTransformer());

		port(8001);

		Thread.sleep(100000);
	}

}
