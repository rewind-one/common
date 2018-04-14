/**
 * NotifierTest.java
 * @author karajan
 * @date 下午2:43:14
 * 
 */
package one.rewind.io.test;

import org.junit.Test;
import one.rewind.io.EmailSender;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

/**
 * @author karajan
 * @date 2015年2月25日 下午2:43:14
 * 
 */
public class NotifierTest {

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

}
