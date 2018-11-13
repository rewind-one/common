package one.rewind.io.requester.parser;

import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	public String src;
	public Document doc;

	public Parser() {}

	public Parser(String src, Document doc) {
		this.src = src;
		this.doc = doc;
	}

	/*private static <T> T cast(Object o, Class<T> clazz) {
		return clazz != null && clazz.isInstance(o) ? clazz.cast(o) : null;
	}*/

	/**
	 * 类型转换方法
	 * @param str
	 * @param className
	 * @return
	 * @throws Exception
	 */
	public static Object cast(String str, String className) {

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

		return str;
	}

	/**
	 * 正则方式匹配字符串
	 * @param src
	 * @param path
	 * @param multi
	 * @return
	 */
	public static List<String> regMatch(String src, String path, boolean multi) {

		List<String> list = new ArrayList<>();

		Pattern pattern = Pattern.compile(path);
		Matcher matcher = pattern.matcher(src);

		while(matcher.find()) {

			if(matcher.group("T") != null || matcher.group("T").length() > 0) {
				list.add(matcher.group("T"));
			}
			else if(matcher.group().length() > 0) {
				list.add(matcher.group());
			}

			if(!multi) break;
		}

		return list;
	}

	/**
	 *
	 * @param doc
	 * @param path
	 * @param attribute
	 * @param multi
	 * @return
	 */
	public static List<String> cssMatch(Element doc, String path, String attribute, boolean multi) {

		List<String> list = new ArrayList<>();

		for(Element el : doc.select(path)) {
			// 获取元素的属性
			if(attribute != null) {
				list.add(el.attr(attribute));
			}
			// 获取元素的 html
			else {
				list.add(el.html());
			}

			if(!multi) break;
		}

		return list;
	}

	/**
	 *
	 * @param mapper
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> parse(Mapper mapper) throws Exception {

		if(this.src == null) throw new IllegalStateException("Source not set");

		String src = this.src;
		Element dom = this.doc;

		// A 需要对源数据进行预先截取
		if(mapper.path != null) {

			// A1 正则方式截取
			if(mapper.method == Field.Method.Reg) {

				for(String res : regMatch(src, mapper.path, false)) {
					src = res;
					break;
				}
			}
			// A2 使用 CssPath 方式进行截取
			else {

				if(this.doc == null) throw new IllegalStateException("Doc not set");

				dom = this.doc.select(mapper.path).get(0);
			}
		}

		if(src == null || src.length() == 0 || dom == null) throw new Exception("Mapper path invalid");

		if(mapper.fields.size() == 0) {
			TemplateManager.logger.warn("Mapper has no fields");
		}

		// 结果列表
		List<Map<String, Object>> data = new ArrayList<>();

		// 如果 mapper 是多值匹配方式
		// 每个field的匹配结果可能有多个，而这些结果数量不相同时，取最少的结果数量作为最终匹配结果的数量

		Map<String, List<Object>> founds = new HashMap<>();

		// 每一个 field 单独匹配
		for(Field field : mapper.fields) {

			founds.put(field.name, new ArrayList<>());

			// 正则表达式解析
			if(field.method == Field.Method.Reg) {

				regMatch(src, field.path, mapper.multi).forEach(res -> {

					for(Field.Replacement replacement : field.replacements) {
						res = res.replaceAll(replacement.find, replacement.replace);
					}

					founds.get(field.name).add(cast(res, field.type));
				});

			}
			// CSSPath 解析
			else if(field.method == Field.Method.CssPath) {

				if(this.doc == null) throw new IllegalStateException("Doc not set");

				cssMatch(dom, field.path, field.attribute, mapper.multi).forEach(res -> {

					for(Field.Replacement replacement : field.replacements) {
						res = res.replaceAll(replacement.find, replacement.replace);
					}

					founds.get(field.name).add(cast(res, field.type));
				});
			}
		}

		// 生成最小公共长度
		int common_length = Integer.MAX_VALUE;

		for(String name : founds.keySet()) {
			if(founds.get(name).size() < common_length) {
				common_length = founds.get(name).size();
			}
		}

		if(common_length == 0) throw new Exception("Match no data");

		// 生成data
		for(int i=0; i<common_length; i++) {

			Map<String, Object> item = new HashMap<>();

			for(String name : founds.keySet()) {
				item.put(name, founds.get(name).get(i));
			}

			data.add(item);
		}

		return data;
	}


}
