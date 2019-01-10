package one.rewind.io.requester.parser;

import one.rewind.db.RedissonAdapter;
import one.rewind.io.requester.exception.TaskException;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.txt.ContentCleaner;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	public Task t;
	public String url;
	public String src;
	public Document doc;

	public Parser() {}

	public Parser(Task t) {
		this.t = t;
		this.url = t.url;
		this.src = t.getResponse().getText();
		this.doc = t.getResponse().getDoc();
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
		else if (className.equals(List.class.getSimpleName())) {
			return Arrays.asList(str);
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

		String src_hash = null;

		// A 需要对源数据进行预先截取
		if(mapper.path != null) {

			// A1 正则方式截取
			if(mapper.method == Field.Method.Reg) {

				for(String res : regMatch(src, mapper.path, false)) {
					src = res;
					src_hash = StringUtil.MD5(src);
					break;
				}
			}
			// A2 使用 CssPath 方式进行截取
			else {

				if(this.doc == null) throw new IllegalStateException("Doc not set");

				dom = this.doc.select(mapper.path).get(0);
				src_hash = StringUtil.MD5(dom.html());
			}
		}
		// A2.1 不需要对源数据进行预先截取
		else {
			src_hash = StringUtil.MD5(src);
		}

		if(src == null || src.length() == 0 || dom == null) throw new Exception("Mapper path invalid");

		// A3 对获取的 src / dom 进行去重判断
		// TODO t.holder.vars need to be LinkedHashMap
		int template_id = t.holder.template_id;
		String mapper_hash = mapper.getHash();
		RMap<String, String> rmap = RedissonAdapter.redisson.getMap("Template-" + template_id + "-Mapper-" + mapper_hash);

		if(rmap.containsValue(src_hash)) {
			//mapper内容不允许重复出现时，抛出异常
			//mapper内容允许重复出现时，跳过不再次处理,返回null
			if(mapper.forbidDuplicateContent) {
				throw new TaskException.DuplicateContentException();
			} else {
				return new ArrayList<>();
			}
		} else {
			rmap.put(StringUtil.MD5(JSON.toJson(new TreeMap<>(t.holder.vars))), src_hash);
		}

		if(mapper.fields.size() == 0) {
			TemplateManager.logger.warn("Mapper has no fields");
		}

		// B 生成结果列表
		List<Map<String, Object>> data = new ArrayList<>();

		// 如果 mapper 是多值匹配方式
		// 每个field的匹配结果可能有多个，而这些结果数量不相同时，取最少的结果数量作为最终匹配结果的数量

		Map<String, List<Object>> founds = new HashMap<>();

		// 每一个 field 单独匹配
		for(Field field : mapper.fields) {

			founds.put(field.name, new ArrayList<>());

			if (field.path != null) {
				// B1.1 正则表达式解析
				if (field.method == Field.Method.Reg) {

					regMatch(src, field.path, mapper.multi).forEach(res -> {

						for (Field.Replacement replacement : field.replacements) {
							res = res.replaceAll(replacement.find, replacement.replace);
						}

						founds.get(field.name).add(cast(res, field.type));
					});

				}
				// B1.2 CSSPath 解析
				else if (field.method == Field.Method.CssPath) {

					if (this.doc == null) throw new IllegalStateException("Doc not set");

					cssMatch(dom, field.path, field.attribute, mapper.multi).forEach(res -> {

						for (Field.Replacement replacement : field.replacements) {
							res = res.replaceAll(replacement.find, replacement.replace);
						}

						founds.get(field.name).add(cast(res, field.type));
					});
				}
			}
			// B2 path为null或path选择不出数据时
			if (founds.get(field.name).size() == 0) {

				// B2.1 defaultString有值时，使用defaultString填充
				if (field.defaultString != null) {

					founds.get(field.name).add(cast(field.defaultString, field.type));
				}
				// B2.2 defaultString没有设定时，且该Field不能为空时，抛出异常
				else if (!field.nullable) {

					throw new Exception("Field : [" + field.name + "] not allowed to be null.");
				}
			}
		}

		// 定义Mapper中Field的几种场景
		// 如果存在一个Field拿到 n 个结果，n是Mapper中所有Field拿到结果数量最大的那个
		// 如果存在另外一个Field 拿到 0 个结果，且这个Field不是nullable，此处应该抛出异常
		// 如果存在另外一个Field 拿到 m 个结果，且m < n 个，则自动忽略 第2个到第m个结果，认为拿到了n个一样的结果，相当于把第一个结果复制了n份
		// 通过这样处理，可认为此时所有的 非 nullable 的 Field 都拿到了 n 个结果
		// 将这些结果组合，相当于得到了 n 个map
		// 此时对每个map 再进行evalRule处理，抛出 js eval 过程中出现的异常，生成最终的map，传递到下一层处理
		// va --> 1 // 媒体名称
		// vb --> 6 // 文章名称
		// vc --> 6 // 文章摘要
		// vd --> 0 nullable

		// va --> 1 媒体名称 md5({{vb}}+"-"+{{va}}) --> Nashorn 加载自定义函数
		// vb --> 0 文章名称
		// vc --> 1 文章摘要
		// vd --> 1 nullable

		// C 对齐 获取的数据
		// C1 生成最大公共长度
		int maxLength = founds.values().stream().map(list -> list.size()).reduce(Integer::max).get();

		// C2 生成data
		for (int i = 0; i < maxLength; i++) {

			Map<String, Object> item = new HashMap<>();

			for (String name : founds.keySet()) {

				if(0 < founds.get(name).size() && founds.get(name).size() < maxLength) {
					item.put(name, founds.get(name).get(0));
				}
				else if (founds.get(name).size() == maxLength){
					item.put(name, founds.get(name).get(i));
				}

				//对 content 和 src 进行清洗
				if (name.equals("content")) {

					// TODO 如果图片要单独下载，并保存在小文件系统，何如？
					// 对 images src 进行修正
					String content = item.get(name).toString();

					List<String> img_urls = new ArrayList<>();
					content = ContentCleaner.clean(content, img_urls, url);

					item.put("content", content);
					item.put("images", img_urls);
				}
			}

			data.add(item);
		}

		// D 预定义规则运算
		for(Map<String, Object> item : data) {

			for(Field f : mapper.fields) {

				if(f.evalRule != null) {

					String rule = f.evalRule;

					for(String key : item.keySet()) {
						rule = rule.replaceAll("\\{\\{" + key + "\\}\\}", item.get(key).toString());
					}

					if(rule.contains("{{") && rule.contains("}}")) {
						throw new Exception("EvalRule not fully construct.");
					}

					item.put(f.name, Evaluator.getInstance().serialEval(rule).toString());
				}
			}
		}

		return data;
	}
}
