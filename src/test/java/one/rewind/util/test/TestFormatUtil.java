package one.rewind.util.test;

import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.StringUtil;
import org.junit.Test;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.StringUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TestFormatUtil {
	
	@Test
	public void binaryId() {

		UUID uid = UUID.randomUUID();

		byte[] id = StringUtil.getIdAsByte(uid);
		String id1 = StringUtil.byteArrayToHex(id);
		byte[] id2 = StringUtil.hexStringToByteArray(id1);
		String id3 = StringUtil.byteArrayToHex(id2);

		assertEquals(id1, id3);
	}


	@Test
	public void test() throws ParseException {
		System.out.println(new Date("Jan 3, 2014 7:30:16"));
		System.out.println(DateFormatUtil.parseTime("2014.10.01"));
	}

	@Test
	public void testPage() {

		long total = 102;
		long limit = 10;

		System.err.println(total / limit + ( total % limit == 0 ? 0 : 1));
	}
}
