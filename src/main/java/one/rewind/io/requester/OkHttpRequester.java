package one.rewind.io.requester;

import okhttp3.*;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.*;

import java.net.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 基于OkHttp
 *
 * 基本HTTP/HTTPS内容请求器
 * @author gcy116149@gmail.com
 * @version 2018/07/30 20:43
 */
public class OkHttpRequester {

	private static final Logger logger = LogManager.getLogger(OkHttpRequester.class.getName());

	protected static OkHttpRequester instance;

	public static OkHttpClient client;

	public Request request;

	public Proxy proxy;

	boolean retry = false;

	/**
	 * 单例模式
	 * @return
	 */
	public static OkHttpRequester getInstance() {

		if (instance == null) {
			synchronized (BasicRequester.class) {
				if (instance == null) {
					instance = new OkHttpRequester();
				}
			}
		}

		return instance;
	}

	private OkHttpRequester() {
		client = new OkHttpClient();
	}

	public void setProxy(Proxy proxy) {

		this.proxy = proxy;

		OkHttpClient okHttpClient = new OkHttpClient.Builder()
				.readTimeout(BasicRequester.READ_TIMEOUT, TimeUnit.SECONDS)
				.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxy.host, proxy.port)))
				.proxyAuthenticator(new okhttp3.Authenticator() {
					@Nullable
					@Override
					public Request authenticate(Route route, Response response) throws IOException {
						if (response.request().header("Proxy-Authorization") != null) {
							// Give up, we've already failed to authenticate.
							return null;
						}

						String credential = Credentials.basic(proxy.username, proxy.password);
						return response.request().newBuilder()
								.header("Proxy-Authorization", credential)
								.build();
					}
				})
				.sslSocketFactory(createSSLSocketFactory())
				.build();

		client = okHttpClient;

	}

	/**
	 * 默认为 get方法
	 * 同步 提交任务
	 * @param task
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void submit(Task task) {

		if (task.getProxy() != null) {
			instance.setProxy(task.getProxy());
		}

		Map<String, String> headers = null;

		String method = task.getRequestMethod();

		// header 设置
		if (task.getHeaders() != null) {
			headers = task.getHeaders();
		}
		else {
			try {
				headers = BasicRequester.HeaderBuilder.build(task.getUrl(), null, null);
			} catch (URISyntaxException | MalformedURLException e) {
				logger.error(e);
			}
		}

		/*task.getResponse().setCookies(headers.get("Cookie"));
		task.getResponse().setHeader(Headers.of(headers).toMultimap());*/

		// 请求url  两种请求方式 get / post / put / delete
		// TODO post put delete 方式暂未测试
		if (method.toUpperCase().equals("POST")) {

			request = new Request.Builder()
					.headers(Headers.of(headers))
					.post(post(task.getUrl()))
					.url(task.getUrl())
					.build();

		} else if (method.toUpperCase().equals("GET")){
			request = new Request.Builder()
					.headers(Headers.of(headers))
					.url(task.getUrl())
					.build();

		} else if (method.toUpperCase().equals("PUT")) {
			request = new Request.Builder()
					.headers(Headers.of(headers))
					.put(put(task))
					.url(task.getUrl())
					.build();

		} else if (method.toUpperCase().equals("DELETE")) {
			request = new Request.Builder()
					.headers(Headers.of(headers))
					.delete()
					.url(task.getUrl())
					.build();
		}

		try (Response response = client.newCall(request).execute()) {

			// response.body 只能使用一次
			//String result = response.body().string();
			byte[] bytes = response.body().bytes();

			String result = new String(bytes);

			if (result.contains("Method Not Allowed")) {
				logger.error("{} {}", task.getRequestMethod(), task.getUrl());
			}
			else {

				try {
					pullResponse(task, bytes);
				} catch (Exception e) {
					e.printStackTrace();
				}

				taskCallBack(task);
				logger.info("Task Done {}", task.getUrl());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 异步
	 * @param task
	 * @param timeout
	 */
	public void submit(Task task, long timeout) {
		logger.info("start submit ...");
		client.newBuilder().connectTimeout(timeout,TimeUnit.SECONDS);

		Map<String, String> headers = null;

		// header 设置
		if (task.getHeaders() != null) {
			headers = task.getHeaders();
		}else {
			try {
				headers = BasicRequester.HeaderBuilder.build(task.getUrl(), null, null);
			} catch (URISyntaxException | MalformedURLException e) {
				e.printStackTrace();
			}
		}
		request = new Request.Builder()
				.headers(Headers.of(headers))
				.url(task.getUrl())
				.build();

		logger.info("start request ...");

		client.newCall(request)
				.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {

						logger.error("Task {} is ERROR {}", task.getUrl(), e);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {

						// response.body 只能使用一次
						//String result = response.body().string();
						byte[] bytes = response.body().bytes();

						String result = new String(bytes);

						if (result.contains("Method Not Allowed")) {

							logger.error("Task Methed is Error -> {}", task.getRequestMethod());

						} else {

							try {
								pullResponse(task, bytes);
							} catch (Exception e) {
								e.printStackTrace();
							}

							taskCallBack(task);
							logger.info("Task Done {}", task.getUrl());
						}
					}
				});
	}

	/**
	 *  将response填入task中，并做处理
	 * @param task
	 * @param bytes
	 * @throws Exception
	 */
	public void pullResponse(Task task, byte[] bytes) throws Exception {

		task.getResponse().setSrc(bytes);

		if (task.getResponse().isText()) {
			task.getResponse().setText();
		}

		// 判断重试
		if (task.getResponse().getText() != null) {
			handleRefreshRequest(task);
			if (retry) {
				submit(task);
			}
		}
	}

	/**
	 * 执行 doneCallBack
	 * @param task
	 */
	public void taskCallBack(Task<Task> task) {

		for(TaskCallback callback : task.doneCallbacks) {
			try {
				callback.run(task);
			} catch (Exception e) {
				logger.error("Error proc doneCallback, Task:{}. ", task.getUrl(), e);
			}
		}
	}

	/**
	 *  put
	 *  以二进制文件传输
	 * @param task
	 * @return
	 */
	public RequestBody put(Task<Task> task) {

		// 文件格式
		MediaType mediaType = MediaType.parse(task.getParamString("mediaType"));
		// 文件位置
		String localPath = task.getParamString("localPath");

		byte[] bytes = task.getParamString("bytes").getBytes();

		if (bytes == null || bytes.length <= 0) {

			File file = new File(localPath);

			ByteArrayOutputStream out = null;
			try {
				FileInputStream in = new FileInputStream(file);
				out = new ByteArrayOutputStream();
				byte[] b = new byte[1024];
				int i = 0;
				while ((i = in.read(b)) != -1) {

					out.write(b, 0, b.length);
				}
				out.close();
				in.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] s = out.toByteArray();

			return RequestBody.create(mediaType, s);
			//return RequestBody.create(mediaType, file);
		} else {
			return RequestBody.create(mediaType, bytes);
		}
	}

	/**
	 *  获取 post 的请求参数
	 * @param url
	 * @return
	 */
	public RequestBody post(String url) {

		// 创建表单
		FormBody.Builder bodyBuilder = new FormBody.Builder();

		if (url.contains("?")) {
			String param = url.split("\\?")[1];

			String[] params = param.split("&");
			for (int i = 0; i < params.length; i++) {
				if (params[i].equals("")) {
					continue;
				}
				String[] kv = params[i].split("=");
				bodyBuilder.add(kv[0], kv[1]);
			}
		}

		// 创建请求体对象
		return bodyBuilder.build();
	}
	/**
	 * 信任所有证书
	 * @return
	 */
	private static SSLSocketFactory createSSLSocketFactory() {
		SSLSocketFactory ssfFactory = null;

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null,  new TrustManager[] { new TrustAllCerts() }, new SecureRandom());

			ssfFactory = sc.getSocketFactory();
		} catch (Exception e) {
		}

		return ssfFactory;
	}

	private static class TrustAllCerts implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
	}

	private static class TrustAllHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	/**
	 * 处理内容跳转页面
	 * @param task
	 * @throws SocketTimeoutException
	 * @throws IOException
	 * @throws Exception
	 */
	private void handleRefreshRequest(Task task) throws SocketTimeoutException, IOException, Exception {

		Pattern p = Pattern.compile("(?is)<META HTTP-EQUIV=REFRESH CONTENT=['\"]\\d+;URL=(?<T>[^>]+?)['\"]>");
		Matcher m = p.matcher(task.getResponse().getText());

		if(m.find()){
			task.setUrl(m.group("T"));
			retry = true;
		}
	}

	/**
	 * 解压缩GZIP输入流
	 *
	 * @param input
	 * @return
	 */
	public static InputStream decompress_stream(InputStream input) {

		// 使用 PushbackInputStream 进行预查
		PushbackInputStream pb = new PushbackInputStream(input, 2);

		byte[] signature = new byte[2];

		try {
			// 读取 signature
			pb.read(signature);
			// 放回 signature
			pb.unread(signature);

		} catch (IOException e) {
			logger.warn(e.toString());
		}

		if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b)

			try {
				return new GZIPInputStream(pb);
			} catch (IOException e) {
				logger.warn(e.toString());
				return pb;
			}

		else
			return pb;
	}

	/**
	 * 辅助方法 终端打印 Cookies
	 * @param cookies
	 */
	public static void printCookies(String cookies) {

		Map<String, String> map = new TreeMap<String, String>();

		if(cookies != null && cookies.length() > 0) {
			String[] cookie_items = cookies.split(";");
			for(String cookie_item : cookie_items) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {

					map.put(kv[0], kv[1]);
				}
			}
		}

		for(String k: map.keySet()){
			System.out.println(k + "=" + map.get(k) + "; ");
		}

	}


	/**
	 * 使用自定义的header
	 * @return
	 */
	public static HashMap<String, String> genHeaders() {

		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Host", "xueqiu.com");
		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
		// Accept-Encoding 不能为gzip
		headers.put("Accept-Encoding", "*");
		headers.put("Accept-Charset", "utf-8,gb2312;q=0.8,*;q=0.8");
		headers.put("Cache-Control", "max-age=0");
		headers.put("Connection", "keep-alive");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("Pragma", "no-cache");
		headers.put("Cookie", "s=eq12h2qes7; _ga=GA1.2.1912659196.1531978729; _gid=GA1.2.433629090.1531978729; device_id=fd6e2ca5e2d7506846026f2de503b50e; xqat=114d0743c7a8bd3a2c431f898c893342ab6914ea; bid=ddcf77e3808489ee6c12a105113ea8a8_jjs57zag; __lnkrntdmcvrd=-1; xq_a_token.sig=3dXmfOS3uyMy7b17jgoYQ4gPMMI; xq_r_token.sig=6hcU3ekqyYuzz6nNFrMGDWyt4aU; Hm_lvt_1db88642e346389874251b5a1eded6e3=1531978728,1532066349,1532090836; xq_a_token=114d0743c7a8bd3a2c431f898c893342ab6914ea; xq_r_token=ae3083a4b927e33b0894e91803036e9b5cb8e75d; xq_token_expire=Tue%20Aug%2014%202018%2020%3A47%3A40%20GMT%2B0800%20(CST); xq_is_login=1; u=9132619086; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1532090898");
		return headers;

	}

	/**
	 *  测试
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {

		OkHttpRequester example = OkHttpRequester.getInstance();
		Proxy proxy = new ProxyImpl("118.190.44.184",59998,"tfelab","TfeLAB2@15");
		example.setProxy(proxy);

		Task task = new Task("https://xueqiu.com/u/8719450803");

		task.setHeaders(genHeaders());

		example.submit(task);
		System.out.println(new String(task.getResponse().getSrc(), "UTF-8"));
		//((ResponseBody)response).string();
		//System.out.println(JSON.toPrettyJson(response));


	}
}
