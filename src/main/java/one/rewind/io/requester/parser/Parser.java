package one.rewind.io.requester.parser;

import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import org.jsoup.nodes.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	public String src;
	public Document doc;

	public Parser() {}

	public Parser(String src, Document doc) {
		this.src = src;
	}

	/*private static <T> T cast(Object o, Class<T> clazz) {
		return clazz != null && clazz.isInstance(o) ? clazz.cast(o) : null;
	}*/

	public static Object cast(String str, String className) throws Exception {

		if(className.equals(String.class.getSimpleName())) {
			return str;
		}
		else if(className.equals(Integer.class.getSimpleName())) {
			return NumberFormatUtil.parseInt(str);
		}
		else if(className.equals(Float.class.getSimpleName())) {
			return NumberFormatUtil.parseFloat(str);
		}
		else if(className.equals(Double.class.getSimpleName())) {
			return NumberFormatUtil.parseDouble(str);
		}
		else if(className.equals(Date.class.getSimpleName())) {
			return DateFormatUtil.parseTime(str);
		}

		throw new Exception("Unknown class name: " + className);
	}

	/**
	 *
	 * @param mapper
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> parse(Mapper mapper) throws Exception {

		Map<String, Object> data = new HashMap<>();

		for(Field field : mapper.fields) {

			String found = null;

			// 正则表达式解析
			if(field.method == Field.Method.Reg) {

				if(this.src == null) throw new IllegalStateException("Source not set");

				Pattern pattern = Pattern.compile(field.path);
				Matcher matcher = pattern.matcher(src);

				if(matcher.find()) {
					if(matcher.group("T") != null || matcher.group("T").length() > 0) {
						found = matcher.group("T");
					} else {
						found = matcher.group();
					}
				}

			}
			// CSSPath 解析
			else if(field.method == Field.Method.CssPath) {

				if(this.doc == null) throw new IllegalStateException("Doc not set");

				found = doc.select(field.path).text();
			}

			for(Field.Replacement replacement : field.replacements) {
				found = found.replaceAll(replacement.find, replacement.replace);
			}

			data.put(field.name, cast(found, field.type));
		}

		return data;
	}


}
