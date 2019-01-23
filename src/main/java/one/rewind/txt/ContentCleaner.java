package one.rewind.txt;

import com.google.common.collect.Sets;
import one.rewind.io.requester.parser.TemplateManager;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentCleaner {

	public static final Logger logger = LogManager.getLogger(ContentCleaner.class.getName());

	public interface CleanStep {

		String clean(String content);
	}

	public static List<CleanStep> cleanSteps = new ArrayList<>();

	public static void addCleanStep(CleanStep step) {
		cleanSteps.add(step);
	}

	/**
	 * 清洗数据，获得a，img地址，并且保留干净的a，img 标签
	 * @param in
	 * @param img_urls
	 * @return
	 */
	public static String clean(String in, List<String> img_urls){
		return clean(in, img_urls, null);
	}

	/**
	 *
	 * @param in
	 * @param img_urls
	 * @param url
	 * @return
	 */
	public static String clean(String in, List<String> img_urls, String url){

		try {
			for (CleanStep step : cleanSteps) {
				in = step.clean(in);
			}
		} catch (Exception e) {
			logger.error("Error exec clean step, {}, ", in, e);
		}

		in = in.replaceAll("(?si)<html>.+?<body>\\s+", "").replaceAll("(?si)\\s+</body>.+?</html>", "");

		in = in.replace("(?i)^<.+?>", "");
		in = in.replace("(?i)</.+?>$", "");

		String out = "<p>" + in + "</p>";

		// 去HTML注释
		out = out.replaceAll("<!--.*?-->", "");
		out = out.replaceAll("<?xml.*?>", "");
		out = out.replaceAll("(?i)<!--[if !IE]>.*?<![endif]-->", "");

		// 去掉<span>标签
		out = out.replaceAll("(?is)<span([^>]*)>|</span>|<wbr>", "");

		out = out.replaceAll("(?is)<div class=\"cnblogs_code\">.+?</div>", "");

		// 去掉隐藏的HTML对象
		out = out.replaceAll("<[^>]*?display:none.*?>.*?</.*?>", "");


		// 特殊转义符替换
		out = out.replaceAll("(&nbsp;?)+", " ");
		out = out.replaceAll("　+|	+| +| +", " "); // 将特殊空格转换为标准空格
		out = out.replaceAll("[\uE000-\uF8FF]", " "); // 将UTF-8编码的特殊空白符号转化为标准空格
		out = out.replaceAll("&lt;+", "<");
		out = out.replaceAll("&gt;+", ">");
		out = out.replaceAll("\\\\", "/");

		// 去掉特殊控制符
		char c1 = (char) 0;
		char c2 = (char) 31;
		out = out.replaceAll("[" + c1 + "-" + c2 + "]+", "");

		// 去掉多余的空白符
		out = out.replaceAll(">\\s+", ">");
		out = out.replaceAll("\\s+<", "<");

		// 去掉i标签
		out = out.replaceAll("<i.*?></i>", "");

		// 去掉<a>标签
		out = out.replaceAll("(?si)<a.*?>|</a>", "");

		// 删除javascript标记
		// TODO 此行代码应该下移
		out = out.replaceAll("javascript:.+?(?=['\" ])", "");

		// *****************************
		// 处理图片，获取需要下载的连接
		//    <img src="{binaryId}">
		//    <img src="data:image/...">

		// 清洗 img 标签，只保留 src 属性
		Matcher matcher = Pattern.compile("(?si)<img.*?>").matcher(out);

		List<String> imgs = new ArrayList<>();
		while(matcher.find()){

			// 获取 src属性
			String imgSrc = matcher.group().replaceAll("^.*?src=['\"]?", "")
					.replaceAll("[ \"'>].*?$", "");

			// 图片是Base64 Encode形式
			if(imgSrc.matches("^data:image/.+?")) {
				imgs.add("<img src=\"" + imgSrc + "\">");
			}
			// 如果 img 的 src长度小于等于10 则认为该图片无效
			else if(imgSrc.length() > 10) {

				// 完整路径
				if(imgSrc.matches("http.+?")) {
					// 不做处理
				}

				// 相对路径
				else {
					URL x = null;
					URL y = null;
					try {
						x = new URL(url);
						y = new URL(x, imgSrc);
						imgSrc = y.toString();
					} catch (MalformedURLException e) {
						// 不做处理
						//TODO
					}

				}

				img_urls.add(imgSrc);
				imgs.add("<img src=\"" + imgSrc + "\">");
			}
			else {
				imgs.add("");
			}
		}

		matcher = Pattern.compile("(?si)<img.*?>").matcher(out);
		int i = 0;
		while(matcher.find()){
			out = out.replace(matcher.group(), imgs.get(i));
			i++;
		}

		// 去掉分页特殊文字
		out = out.replaceAll("(?i)(上一页(\\d)*)|(下一页)", "");

		// 去掉图片题注
		out = out.replaceAll("(?i)<p([^>]*)>图\\s*\\d+.*?</p>", "");

		// 去掉数据资料来源说明
		out = out.replaceAll(
				"(?i)<p([^>]*)>(数据来源|资料来源).*?</p>", "");

		// 去掉无法识别的HTML标签
		out = out.replaceAll("(?si)document.write\\(.*?\\);?", ""); // zhuhuihua 2016/06/09
		out = out.replaceAll("(?si)<link.*?>.*?</link>", "");
		out = out.replaceAll("(?si)<script.*?>.*?</script>", "");
		out = out.replaceAll("(?si)<script.*?>|</script>", "");
		out = out.replaceAll("(?si)<style.*?>.*?</style>", "");
		out = out.replaceAll("(?si)<iframe.*?>.*?</iframe>", "");
		out = out.replaceAll("(?si)<form.*?>.*?</form>", "");
		out = out.replaceAll("(?si)<select.*?>.*?</select>", "");
		out = out.replaceAll("(?si)<input.*?>", "");
		out = out.replaceAll("(?si)<object.*?>", "");

		out = out.replaceAll("(?si)<div.*?>|</div>", "");
		out = out.replaceAll("(?si)<font.*?>|</font>", "");
		out = out.replaceAll("(?si)<center>|</center>", "");
		out = out.replaceAll("(?si)<section.*?>|</section>|</fieldset>|<fieldset.*?>", "");
		out = out.replaceAll("(?i)<o:p.*?>|</o:p.*?>", "");

		// 对 dd dl dt的处理
		out = out.replaceAll("(?si)<dd.*?>", "<dd>");
		out = out.replaceAll("(?si)<dl.*?>", "<dl>");
		out = out.replaceAll("(?si)<dt.*?>", "<dt>");
		out = out.replaceAll("(?si)<ul.*?>", "<ul>");
		out = out.replaceAll("(?si)<li.*?>", "<li>");
		out = out.replaceAll("(?si)<ol.*?>", "<ol>");

		//out = out.replaceAll("(?si)<section.*?>", "<section>");

		out = out.replaceAll("(?si)<h1.*?>", "<h1>");
		out = out.replaceAll("(?si)<h2.*?>", "<h2>");
		out = out.replaceAll("(?si)<h3.*?>", "<h3>");
		out = out.replaceAll("(?si)<h4.*?>", "<h4>");
		out = out.replaceAll("(?si)<h5.*?>", "<h5>");
		out = out.replaceAll("(?si)<h6.*?>", "<h6>");
		out = out.replaceAll("(?si)<h7.*?>", "<h7>");

		out = out.replaceAll("(?si)</h1>", "</h1>");
		out = out.replaceAll("(?si)</h2>", "</h2>");
		out = out.replaceAll("(?si)</h3>", "</h3>");
		out = out.replaceAll("(?si)</h4>", "</h4>");
		out = out.replaceAll("(?si)</h5>", "</h5>");
		out = out.replaceAll("(?si)</h6>", "</h6>");
		out = out.replaceAll("(?si)</h7>", "</h7>");


		out = out.replaceAll("(?si)</?em>", "");

		out = out.replaceAll("。+", "。");

		out = out.replaceAll("(?i)<wbr.*?>|<br.*?>", "</p><p>");

		// 去掉Table和p的样式 & 大小写转换
		out = out.replaceAll("(?si)<table.*?>", "<table>");
		out = out.replaceAll("(?si)</table>", "</table>");

		out = out.replaceAll("(?si)<tbody.*?>", "<tbody>");
		out = out.replaceAll("(?si)</tbody>", "</tbody>");

		out = out.replaceAll("(?si)<thead.*?>", "<thead>");
		out = out.replaceAll("(?si)</thead>", "</thead>");

		out = out.replaceAll("(?si)<col.*?>", "<col>");
		out = out.replaceAll("(?si)</col>", "</col>");

		out = out.replaceAll("(?si)<colgroup.*?>", "<colgroup>");
		out = out.replaceAll("(?si)</colgroup>", "</colgroup>");

		out = out.replaceAll("(?si)<tr.*?>", "<tr>");
		out = out.replaceAll("(?si)</tr>", "</tr>");

		out = out.replaceAll("(?si)<td.*?>", "<td>");
		out = out.replaceAll("(?si)</td>", "</td>");

		out = out.replaceAll("(?si)<th.*?>", "<th>");
		out = out.replaceAll("(?si)</th>", "</th>");

		out = out.replaceAll("(?si)<p.*?>", "<p>");
		out = out.replaceAll("(?si)</p>", "</p>");

		// 删除 qqmusic
		out = out.replaceAll("(?si)<qqmusic.*?>|</qqmusic>", "");


		// 去掉开头结尾的空白
		out = out.replaceAll("^ +| +$", "");

		// 合并嵌套<p> <b>标记
		out = out.replaceAll("(?si)(<p>[\\r\\n\\s ]*)+<p>", "<p>");
		out = out.replaceAll("(?si)(</p>[\\r\\n\\s ]*)+</p>", "</p>");

		out = out.replaceAll("(?si)(<b.*?>)+", "<b>");
		out = out.replaceAll("(?si)(</b>)+", "</b>");

		// 保留strong标签，标签大小写统一
		out = out.replaceAll("(?si)(<strong.*?>)+", "<strong>");
		out = out.replaceAll("(?i)<strong>", "<strong>");
		out = out.replaceAll("(?i)</strong>", "</strong>");

		// 特殊标点转换，全角 -> 半角
		out = out.replaceAll("．", ".");
		out = out.replaceAll("％", "%");
		out = out.replaceAll("﹐", "，");
		out = out.replaceAll("﹔", "；");
		out = out.replaceAll("。、", "。");

		// unicode编码字符
		out = out.replaceAll("&#[0-9]+;", "");

		// 中文段落中解决符号乱用
		out = out.replaceAll("(?<=[\u4E00-\u9FA5]) *& *(?=[\u4E00-\u9FA5])", "和"); // 解决&乱用
		out = out.replaceAll("(?<=[\u4E00-\u9FA5]) *[:：] *(?=[\u4E00-\u9FA5])", "："); // 冒号转换:->：
		out = out.replaceAll("(?<=[\u4E00-\u9FA5]) *[;；] *", "；");
		out = out.replaceAll("(?<=[\u4E00-\u9FA5]) *[\\?？] *", "？");
		out = StringEscapeUtils.unescapeHtml4(out);

		// 去掉空<p>标记
		out = out.replaceAll("<\\w+>[\\r\\n\\s ]*</\\w+>", "");
		out = out.replaceAll("<\\w+>[\\r\\n\\s ]*</\\w+>", "");
		out = out.replaceAll("<\\w+>[\\r\\n\\s ]*</\\w+>", "");

		/*out = out.replaceAll("(?<=[\u4E00-\u9FA5]) (?=[\u4E00-\u9FA5])", ""); // 中文间空格转化为逗号
		out = out.replaceAll(" (?=[\u4E00-\u9FA5])", ""); // 去掉中文前空格
		out = out.replaceAll("<p>[^\u4E00-\u9FA5]+?</p>", ""); // 删除没有中文的段落*/

		// <p> </p> 标记的补全
		out = out.replaceAll("(?<=[\u4E00-\u9FA5：])<p>(?=[\u4E00-\u9FA5])", "</p><p>"); // 段落标记的补全
		out = out.replaceAll("(?<=[\u4E00-\u9FA5：])</p>(?=[\u4E00-\u9FA5])", "</p><p>"); // 段落标记的补全

		// 去掉特殊无意义字符
		out = out.replaceAll("[—§№☆★○●◎⊙◇◆□■△▲※→←]", "");

		return out;
	}

	public static List<String> strToList(String src) {

		if(src == null) return null;
		List list =  Arrays.asList(src.split(",")).stream().map(el -> el.trim()).filter(el -> !el.equals(" ") && !el.equals("")).collect(Collectors.toList());
		if(list.size() == 0) list = null;
		return list;
	}

	/**
	 * 去除给定内容中的特殊字符
	 * @param in
	 * @return
	 */
	public static String specialCharCleaner(String in) {

		String[] specialChar = {
				"\\u0008", "\\u0009", "\\u000A", "\\u000B", "\\u000C", "\\u000D",
				"\\u0022", "\\u0027", "\\u005C", "\\u00A0", "\\u2028", "\\u2029", "\\uFEFF"
		};

		String reg = "[" + Arrays.asList(specialChar).stream().collect( Collectors.joining( "|" ) ) + "]";

		return in.replaceAll(reg, "");
	}

	/**
	 *
	 * @param els
	 * @return
	 */
	public static Set<String> getDistinctTags(List<Element> els) {

		Set<String> tags = new HashSet<>();

		for (Element el : els) {
			if (!tags.contains(el.tagName())) tags.add(el.tagName());
		}

		return tags;
	}

	/**
	 * 获取给定element下所有子孙elements
	 * @param el
	 * @return
	 */
	public static List<Element> getAllSubElements(Element el) {

		List<Element> els = new ArrayList<>();

		for (Element sel : el.children()) {
			els.add(sel);
			els.addAll(getAllSubElements(sel));
		}

		return els;
	}

	/**
	 * 根据给定element的sysle判断样式是否显示，若sysle包含(display:none;),判断element不显示，返回true，否则返回false
	 * @param element
	 * @return
	 */
	public static boolean cleanElementByDisplayNone(Element element) {
		String style = element.attr("style");
		if (style.contains("display:none;")) {
			return true;
		}
		return false;
	}

	/**
	 * 根据data_w和style判断element宽度，根据width:px/width:%/width:em判断，若小于给定值且大于0，则返回true，否则返回false
	 * @param element
	 * @param maxWidth
	 * @param maxWidthPercent
	 * @param maxEm
	 * @return
	 */
	public static boolean cleanElementBySmallWidth(Element element, int maxWidth, float maxWidthPercent, float maxEm) {

		String data_w = element.attr("data-w");
		String style = element.attr("style");

		int width;
		float widthPercent;
		float widthEm;

		//根据data_w的值判定width，小于给定值maxWidth则返回true,否则继续判断
		try {
			width = Integer.parseInt(data_w);
			if (width > 0 && width < maxWidth){
				return true;
			}
		}catch (Exception e){

		}
		//根据style的内容判定width，小于给定值maxWidth则返回true,否则继续判断
		try {
			width = getWidthOfStr(style);
			if (width > 0 && width < maxWidth){
				return true;
			}
		} catch (Exception e) {

		}
		//根据style的内容判定widthPercent，小于给定值maxWidthPercent则返回true,否则返回false
		try {
			widthPercent = getWidthPercentOfStr(style);
			if (widthPercent > 0 && widthPercent < maxWidthPercent){
				return true;
			}
		} catch (Exception e) {

		}
		//根据style的内容判定width的em比例，小于给定值maxEm则返回true,否则返回false
		try {
			widthEm = getWidthEmOfStr(style);
			if (widthEm > 0 && widthEm < maxEm){
				return true;
			}
		} catch (Exception e) {

		}

		return false;
	}

	/**
	 * 根据给定字符串正则匹配，匹配不到时返回0
	 * @param src
	 * @return
	 */
	public static int getWidthOfStr(String src) {
		src = ";" + src;
		String regex = ";width: (?<T>\\d+?)px";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(src);
		int width = 0;

		if (matcher.find()) {

			if (matcher.group("T") != null || matcher.group("T").length() > 0) {
				try {
					width = Integer.parseInt(matcher.group("T"));
				} catch (Exception e) {
					width = 0;
				}
			}
		}

		return width;
	}

	/**
	 * 根据给定字符串正则匹配，匹配不到时返回0
	 * @param src
	 * @return
	 */
	public static float getWidthPercentOfStr(String src) {
		src = ";" + src;
		String regex = ";width: (?<T>[0-9]*\\.?[0-9]+?)%";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(src);
		float width = 0;

		if (matcher.find()) {

			if (matcher.group("T") != null || matcher.group("T").length() > 0) {
				try {
					width = Float.parseFloat(matcher.group("T"));
				} catch (Exception e) {
					width = 0;
				}
			}
		}
		return width;
	}

	/**
	 * 根据给定字符串进行正则匹配，匹配不到时返回0
	 * @param src
	 * @return
	 */
	public static float getWidthEmOfStr(String src) {
		src = ";" + src;
		String regex = ";width: (?<T>[0-9]*\\.?[0-9]+?)em";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(src);
		float width = 0;

		if (matcher.find()) {

			if (matcher.group("T") != null || matcher.group("T").length() > 0) {
				try {
					width = Float.parseFloat(matcher.group("T"));
				} catch (Exception e) {
					width = 0;
				}
			}
		}

		return width;
	}

}
