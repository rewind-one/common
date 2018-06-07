package one.rewind.txt;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateFormatUtil {

	public static DateTimeFormatter dfd = DateTimeFormat.forPattern("yyyy-MM-dd");
	public static DateTimeFormatter dff = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
	public static DateTimeFormatter dfm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
	public static DateTimeFormatter dft = DateTimeFormat.forPattern("HH:mm:ss");
	public static DateTimeFormatter dft1 = DateTimeFormat.forPattern("HH:mm");
	public static DateTimeFormatter dfn = DateTimeFormat.forPattern("yyyyMMdd");
	public static DateTimeFormatter dfn1 = DateTimeFormat.forPattern("dd-MM-yyyy");
	
	public static DateTimeFormatter dfd_en_1 = DateTimeFormat.forPattern("MMM dd, yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_2 = DateTimeFormat.forPattern("MMM dd, yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_11 = DateTimeFormat.forPattern("dd MMM, yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_21 = DateTimeFormat.forPattern("dd MMM, yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_12 = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_22 = DateTimeFormat.forPattern("dd MMM yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_3 = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_4 = DateTimeFormat.forPattern("MMM dd yy").withLocale(Locale.US);
	
	public static DateTimeFormatter dfd_en_5 = DateTimeFormat.forPattern("dd-MMM-yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_51 = DateTimeFormat.forPattern("dd-MMM-yyyy").withLocale(Locale.US);
	
	public static DateTimeFormatter dfd_en_6 = DateTimeFormat.forPattern("MM/yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_61 = DateTimeFormat.forPattern("MMM yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_62 = DateTimeFormat.forPattern("yyyy MMM").withLocale(Locale.US);


	/**
	 * 一般性日期字符串解析方法
	 * @param in 日期字串
	 * @return Date类型日期
	 * @throws ParseException
	 */
	@SuppressWarnings("deprecation")
	public static Date parseTime(String in) throws ParseException {
		
		if (in == null) {
			return new Date();
		}
		in = in.trim();
		
		String prefix = null;
		Pattern p = Pattern.compile("今天|昨天|前天|\\d+(个星期|天|分钟|小时)前");
		Matcher m = p.matcher(in);
		if(m.find()){
			prefix = m.group();
			in = in.replaceAll(prefix, "");
		}
		
		in = in.trim();
		Date date = new Date();
		
		String yyyyMMdd = Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH)+1) + "-" + Calendar.getInstance().get(Calendar.DATE);
		
		in = in.replaceAll("日", "")
				.replaceAll("年|月", "-")
				.replaceAll("/", "-")
				.replaceAll("\\.", "-")
				.replaceAll("T", " ").replaceAll("Z", "");

		// 以秒为单位
		if (in.matches("\\d{9,10}")) {
			return new Date(Long.parseLong(in + "000"));
		}
		// 以毫秒为单位
		else if (in.matches("\\d{12,13}")) {
			return new Date(Long.parseLong(in));
		}
		else if (in.matches("\\d{1,2}-\\d{1,2}-\\d+")) {
			return dfn1.parseDateTime(in).toDate();
		}
		// 默认格式
		else if (in.matches("[A-Za-z]{3,4} \\d{1,2}, \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} (AM|PM)")) {
			return new Date(in);
		}
		// yyyy-MM-dd HH:mm:ss
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}")) {
			return dff.parseDateTime(in).toDate();
		}
		// yyyy-MM-dd HH:mm
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}")) {
			return dfm.parseDateTime(in).toDate();
		}
		// yyyy-MM-dd
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2}")) {
			return dfd.parseDateTime(in).toDate();
		}
		// MM-dd
		else if (in.matches("\\d{1,2}-\\d{1,2}")) {
			return dfd.parseDateTime(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + '-' + in).toDate();
		}
		// MM-dd HH:mm
		else if (in.matches("\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}")) {
			return dfm.parseDateTime(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + '-' + in).toDate();
		}
		// HH:mm:ss
		else if (in.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
			
			return new Date(dff.parseDateTime(yyyyMMdd + " " + in).toDate().getTime() + getShiftValue(prefix));
		}
		// HH:mm
		else if (in.matches("\\d{1,2}:\\d{2}")) {
			return new Date(dfm.parseDateTime(yyyyMMdd + " " + in).toDate().getTime() + getShiftValue(prefix));
		}
		// yyyyMMdd
		else if (in.matches("\\d{8}")) {
			return dfn.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd, yyyy -- Mar 3, 2016
		else if(in.matches("\\w+ +\\d{1,2} *, +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_1.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd, yy -- Mar 3, 16
		else if(in.matches("\\w+ +\\d{1,2} *, +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_2.parseDateTime(in).toDate();
		}
		// 英文日期格式dd MMM, yyyy -- Mar 3, 2016
		else if(in.matches("\\d{1,2} +\\w+ *, +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_11.parseDateTime(in).toDate();
		}
		// 英文日期格式 dd MMM, yy -- Mar 3, 16
		else if(in.matches("\\d{1,2} +\\w+ *, +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_21.parseDateTime(in).toDate();
		}
		// 英文日期格式dd MMM, yyyy -- Mar 3, 2016
		else if(in.matches("\\d{1,2} +\\w+ +\\d{4}")){
			in = in.replaceAll(" +", " ");
			return dfd_en_12.parseDateTime(in).toDate();
		}
		// 英文日期格式 dd MMM, yy -- Mar 3, 16
		else if(in.matches("\\d{1,2} +\\w+ +\\d{2}")){
			in = in.replaceAll(" +", " ");
			return dfd_en_22.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd yyyy -- Mar 3, 2016
		else if(in.matches("\\w+ +\\d{1,2} +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_3.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd yy -- Mar 3, 16
		else if(in.matches("\\w+ +\\d{1,2} +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_4.parseDateTime(in).toDate();
		}
		// 英文日期格式01-Nov-15
		else if(in.matches("\\d{1,2}-\\w+-\\d{2}")){
			return dfd_en_5.parseDateTime(in).toDate();
		}
		// 英文日期格式01-Nov-2015
		else if(in.matches("\\d{1,2}-\\w+-\\d{4}")){
			return dfd_en_51.parseDateTime(in).toDate();
		}
		// 英文日期格式10/2016
		else if(in.matches("\\d{1,2}/-\\d{4}")){
			return dfd_en_6.parseDateTime(in).toDate();
		}
		// 英文日期格式June 2016
		else if(in.matches("\\w+ \\d{4}")){
			return dfd_en_61.parseDateTime(in).toDate();
		}
		// 英文日期格式2016 June
		else if(in.matches("\\d{4} \\w+")){
			return dfd_en_62.parseDateTime(in).toDate();
		}
		// 英文日期格式21 Oct
		else if(in.matches("\\d{1,2} \\w+")){
			return dfd_en_12.parseDateTime(in + " " + Calendar.getInstance().get(Calendar.YEAR)) .toDate();
		}
		// 英文日期格式Oct 21
		else if(in.matches("\\w+ \\d{1,2}")){
			return dfd_en_3.parseDateTime(in + " " + Calendar.getInstance().get(Calendar.YEAR)).toDate();
		}
		else if (prefix != null){
			return new Date(new Date().getTime() + getShiftValue(prefix));
		}
		// 不能解析的情况
		else {
			return date;
		}
	}
	
	/**
	 * 获得文本日期时间描述的偏移量
	 * @param prefix
	 * @return
	 */
	private static long getShiftValue(String prefix){
		long v = 0;
		if(prefix == null){
			
		} else if(prefix.equals("今天")){
			
		} else if (prefix.equals("昨天")){
			v = - 24 * 60 * 60 * 1000;
		} else if (prefix.equals("前天")){
			v = - 2 * 24 * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+天前")){
			int n = Integer.parseInt(prefix.replaceAll("天前", ""));
			v = - n * 24 * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+小时前")){
			int n = Integer.parseInt(prefix.replaceAll("小时前", ""));
			v = - n * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+分钟前")){
			int n = Integer.parseInt(prefix.replaceAll("分钟前", ""));
			v = - n * 60 * 1000;
		}else if (prefix.matches("\\d+个星期前")){
			int n = Integer.parseInt(prefix.replaceAll("个星期前", ""));
			v = - n * 7 * 24 * 60 * 60 * 1000;
		}
		
		return v;
	}


}
