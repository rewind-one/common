package one.rewind.util.test;

import com.google.common.collect.ImmutableList;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import org.junit.Test;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class FormatUtilTest {

	@Test
	public void testDateFormat() {
		
		List<String> raw = ImmutableList.of(
			"2016-06-01 01:31:18",
			"2016-10-13 09:39",
			"5月18日 22:52",
			"2015年11月5日 17:27",
			"2016/12/31 2:47",
			"7.10.2016",
			"5月20日",
			"2015/12/21",
			"今天 01:08",
			"昨天 08:16",
			"10:05",
			"05-09",
			"1æœˆ19æ—¥ 15:14",
			"6天前",
			"昨天",
			"41分钟前",
			"11小时前",
			"2小时前",
			"August 3, 2016 ",
			"August 3, 16 ",
			"May 27 2016",
			"28 October 2016",
			"28 October, 2016",
			"01-Nov-2016",
			"10/2016",
			"2016-10-03 12:00:00",
			"June 2016",
			"2016 June",
			"31.10.2016",
			"2016-10-03",
			"21 Oct",
			"May 3"
		);
		
		raw.stream().forEach(s -> {
			System.out.print(s + "\t");
			try {
				DateFormat format =
			            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				
				System.out.println(format.format(DateFormatUtil.parseTime(s)));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
	}

	@Test
	public void testNumberFormat() {
		List<String> raw = ImmutableList.of(
			"1.2万",
			"32万+",
			"224万+",
			"5.5万+",
			"5323.4532435435435333333333333333333333333333333333333333333333333333333333333333333333333333333333333334",
			"0.9万",
			"9000亿",
			"0.06万万",
				"4T"
		);
		
		for(String s : raw){
			System.err.println(s + "\t");
			try {
				System.out.println(NumberFormatUtil.parseDouble(s));
				System.out.println(NumberFormatUtil.parseFloat(s));
				System.out.println(NumberFormatUtil.parseInt(s));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	@Test
	public void formatTest() {
		float i = (float) 10000;
		NumberFormat nf = new DecimalFormat("#.000000");
		System.err.println(nf.format(i));
	}
}
