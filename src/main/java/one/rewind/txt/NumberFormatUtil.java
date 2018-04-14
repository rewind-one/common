package one.rewind.txt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberFormatUtil {
	/**
	 *
	 * @param in
	 * @return
	 */
	public static double parseDouble(String in) {

		double v = 0;

		Map<String, Double> map = new HashMap<>();

		map.put("百", 100D);
		map.put("千", 1000D);
		map.put("万", 10000D);
		map.put("百万", 100 * 10000D);
		map.put("亿", 10000 * 10000D);
		map.put("K", 1000D);
		map.put("k", 1000D);
		map.put("M", 1000000D);
		map.put("m", 1000000D);
		map.put("G", 1000000000D);
		map.put("g", 1000000000D);
		map.put("T", 1000000000000D);
		map.put("t", 1000000000000D);

		List<Double> multis = new ArrayList<Double>();
		Pattern p = Pattern.compile("百|千|万|百万|亿|k|K|m|M|g|G|t|T");
		Matcher m = p.matcher(in);
		while(m.find()){
			multis.add(map.get(m.group()));
		}

		in = in.trim();
		boolean negative = false;
		if(in.length() > 1 && in.subSequence(0, 1).equals("-")) {
			negative = true;
		}

		in = in.replaceAll("百|千|万|百万|亿|k|K|m|M|g|G|t|T|,", "").replaceAll("\\+|-", "").trim();

		if(in.matches("(\\d+\\.)?\\d+")){
			v = Double.parseDouble(in);
			for(Double ms : multis){
				v *= ms;
			}
		}

		return negative ? -v : v;
	}

	/**
	 *
	 * @param in
	 * @return
	 */
	public static float parseFloat(String in){
		return (float) parseDouble(in);
	}

	/**
	 *
	 * @param in
	 * @return
	 */
	public static int parseInt(String in){
		return (int) parseDouble(in);
	}
}
