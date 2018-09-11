package one.rewind.io.requester.task;

import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.db.DaoManager;
import one.rewind.io.requester.BasicRequester;
import one.rewind.io.requester.callback.NextTaskGenerator;
import one.rewind.io.requester.callback.TaskCallback;
import one.rewind.io.requester.callback.TaskValidator;
import one.rewind.json.JSON;
import one.rewind.txt.ChineseChar;
import one.rewind.txt.NumberFormatUtil;
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

	public enum Priority {
		LOWEST,
		LOWER,
		LOW,
		MEDIUM,
		HIGH,
		HIGHER,
		HIGHEST
	}

	public static enum RequestMethod {
		GET,
		POST,
		HEAD,
		PUT,
		DELETE,
		TRACE,
		OPTIONS
	}

	public static enum Flag {
		PRE_PROC,
		SHOOT_SCREEN,
		BUILD_DOM,
		SWITCH_PROXY
	}

	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	public String id;

	@DatabaseField(dataType = DataType.ENUM_INTEGER, width = 2, canBeNull = false)
	private Priority priority = Priority.MEDIUM;

	@DatabaseField(dataType = DataType.STRING, width = 4096)
	private String url;

	@DatabaseField(dataType = DataType.STRING, width = 16)
	private RequestMethod request_method;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Map<String, String> headers;

	@DatabaseField(dataType = DataType.LONG_STRING)
	private String post_data;

	@DatabaseField(dataType = DataType.STRING, width = 4096)
	private String cookies;

	@DatabaseField(dataType = DataType.STRING, width = 4096)
	private String ref;

	@DatabaseField(dataType = DataType.STRING, width = 256)
	private String domain;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Map<String, Object> params = new HashMap<>();

	@DatabaseField(dataType = DataType.STRING, width = 256)
	private String requester_class = BasicRequester.class.getSimpleName();

	@DatabaseField(dataType = DataType.BOOLEAN)
	private boolean login_task = false;

	// 账户信息
	@DatabaseField(dataType = DataType.STRING, width = 256)
	private String username;

	// 代理出口信息
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private one.rewind.io.requester.proxy.Proxy proxy;

	// 参数
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<Flag> flags = new ArrayList<>();

	// 请求过滤器
	private RequestFilter requestFilter;

	// 响应过滤器
	private ResponseFilter responseFilter;

	// 返回对象
	private transient Response response = new Response();

	// 记录参数
	@DatabaseField(dataType = DataType.DATE)
	private Date start_time;

	// 总执行时间
	@DatabaseField(dataType = DataType.LONG)
	private long duration = 0;

	// 是否需要重试
	private transient boolean retry = false;

	// 重试次数
	@DatabaseField(dataType = DataType.INTEGER)
	private int retryCount = 0;

	// 任务指定步骤 当 step = 1 时 不生成下一步任务
	// step = 0 不进行任何限制
	private int step = 0;

	public TaskValidator validator;

	// 采集后回调
	public List<TaskCallback<T>> doneCallbacks = new LinkedList<>();

	public List<NextTaskGenerator<T>> nextTaskGenerators = new LinkedList<>();

	// 采集异常回调
	public List<TaskCallback<T>> exceptionCallbacks = new LinkedList<>();

	// 异常记录
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private ArrayList<Throwable> exceptions = new ArrayList<>();

	// 创建时间
	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	private Date create_time = new Date();

	private boolean SAVE_SRC = true;

	public void setSaveSrc( boolean SAVE_SRC ){
		this.SAVE_SRC = SAVE_SRC;
	}

	public boolean getSaveSrc(){
		return this.SAVE_SRC;
	}

	private Task() {}

	/**
	 * 简单 GET 请求
	 *
	 * @param url url地址
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url) throws MalformedURLException, URISyntaxException {
		this(url, null);
	}

	/**
	 * 简单 POST 请求
	 *
	 * @param url       url 地址
	 * @param post_data post data 字符串格式
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String post_data) throws MalformedURLException, URISyntaxException {

		this(url, post_data, null, null);
	}

	/**
	 * 需要 Cookie 的 POST 请求
	 *
	 * @param url
	 * @param post_data
	 * @param cookies
	 * @param ref
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {

		this(url, null, post_data, cookies, ref);
	}

	/**
	 * 完整参数请求
	 *
	 * @param url
	 * @param headers
	 * @param post_data
	 * @param cookies
	 * @param ref
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, HashMap<String, String> headers, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {

		this.url = url;
		this.headers = headers;
		this.post_data = post_data;
		this.cookies = cookies;
		this.ref = ref;
		domain = URLUtil.getDomainName(url);

		this.response = new Response();
		this.id = StringUtil.MD5(url + "::" + post_data + "::" + cookies + "::" + System.currentTimeMillis());

		if (post_data != null && post_data.length() > 0) {
			this.request_method = RequestMethod.POST;
		} else {
			this.request_method = RequestMethod.GET;
		}
	}

	/**
	 * 获取ID
	 * @return
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 获取URL
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 设置URL
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 获取HTTP请求方法
	 * 仅对BasicRequester有效
	 * @return
	 */
	public String getRequestMethod() {
		return request_method.name();
	}

	/**
	 * 设定请求方法
	 * 仅对BasicRequester有效
	 * @param request_method
	 */
	public void setRequestMethod(RequestMethod request_method) {
		this.request_method = request_method;
	}

	/**
	 * 设定POST请求方法
	 * 仅对BasicRequester有效
	 */
	public void setPost() {
		this.request_method = RequestMethod.POST;
	}

	/**
	 * 设定PUT请求方法
	 * 仅对BasicRequester有效
	 */
	public void setPut() {
		this.request_method = RequestMethod.PUT;
	}

	/**
	 * 设定DELETE请求方法
	 * 仅对BasicRequester有效
	 */
	public void setDelete() {
		this.request_method = RequestMethod.DELETE;
	}

	/**
	 * 获取HEADER
	 * 仅对BasicRequester有效
	 * @return
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * 设定HEADER
	 * 仅对BasicRequester有效
	 * @param headers
	 */
	public Task setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 *
	 * @param k
	 * @param v
	 * @return
	 */
	public Task addHeader(String k, String v) {
		if(headers == null) headers = new HashMap<>();
		headers.put(k, v);
		return this;
	}

	/**
	 * 获取POST DATA
	 * 仅对BasicRequester有效
	 * @return
	 */
	public String getPost_data() {
		return post_data;
	}

	/**
	 * 获取Cookies
	 * 仅对BasicRequester有效
	 * @return
	 */
	public String getCookies() {
		return cookies;
	}

	/**
	 * 获取Reference
	 * 仅对BasicRequester有效
	 * @return
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * 仅对BasicRequester有效
	 * @param ref
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * 获取Domain
	 * @return
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * 获取Proxy
	 * @return
	 */
	public one.rewind.io.requester.proxy.Proxy getProxy() {
		return proxy;
	}

	/**
	 * 设定Proxy
	 * @param proxy
	 */
	public void setProxy(one.rewind.io.requester.proxy.Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * 设定登录任务标识
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public Task setLoginTask() {
		this.login_task = true;
		return this;
	}

	/**
	 * 判断是否为登录任务
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public boolean isLoginTask() {
		return this.login_task;
	}

	/**
	 * 获取用户名
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设定用户名
	 * ChromeDriverAgent 专用
	 * @param username
	 * @return
	 */
	public Task setUsername(String username) {
		this.username = username;
		this.login_task = true;
		return this;
	}

	/**
	 * 设定请求过滤器
	 * ChromeDriverAgent 专用
	 * TODO 在使用MITM代理时，报错 io.netty.util.IllegalReferenceCountException: refCnt: 0, increment: 1
	 * 使用 HttpFiltersSourceAdapter 则没有报错
	 * 有可能是littleproxy自身问题
	 * @param filter
	 * @return
	 */
	public Task setRequestFilter(RequestFilter filter) {
		this.requestFilter = filter;
		return this;
	}

	/**
	 * 获取请求过滤器
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public RequestFilter getRequestFilter() {
		return this.requestFilter;
	}

	/**
	 * 设定返回过滤器
	 * ChromeDriverAgent 专用
	 * @param filter
	 * @return
	 */
	public Task setResponseFilter(ResponseFilter filter) {
		this.responseFilter = filter;
		return this;
	}

	/**
	 * 获取返回过滤器
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public ResponseFilter getResponseFilter() {
		return this.responseFilter;
	}

	/**
	 * 获得返回对象
	 * @return
	 */
	public Response getResponse() {
		return response;
	}

	/**
	 * 是否前置处理
	 * @return
	 */
	public boolean preProc() {
		return flags.contains(Flag.PRE_PROC);
	}

	/**
	 * 设定前置处理
	 */
	public void setPreProc() {
		flags.add(Flag.PRE_PROC);
	}

	public boolean switchProxy() {
		return flags.contains(Flag.SWITCH_PROXY);
	}

	public void setSwitchProxy() {
		flags.add(Flag.SWITCH_PROXY);
	}

	/**
	 * 是否构建DOM
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public boolean buildDom() {
		return flags.contains(Flag.BUILD_DOM);
	}

	/**
	 * 设定构建DOM
	 * ChromeDriverAgent 专用
	 */
	public void setBuildDom() {
		flags.add(Flag.BUILD_DOM);
	}

	/**
	 * 是否进行截屏
	 * ChromeDriverAgent 专用
	 * @return
	 */
	public boolean shootScreen() {
		return flags.contains(Flag.SHOOT_SCREEN);
	}

	/**
	 * 设定进行截屏
	 */
	public void setShootScreen() {
		flags.contains(Flag.SHOOT_SCREEN);
	}

	/**
	 * 设定开始时间
	 */
	public void setStartTime() {
		this.start_time = new Date();
	}

	/**
	 * 获取用时
	 * @return
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * 设定用时
	 */
	public void setDuration() {
		this.duration = System.currentTimeMillis() - this.start_time.getTime();
	}

	/**
	 * 设定重试
	 */
	public void setRetry() {
		this.retry = true;
	}

	/**
	 * 判断重试
	 * @return
	 */
	public boolean needRetry() {
		return this.retry;
	}

	/**
	 * 设定重试
	 */
	public void setStep(int step) {
		this.step = step;
	}

	/**
	 * 判断重试
	 * @return
	 */
	public synchronized int getStep() {
		return this.step;
	}

	/**
	 * 设置验证器
	 * TODO 默认应该没有ChromeDriverAgent对象
	 * @param validator
	 * @return
	 */
	public Task setValidator(TaskValidator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * 生成JSON
	 * @return
	 */
	public String toJSON() {
		return JSON.toJson(this);
	}

	/**
	 * 获取重试次数
	 * @return
	 */
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * 增加重试次数
	 */
	public void addRetryCount() {
		this.retryCount ++;
	}

	/**
	 * 获取请求器类名
	 * @return
	 */
	public String getRequester_class() {
		return requester_class;
	}

	/**
	 * 设置请求器类名
	 * @param requester_class
	 */
	public void setRequester_class(String requester_class) {
		this.requester_class = requester_class;
	}

	/**
	 * 获取异常列表
	 * @return
	 */
	public List<Throwable> getExceptions() {
		return exceptions;
	}

	/**
	 * 添加Exception
	 * @param e
	 */
	public void addExceptions(Throwable e) {
		this.exceptions.add(e);
	}

	/**
	 * 获取字符串类型参数
	 * @param key
	 * @return
	 */
	public String getParamString(String key) {
		if (params.get(key) == null) return null;

		return String.valueOf(params.get(key));
	}

	/**
	 * 获取Int类型参数
	 * @param key
	 * @return
	 */
	public int getParamInt(String key) {
		if (params.get(key) == null) return 0;
		//return Integer.valueOf((int) params.get(key));
		return NumberFormatUtil.parseInt(String.valueOf(params.get(key)));
	}

	/**
	 * 设置参数
	 * @param key
	 * @param object
	 */
	public Task param(String key, Object object) {
		this.params.put(key, object);
		return this;
	}

	/**
	 *
	 * @return
	 */
	public String getFingerprint() {

		if(params.keySet().size() == 0) {
			return id;
		}
		else {

			String src = "[" + domain + ":" + username + "];";

			for(String key : params.keySet()) {
				src += key + ":" + params.get(key) + ";";
			}

			return StringUtil.MD5(src);
		}
	}

	/**
	 * 保存到数据库
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 设定优先级
	 * @param priority
	 */
	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	/**
	 * 获取优先级
	 * @return
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * 优先级比较
	 *
	 * @param another
	 * @return
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
	 * @param callback
	 * @return
	 */
	public Task addDoneCallback(TaskCallback<T> callback) {
		if (this.doneCallbacks == null) this.doneCallbacks = new LinkedList<>();
		this.doneCallbacks.add(callback);
		return this;
	}

	/**
	 *
	 * @param ntg
	 * @return
	 */
	public Task addNextTaskGenerator(NextTaskGenerator<T> ntg) {
		if (this.nextTaskGenerators == null) this.nextTaskGenerators = new LinkedList<>();
		this.nextTaskGenerators.add(ntg);
		return this;
	}

	/**
	 * 添加采集异常回调
	 * @param callback
	 * @return
	 */
	public Task addExceptionCallback(TaskCallback<T> callback) {
		if (this.exceptionCallbacks == null) this.exceptionCallbacks = new LinkedList<>();
		this.exceptionCallbacks.add(callback);
		return this;
	}

	/**
	 *
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
		 * @return
		 */
		public Map<String, List<String>> getHeader() {
			return header;
		}

		/**
		 * 设置Header
		 *
		 * @param header
		 */
		public void setHeader(Map<String, List<String>> header) {
			this.header = header;
		}

		/**
		 * 获取源文件
		 *
		 * @return
		 */
		public byte[] getSrc() {
			return src;
		}

		/**
		 * 设置源文件
		 *
		 * @param src
		 */
		public void setSrc(byte[] src) {
			this.src = src;
		}

		/**
		 * 获取编码
		 *
		 * @return
		 */
		public String getEncoding() {
			return encoding;
		}

		/**
		 * 设置编码
		 *
		 * @param encoding
		 */
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		/**
		 * 获取返回cookie
		 *
		 * @return
		 */
		public String getCookies() {
			return cookies;
		}

		/**
		 * 设置cookie
		 *
		 * @param cookies
		 */
		public void setCookies(String cookies) {
			this.cookies = cookies;
		}

		/**
		 * 判断 Task 中 Action是否已经执行完毕
		 *
		 * @return
		 */
		public boolean isActionDone() {
			return actionDone;
		}

		/**
		 * @param actionDone
		 */
		public void setActionDone(boolean actionDone) {
			this.actionDone = actionDone;
		}

		/**
		 * @return
		 */
		public String getText() {
			return text;
		}

		/**
		 * @return
		 */
		public byte[] getScreenshot() {
			return screenshot;
		}

		/**
		 * @param screenshot
		 */
		public void setScreenshot(byte[] screenshot) {
			this.screenshot = screenshot;
		}

		/**
		 * @param key
		 * @param value
		 */
		public void setVar(String key, String value) {
			vars.put(key, value);
		}

		/**
		 * @param key
		 * @return
		 */
		public String getVar(String key) {
			return vars.get(key);
		}

		/**
		 * @param key
		 * @return
		 */
		public int getIntVar(String key) {
			return Integer.parseInt(vars.get(key));
		}

		/**
		 * 判断 Response 是否为文本
		 *
		 * @return
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
		 * @throws UnsupportedEncodingException
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
		 * @throws UnsupportedEncodingException
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
		 *
		 * @throws UnsupportedEncodingException
		 */
		public void buildDom() throws UnsupportedEncodingException {

			if (src != null && src.length > 0) {
				doc = Jsoup.parse(BasicRequester.autoDecode(src, encoding));
			}
		}

		/**
		 * 获取文档
		 * @return
		 */
		public Document getDoc() {
			return doc;
		}
	}
}