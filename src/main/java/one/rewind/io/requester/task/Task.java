package one.rewind.io.requester.task;

import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.io.requester.callback.NextTaskGenerator;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.callback.TaskValidator;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.txt.ChineseChar;
import one.rewind.txt.StringUtil;
import one.rewind.txt.URLUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * HTTP 请求任务
 * @author karajan
 *
 */
public class Task<T extends Task> implements Comparable<Task> {

	/**
	 * 任务优先级
	 */
	public enum Priority {
		LOWEST,
		LOWER,
		LOW,
		MEDIUM,
		HIGH,
		HIGHER,
		HIGHEST
	}

	/**
	 * 任务的请求方法
	 */
	public static enum RequestMethod {
		GET,
		POST,
		HEAD,
		PUT,
		DELETE,
		TRACE,
		OPTIONS
	}

	/**
	 * 任务的额外标签
	 */
	public static enum Flag {
		PRE_PROC, // 是否进行预处理
		SHOOT_SCREEN,
		BUILD_DOM,
		SWITCH_PROXY
	}

	/**
	 * 相同fingerprint的最小请求间隔
	 */
	public static long MIN_INTERVAL = 0;

	// id
	public String id;

	// 优先级
	private Priority priority = Priority.MEDIUM;

	// 域名
	public String domain;

	// url
	public String url;

	// BasicRequester
	// 请求方法
	private RequestMethod request_method;

	// BasicRequester
	// 请求头
	private Map<String, String> headers;

	// BasicRequester
	// POST data
	private String post_data;

	// BasicRequester
	// Cookie 信息
	private String cookies;

	// BasicRequester
	// 引用URL
	private String ref;

	// 是否需要登录 --> 可以放在Holder中
	private boolean login_task = false;

	// 账户信息 --> 可以放在Holder中
	private String username;

	// Holder 信息
	public TaskHolder holder;

	// 代理信息
	private Proxy proxy;

	// 参数表
	private List<Flag> flags = new ArrayList<>();

	// BasicRequester
	// 请求过滤器
	private RequestFilter requestFilter;

	// BasicRequester
	// 响应过滤器
	private ResponseFilter responseFilter;

	// 返回对象
	private transient Response response = new Response();

	// 是否需要重试
	private transient boolean retry = false;

	// 重试次数
	private int retryCount = 0;

	// 任务指定步骤 当 step = 1 时 不生成下一步任务
	// step = 0 不进行任何限制
	private int step = 0;

	// 任务验证器
	public TaskValidator validator;

	// 采集成功后回调
	public List<TaskCallback<T>> doneCallbacks = new LinkedList<>();

	// 采集成功后回调，生成下一级任务
	public List<NextTaskGenerator<T>> nextTaskGenerators = new LinkedList<>();

	// 采集异常回调
	public List<TaskCallback<T>> exceptionCallbacks = new LinkedList<>();

	// 异常信息，不用记录多个
	public Throwable exception;

	// 创建时间
	private Date create_time = new Date();

	// 任务开始采集时间
	public Date start_time;

	// 总执行时间
	private long duration = 0;

	private Task() {}

	/**
	 * 简单 GET 请求
	 *
	 * @param url URL
	 * @throws MalformedURLException URL协议异常
	 * @throws URISyntaxException URL格式异常
	 */
	public Task(String url) throws MalformedURLException, URISyntaxException {
		this(url, null);
	}

	/**
	 * 简单 POST 请求
	 *
	 * @param url URL
	 * @param post_data PostData
	 * @throws MalformedURLException URL协议异常
	 * @throws URISyntaxException URL格式异常
	 */
	public Task(String url, String post_data) throws MalformedURLException, URISyntaxException {

		this(url, post_data, null, null);
	}

	/**
	 * 需要 Cookie 的 POST 请求
	 *
	 * @param url URL
	 * @param post_data PostData
	 * @param cookies Cookies
	 * @param ref Ref URL
	 * @throws MalformedURLException URL协议异常
	 * @throws URISyntaxException URL格式异常
	 */
	public Task(String url, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {

		this(url, null, post_data, cookies, ref);
	}

	/**
	 * 完整参数请求
	 *
	 * @param url URL
	 * @param headers Header
	 * @param post_data PostData
	 * @param cookies Cookies
	 * @param ref Ref URL
	 * @throws MalformedURLException URL协议异常
	 * @throws URISyntaxException URL格式异常
	 */
	public Task(String url, HashMap<String, String> headers, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {

		this.url = url;
		this.headers = headers;
		this.post_data = post_data;
		this.cookies = cookies;
		this.ref = ref;
		domain = URLUtil.getDomainName(url);

		this.response = new Response();

		// 此处生成ID
		this.id = StringUtil.MD5(url + "-" + post_data + "-" + cookies + "-" + System.currentTimeMillis());

		if (post_data != null && post_data.length() > 0) {
			this.request_method = RequestMethod.POST;
		}
		else {
			this.request_method = RequestMethod.GET;
		}
	}

	/**
	 * 仅对BasicRequester有效
	 * @return HTTP请求方法
	 */
	public String getRequestMethod() {
		return request_method.name();
	}

	/**
	 * 仅对BasicRequester有效
	 * @param request_method 请求方法
	 * @return Self
	 */
	public Task setRequestMethod(RequestMethod request_method) {
		this.request_method = request_method;
		return this;
	}

	/**
	 * 设定POST请求方法
	 * 仅对BasicRequester有效
	 * @return Self
	 */
	public Task setPost() {
		this.request_method = RequestMethod.POST;
		return this;
	}

	/**
	 * 设定PUT请求方法
	 * 仅对BasicRequester有效
	 * @return Self
	 */
	public Task setPut() {
		this.request_method = RequestMethod.PUT;
		return this;
	}

	/**
	 * 设定DELETE请求方法
	 * 仅对BasicRequester有效
	 * @return Self
	 */
	public Task setDelete() {
		this.request_method = RequestMethod.DELETE;
		return this;
	}

	/**
	 * 仅对BasicRequester有效
	 * @return HEADER
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * 仅对BasicRequester有效
	 * @param headers HEADER
	 */
	public Task setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 * 设定Header中特定键值对
	 * @param k key
	 * @param v value
	 * @return
	 */
	public Task addHeader(String k, String v) {
		if(headers == null) headers = new HashMap<>();
		headers.put(k, v);
		return this;
	}

	/**
	 * @param k key
	 * @return Header中 k 对应的值
	 */
	public Task removeHeader(String k) {
		if(headers == null) return this;
		headers.remove(k);
		return this;
	}

	/**
	 * 仅对BasicRequester有效
	 * @return post_data
	 */
	public String getPost_data() {
		return post_data;
	}

	/**
	 * @param cookies Cookies
	 * @return Self
	 */
	public Task setCookies(String cookies) {
		this.cookies = cookies;
		return this;
	}

	/**
	 * @return Cookies
	 */
	public String getCookies() {
		return cookies;
	}

	/**
	 * @param ref Ref URL
	 * @return Self
	 */
	public Task setRef(String ref) {
		this.ref = ref;
		return this;
	}

	/**
	 * 获取Reference
	 * @return Ref url
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * 获取Domain
	 * @return domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * 获取Proxy
	 * @return proxy
	 */
	public Proxy getProxy() {
		return proxy;
	}

	/**
	 * 设定Proxy
	 * @param proxy proxy
	 * @return Self
	 */
	public Task setProxy(Proxy proxy) {
		this.proxy = proxy;
		return this;
	}

	/**
	 * 设定登录任务标识
	 * ChromeAgent 专用
	 * @return Self
	 */
	public Task setLoginTask() {
		this.login_task = true;
		return this;
	}

	/**
	 * 判断是否为登录任务
	 * ChromeAgent 专用
	 * @return
	 */
	public boolean isLoginTask() {
		return this.login_task;
	}

	/**
	 * 设定用户名
	 * ChromeAgent 专用
	 * @param username 用户名
	 * @return Self
	 */
	public Task setUsername(String username) {
		this.username = username;
		this.login_task = true;
		return this;
	}

	/**
	 * 获取用户名
	 * ChromeAgent 专用
	 * @return 用户名
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设定请求过滤器
	 * ChromeAgent 专用
	 * TODO 在使用MITM代理时，报错 io.netty.util.IllegalReferenceCountException: refCnt: 0, increment: 1
	 * 使用 HttpFiltersSourceAdapter 则没有报错
	 * 有可能是littleproxy自身问题
	 * @param filter 请求过滤器
	 * @return Self
	 */
	public Task setRequestFilter(RequestFilter filter) {
		this.requestFilter = filter;
		return this;
	}

	/**
	 * 获取请求过滤器
	 * ChromeAgent 专用
	 * @return 请求过滤器
	 */
	public RequestFilter getRequestFilter() {
		return this.requestFilter;
	}

	/**
	 * 设定返回过滤器
	 * ChromeAgent 专用
	 * @param filter 返回过滤器
	 * @return Self
	 */
	public Task setResponseFilter(ResponseFilter filter) {
		this.responseFilter = filter;
		return this;
	}

	/**
	 * 获取返回过滤器
	 * ChromeAgent 专用
	 * @return 返回过滤器
	 */
	public ResponseFilter getResponseFilter() {
		return this.responseFilter;
	}

	/**
	 * 获得返回对象
	 * @return 返回对象
	 */
	public Response getResponse() {
		return response;
	}

	/**
	 * 设定前置处理
	 * @return Self
	 */
	public Task setPreProc() {
		flags.add(Flag.PRE_PROC);
		return this;
	}

	/**
	 * @return 是否前置处理
	 */
	public boolean preProc() {
		return flags.contains(Flag.PRE_PROC);
	}

	/**
	 * 设定切换代理
	 * @return Self
	 */
	public Task setSwitchProxy() {
		flags.add(Flag.SWITCH_PROXY);
		return this;
	}

	/**
	 * @return 是否切换代理
	 */
	public boolean switchProxy() {
		return flags.contains(Flag.SWITCH_PROXY);
	}

	/**
	 * 设定构建DOM
	 * @return Self
	 */
	public Task setBuildDom() {
		flags.add(Flag.BUILD_DOM);
		return this;
	}

	/**
	 * @return 是否构建DOM
	 */
	public boolean buildDom() {
		return flags.contains(Flag.BUILD_DOM);
	}

	/**
	 * 设定进行截屏
	 * @return Self
	 */
	public Task setShootScreen() {
		flags.contains(Flag.SHOOT_SCREEN);
		return this;
	}

	/**
	 * @return 是否进行截屏
	 */
	public boolean shootScreen() {
		return flags.contains(Flag.SHOOT_SCREEN);
	}

	/**
	 * 获取用时
	 * @return 任务执行时长
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * 设定用时
	 * @return Self
	 */
	public Task setDuration() {
		this.duration = System.currentTimeMillis() - this.start_time.getTime();
		return this;
	}

	/**
	 * 设定重试
	 * @return Self
	 */
	public Task setRetry() {
		this.retry = true;
		return this;
	}

	/**
	 * @return 任务是否需要重试
	 */
	public boolean needRetry() {
		return this.retry;
	}

	/**
	 * 设定最大执行步骤
	 * @param step 当前所剩执行步数
	 * @return Self
	 */
	public Task setStep(int step) {
		this.step = step;
		return this;
	}

	/**
	 * @return 当前所剩执行步数
	 */
	public int getStep() {
		return this.step;
	}

	/**
	 * 设置内容验证器
	 * @param validator 内容验证器
	 * @return Self
	 */
	public Task setValidator(TaskValidator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * 增加重试次数
	 * @return Self
	 */
	public Task addRetryCount() {
		this.retryCount ++;
		return this;
	}

	/**
	 * @return 重试次数
	 */
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * 获取任务指纹信息
	 * @return 指纹字串
	 */
	public String getFingerprint() {

		String src = "[" + domain + ":" + username + "] " + url;
		return StringUtil.MD5(src);
	}

	/**
	 * 设定优先级
	 * @param priority 优先级
	 * @return Self
	 */
	public Task setPriority(Priority priority) {
		this.priority = priority;
		return this;
	}

	/**
	 * 获取优先级
	 * @return 优先级
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * 优先级比较
	 *
	 * @param another 另外一个任务
	 * @return 排序
	 */
	public int compareTo(Task another) {

		final Priority me = this.getPriority();
		final Priority it = another.getPriority();
		if (me.ordinal() == it.ordinal()) {
			return this.create_time.compareTo(another.create_time);
		} else {
			return it.ordinal() - me.ordinal();
		}
	}

	/**
	 * 增加完成回调
	 * @param callback 完成回调
	 * @return Self
	 */
	public Task addDoneCallback(TaskCallback<T> callback) {
		if (this.doneCallbacks == null) this.doneCallbacks = new LinkedList<>();
		this.doneCallbacks.add(callback);
		return this;
	}

	/**
	 * 增加下一集任务生成器
	 * @param ntg 任务生成器
	 * @return Self
	 */
	public Task addNextTaskGenerator(NextTaskGenerator<T> ntg) {
		if (this.nextTaskGenerators == null) this.nextTaskGenerators = new LinkedList<>();
		this.nextTaskGenerators.add(ntg);
		return this;
	}

	/**
	 * 添加采集异常回调
	 * @param callback 异常回调
	 * @return Self
	 */
	public Task addExceptionCallback(TaskCallback<T> callback) {
		if (this.exceptionCallbacks == null) this.exceptionCallbacks = new LinkedList<>();
		this.exceptionCallbacks.add(callback);
		return this;
	}

	/**
	 * 返回当前的Holder
	 * @return
	 */
	public TaskHolder getHolder() {
		return holder;
	}

	/**
	 * 任务请求返回对象封装
	 */
	public class Response {

		// 返回Header
		private Map<String, List<String>> header;

		// 返回源代码
		private byte[] src;

		// 源代码编码
		private String encoding;

		// 返回文本内容
		private String text;

		// 返回Cookie
		private String cookies;

		// 变量集
		private Map<String, String> vars = new HashMap<>();

		// 标识符 是否task的action都已经执行完
		private boolean actionDone;

		// 界面截图
		private byte[] screenshot = null;

		// 返回DOM对象
		private Document doc = null;

		/**
		 * 获取Header
		 *
		 * @return Header
		 */
		public Map<String, List<String>> getHeader() {
			return header;
		}

		/**
		 * 设置Header
		 *
		 * @param header Header
		 */
		public void setHeader(Map<String, List<String>> header) {
			this.header = header;
		}

		/**
		 * 获取二进制字节数组
		 *
		 * @return 字节数组
		 */
		public byte[] getSrc() {
			return src;
		}

		/**
		 * 设置二进制字节数组
		 *
		 * @param src 字节数组
		 */
		public void setSrc(byte[] src) {
			this.src = src;
		}

		/**
		 * 获取编码
		 *
		 * @return 编码
		 */
		public String getEncoding() {
			return encoding;
		}

		/**
		 * 设置编码
		 *
		 * @param encoding 编码
		 */
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		/**
		 * 获取返回cookies
		 *
		 * @return Cookies
		 */
		public String getCookies() {
			return cookies;
		}

		/**
		 * 设置cookie
		 *
		 * @param cookies Cookies
		 */
		public void setCookies(String cookies) {
			this.cookies = cookies;
		}

		/**
		 * 判断 Task 中 Action 是否已经执行完毕
		 *
		 * @return Actions是否执行完毕
		 */
		public boolean isActionDone() {
			return actionDone;
		}

		/**
		 * @param actionDone Actions是否执行完毕
		 */
		public void setActionDone(boolean actionDone) {
			this.actionDone = actionDone;
		}

		/**
		 * @return 返回文本
		 */
		public String getText() {
			return text;
		}

		/**
		 * @return 截图字节数组
		 */
		public byte[] getScreenshot() {
			return screenshot;
		}

		/**
		 * @param screenshot 截图字节数组
		 */
		public void setScreenshot(byte[] screenshot) {
			this.screenshot = screenshot;
		}

		/**
		 * 设定返回变量键值，RequestFilter/ResponseFilter使用
		 * @param key key
		 * @param value value
		 */
		public void setVar(String key, String value) {
			vars.put(key, value);
		}

		/**
		 * 获取变量
		 * @param key key
		 * @return value
		 */
		public String getVar(String key) {
			return vars.get(key);
		}

		/**
		 * 获取整形变量
		 * @param key key
		 * @return value
		 */
		public int getIntVar(String key) {
			return Integer.parseInt(vars.get(key));
		}

		/**
		 * 判断 Response 是否为文本
		 *
		 * @return Response内容是否为文本
		 */
		public boolean isText() {

			if (header == null) return true;

			// 根据Content-Type进行判断
			if (header.get("Content-Type") != null) {

				//
				for (String item : header.get("Content-Type")) {

					if (
						// 非Javascript
							(item.contains("application")
									&& !item.contains("json")
									&& !item.contains("xml")
									&& !item.contains("x-javascript")
							)
									// office 文件
									|| item.contains("application") && item.contains("officedocument")
									// 视频
									|| item.contains("video")
									// 音频
									|| item.contains("audio")
									// 图片
									|| item.contains("image")
							) {
						return false;
					}
				}
			}

			return true;
		}

		/**
		 * 文本内容预处理
		 *
		 * @param input 原始文本
		 * @throws UnsupportedEncodingException 编码不支持
		 */
		public void setText(String input) throws UnsupportedEncodingException {

			this.text = input;

			if (preProc()) {

				try {
					text = StringEscapeUtils.unescapeHtml4(text);
					text = ChineseChar.unicode2utf8(text);
				} catch (Exception e) {
					e.printStackTrace();
				}

				/* src = ChineseChar.unescape(src); */
				text = ChineseChar.toDBC(text);
				/* text = ChineseChar.toSimp(text); */
				text = text.replaceAll("　+|	+| +| +", " ");
				text = text.replaceAll(">\\s+", ">");
				text = text.replaceAll("\\s+<", "<");
				text = text.replaceAll("(\r?\n)+", "\n");
				/* src = src.replaceAll("<!\\[CDATA\\[|\\]\\]>", ""); */

				text = url + "\n" + post_data + "\n" + text;
			}
		}

		/**
		 * 文本内容预处理
		 *
		 * @throws UnsupportedEncodingException 编码不支持
		 */
		public void setText() {
			String input = null;
			try {
				input = BasicRequester.autoDecode(src, encoding);
				setText(input);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 构建Document
		 * @throws UnsupportedEncodingException 编码不支持
		 */
		public void buildDom() throws UnsupportedEncodingException {
			if (src != null && src.length > 0) {
				doc = Jsoup.parse(BasicRequester.autoDecode(src, encoding));
			}
		}

		/**
		 * 获取Document
		 * @return Document
		 */
		public Document getDoc() {
			return doc;
		}
	}
}