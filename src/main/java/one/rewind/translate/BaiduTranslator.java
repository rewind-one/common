package one.rewind.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.util.Configs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 
 * @author zhuhuihua
 *
 */
public class BaiduTranslator {

	private static final Logger logger = LogManager.getLogger(BaiduTranslator.class.getSimpleName());

	private static final String UTF8 = "utf-8";
	
	private static final String SOURCE_LANGUAGE = "jp";
	private static final String TARGET_LANGUAGE = "zh";

	// 申请者开发者id，实际使用时请修改成开发者自己的appid
	private static String appId = "";

	// 申请成功后的证书token，实际使用时请修改成开发者自己的token
	private static String token = "";

	private static final String url = "http://api.fanyi.baidu.com/api/trans/vip/translate";

	// 随机数，用于生成md5值，开发者使用时请激活下边第四行代码
	private static final Random random = new Random();

	public String translate(String source, String from, String to) {

		try {

			appId = Configs.getConfig(BaiduTranslator.class).getString("baiduTranslate.appId");
			token = Configs.getConfig(BaiduTranslator.class).getString("baiduTranslate.token");
			// 用于md5加密
			int salt = random.nextInt(10000);
			// 本演示使用指定的随机数为1435660288
			// int salt = 1435660288;

			// 对appId+源文+随机数+token计算md5值
			StringBuilder md5String = new StringBuilder();
			md5String.append(appId).append(source).append(salt).append(token);
			String md5 = DigestUtils.md5Hex(md5String.toString());

			// 使用Post方式，组装参数
			HttpPost httpost = new HttpPost(url);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("q", source));
			nvps.add(new BasicNameValuePair("from", from));
			nvps.add(new BasicNameValuePair("to", to));
			nvps.add(new BasicNameValuePair("appid", appId));
			nvps.add(new BasicNameValuePair("salt", String.valueOf(salt)));
			nvps.add(new BasicNameValuePair("sign", md5));
			httpost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

			// 创建httpclient链接，并执行
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(httpost);

			// 对于返回实体进行解析
			HttpEntity entity = response.getEntity();
			InputStream returnStream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(returnStream, UTF8));
			StringBuilder result = new StringBuilder();
			String str = null;
			while ((str = reader.readLine()) != null) {
				result.append(str).append("\n");
			}

			// 转化为json对象，注：Json解析的jar包可选其它
			// JSON.
			JsonNode params = new ObjectMapper().readTree(result.toString());
			System.err.println(params.toString());
			// JSONObject resultJson = new JSONObject(result.toString());

			// 开发者自行处理错误，本示例失败返回为null

			JsonNode error_node = params.get("error_code");
			if (error_node != null) {

				System.out.println("出错代码:" + error_node);
				System.out.println("出错信息:" + params.get("error_msg").asText());
				return null;
			}

			// 获取返回翻译结果
			JsonNode text_node = params.get("trans_result");
			if (text_node != null) {

				JsonNode dstNode = text_node.findValue("dst");
				String text = dstNode.asText();
				System.err.println(text);
				text = URLDecoder.decode(text, UTF8);
				return text;
			}

		} catch (Exception e) {
			
			logger.error(e);
		}

		return null;

	}

	/**
	 * @param source
	 * @return
	 * 调用此静态方法进行翻译
	 */
	public static String translateToEn(String source){
		
		// ApplicationContext container = new
		// FileSystemXmlApplicationContext("src//spring//resource//baidu.xml");
		BaiduTranslator baidu = new BaiduTranslator(); // container.getBean("baidu");

		String result = null;
		try {
			
			result = baidu.translate(source, SOURCE_LANGUAGE, TARGET_LANGUAGE);

		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return result;
	}
}
