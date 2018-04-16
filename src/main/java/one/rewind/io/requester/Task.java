package one.rewind.io.requester;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DaoManager;
import one.rewind.io.requester.chrome.action.ChromeAction;
import one.rewind.io.requester.exception.AccountException;
import one.rewind.io.requester.exception.ProxyException;
import one.rewind.json.JSON;
import one.rewind.txt.ChineseChar;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;
import one.rewind.txt.URLUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import one.rewind.db.DBName;
import one.rewind.io.requester.account.Account;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * HTTP 请求任务
 * @author karajan
 *
 */
@DatabaseTable(tableName = "tasks")
@DBName(value = "crawler")
public class Task implements Comparable<Task>{

	public enum Priority {
		LOW,
		MEDIUM,
		HIGH
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
		BUILD_DOM
	}

	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	private String id;

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
	private HashMap<String, Object> params = new HashMap<>();

	@DatabaseField(dataType = DataType.STRING, width = 256)
	private String requester_class = BasicRequester.class.getSimpleName();

	// 代理出口信息
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Account account;
	// 账户信息

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private one.rewind.io.requester.proxy.Proxy proxy;

	// 执行动作列表
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<ChromeAction> actions = new ArrayList<>();

	// 参数
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private List<Flag> flags = new ArrayList<>();

	// 返回对象
	private transient Response response = new Response();

	// 记录参数
	@DatabaseField(dataType = DataType.DATE)
	private Date start_time;

	@DatabaseField(dataType = DataType.LONG)
	private long duration = 0;

	private transient boolean retry = false;

	@DatabaseField(dataType = DataType.INTEGER)
	private int retryCount = 0;

	// 运行时异常
	private transient Throwable e;

	// 异常记录
	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private ArrayList<Throwable> exceptions = new ArrayList<>();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	private Date create_time = new Date();

	private Task() {
		this.response = new Response();
	}
	
	/**
	 * 简单 GET 请求
	 * @param url url地址
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url) throws MalformedURLException, URISyntaxException {
		this.url = url;
		domain = URLUtil.getDomainName(url);
		this.response = new Response();
		this.id = StringUtil.MD5(url + System.nanoTime());
		this.request_method = RequestMethod.GET;

	}
	
	/**
	 * 简单 POST 请求
	 * @param url url 地址
	 * @param post_data post data 字符串格式
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String post_data) throws MalformedURLException, URISyntaxException {
		
		this.url = url;
		this.post_data = post_data;
		domain = URLUtil.getDomainName(url);
		
		this.response = new Response();
		this.id = StringUtil.MD5(url + post_data + System.nanoTime());

		if(post_data != null && post_data.length() > 0) {
			this.request_method = RequestMethod.POST;
		} else {
			this.request_method = RequestMethod.GET;
		}

	}
	
	/**
	 * 需要 Cookie 的 POST 请求
	 * @param url
	 * @param post_data
	 * @param cookies
	 * @param ref
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String post_data, String cookies, String ref) throws MalformedURLException, URISyntaxException {
		
		this.url = url;
		this.post_data = post_data;
		this.cookies = cookies;
		this.ref = ref;
		domain = URLUtil.getDomainName(url);
		
		this.response = new Response();
		this.id = StringUtil.MD5(url + post_data + cookies + System.nanoTime());

		if(post_data != null && post_data.length() > 0) {
			this.request_method = RequestMethod.POST;
		} else {
			this.request_method = RequestMethod.GET;
		}
	}

	/**
	 * 完整参数请求
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
		this.id = StringUtil.MD5(url + post_data + cookies + System.nanoTime());

		if(post_data != null && post_data.length() > 0) {
			this.request_method = RequestMethod.POST;
		} else {
			this.request_method = RequestMethod.GET;
		}
	}

	public String getId() {
		return this.id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRequestMethod() {
		return request_method.name();
	}

	public void setRequestethod(RequestMethod request_method) {
		this.request_method = request_method;
	}

	public void setPost() {
		this.request_method = RequestMethod.POST;
	}

	public void setPut() {
		this.request_method = RequestMethod.PUT;
	}

	public void setDelete() {
		this.request_method = RequestMethod.DELETE;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getPost_data() {
		return post_data;
	}

	public String getCookies() {
		return cookies;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getDomain() {
		return domain;
	}

	public one.rewind.io.requester.proxy.Proxy getProxy() {
		return proxy;
	}

	public void setProxy(one.rewind.io.requester.proxy.Proxy proxy) {
		this.proxy = proxy;
	}

	public Account getAccount() {
		return this.account;
	}

	public void setAccount(Account aw) {
		this.account = aw;
	}

	public List<ChromeAction> getActions() {
		return actions;
	}

	public void addAction(ChromeAction action) {
		this.actions.add(action);
	}

	public void setResponse() {
		response = new Response();
	}

	public Response getResponse() {
		return response;
	}

	public boolean preProc() {
		return flags.contains(Flag.PRE_PROC);
	}

	public void setPreProc() {
		flags.add(Flag.PRE_PROC);
	}

	public boolean buildDom() {
		return flags.contains(Flag.BUILD_DOM);
	}

	public void setBuildDom() {
		flags.add(Flag.BUILD_DOM);
	}

	public boolean shootScreen() {
		return flags.contains(Flag.SHOOT_SCREEN);
	}

	public void setShootScreen() {
		flags.contains(Flag.SHOOT_SCREEN);
	}

	public void setStartTime() {
		this.start_time = new Date();
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration() {
		this.duration = System.currentTimeMillis() - this.start_time.getTime();
	}

	public void setRetry() {
		this.retry = true;
	}

	public boolean needRetry() {
		return this.retry;
	}

	public Throwable getException() {
		return e;
	}

	public void setException(Throwable e) {
		this.e = e;
	}

	public List<? extends Task> postProc() throws Exception {
		return new ArrayList<>();
	}

	/**
	 *
	 * @return
	 * @throws ProxyException.Failed
	 * @throws AccountException.Failed
	 * @throws AccountException.Frozen
	 */
	public Task validate() throws ProxyException.Failed, AccountException.Failed, AccountException.Frozen {
		return this;
	}

	public String toJSON() {
		return JSON.toJson(this);
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void addRetryCount() {
		this.retryCount ++;
	}

	public String getRequester_class() {
		return requester_class;
	}

	public void setRequester_class(String requester_class) {
		this.requester_class = requester_class;
	}

	public List<Throwable> getExceptions() {
		return exceptions;
	}

	public void addExceptions(Throwable e) {
		this.exceptions.add(e);
	}

	public String getParamString(String key) {
		if(params.get(key) == null) return null;

		return String.valueOf(params.get(key));
	}

	public int getParamInt(String key) {
		if(params.get(key) == null) return 0;
		//return Integer.valueOf((int) params.get(key));
		return NumberFormatUtil.parseInt(String.valueOf(params.get(key)));
	}

	public void setParam(String key, Object object) {
		this.params.put(key, object);
	}


	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {

		Dao<Task, String> dao = DaoManager.getDao(Task.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 *
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static Task getTask(String id) throws Exception {
		Dao<Task, String> dao = DaoManager.getDao(Task.class);
		return dao.queryForId(id);
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Priority getPriority() {
		return priority;
	}

	/**
	 * 优先级比较
	 * @param another
	 * @return
	 */
	public int compareTo(Task another) {

		final Priority me = this.getPriority();
		final Priority it = another.getPriority();
		if(me.ordinal() == it.ordinal()) {
			return this.create_time.compareTo(another.create_time);
		} else {
			return it.ordinal() - me.ordinal();
		}
	}


	/**
	 * 返回对象
	 * @author karajan
	 */
	public class Response {

		private Map<String, List<String>> header;
		private byte[] src;
		private String encoding;
		private String cookies;

		private boolean actionDone;

		private String text;

		private byte[] screenshot = null;

		private Document doc = null;

		public Map<String, List<String>> getHeader() {
			return header;
		}

		public void setHeader(Map<String, List<String>> header) {
			this.header = header;
		}

		public byte[] getSrc() {
			return src;
		}

		public void setSrc(byte[] src) {
			this.src = src;
		}

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public String getCookies() {
			return cookies;
		}

		public void setCookies(String cookies) {
			this.cookies = cookies;
		}

		public boolean isActionDone() {
			return actionDone;
		}

		public void setActionDone(boolean actionDone) {
			this.actionDone = actionDone;
		}

		public String getText() {
			return text;
		}

		public byte[] getScreenshot() {
			return screenshot;
		}

		public void setScreenshot(byte[] screenshot) {
			this.screenshot = screenshot;
		}

		/**
		 * 判断 Response 是否为文本
		 * @return
		 */
		public boolean isText(){
			if(header == null) return true;
			if(header.get("Content-Type") != null){
				for(String item: header.get("Content-Type")){

					if((item.contains("application") && !item.contains("json") && !item.contains("xml") && !item.contains("x-javascript"))
						|| item.contains("application") && item.contains("officedocument")
						|| item.contains("video")
						|| item.contains("audio")
						|| item.contains("image")
					){
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * 文本内容预处理
		 * @param input 原始文本
		 * @throws UnsupportedEncodingException
		 */
		public void setText(String input) throws UnsupportedEncodingException {

			this.text = input;

			if(preProc()) {

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

		public void buildDom() {

			if(text!=null && text.length() > 0) {
				doc = Jsoup.parse(text);
			}
		}

		public Document getDoc() {
			return doc;
		}
	}
}
