package one.rewind.io.requester.proxy;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.Daos;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;


public abstract class Proxy implements JSONable<Proxy> {

	static final Logger logger = LogManager.getLogger(Proxy.class.getName());

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false, generatedId = true)
	public int id;

	@DatabaseField(dataType = DataType.STRING, width = 39, index = true)
	public String group;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String host;

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int port;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = true)
	public String location;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "false")
	public boolean https = false;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "false")
	public boolean gl = false;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = true)
	public String username;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = true)
	public String password;

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int request_per_second_limit = 0; //不限制

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long use_cnt = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long bytes_send = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long bytes_rev = 0;

	@DatabaseField(dataType = DataType.ENUM_STRING, width = 16, canBeNull = false)
	public Status status = Status.Free;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "true")
	public boolean enable = true;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date update_time = new Date();

	public enum Status {
		Free,
		Busy,
		INVALID
	}

	public Proxy() {}

	/**
	 *
	 * @param group
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 * @param location
	 */
	public Proxy(String group, String host, int port, String username, String password, String location, int request_per_second_limit) {
		this.group = group;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.location = location;
		this.request_per_second_limit = request_per_second_limit;
	}

	/**
	 *
	 * @throws Exception
	 */
	public boolean validate() {

		ProxyValidator.Task task = ProxyValidator.getInstance().validate(this, ProxyValidator.Type.TestAlive, null);
		logger.info(getInfo() + " ---> " + task.status);

		if(task.status.contains(ProxyValidator.Status.OK) ){
			logger.warn("Proxy:{} {} good.", id, getInfo());
			this.status = Status.Free;
			return true;
		} else {
			logger.warn("Proxy:{} {} invalid.", getInfo());
			this.status = Status.INVALID;
			return false;
		}

	}

	/**
	 * 验证是否匿名，是否支持HTTPS，是否能翻墙等特性
	 * @throws Exception
	 */
	public void validateAll() {

		ProxyValidator.Task task = ProxyValidator.getInstance().validate(this, ProxyValidator.Type.TestAll, null);
		logger.info(getInfo() + " ---> " + task.status);

		if(task.status.contains(ProxyValidator.Status.OK)
				&& task.status.contains(ProxyValidator.Status.Anonymous)
				){
			this.status = Status.Free;
			logger.warn("Proxy:{} {} good.", id, getInfo());
		} else {
			this.status = Status.INVALID;
			logger.warn("Proxy:{} {} invalid.", getInfo());
		}

		if(task.status.contains(ProxyValidator.Status.Https)){
			this.https = true;
		}

		if(task.status.contains(ProxyValidator.Status.GL)){
			this.gl = true;
		}
	}

	/**
	 *
	 * @param bytes
	 */
	public synchronized void addSendBytes(long bytes) {
		this.bytes_send += bytes;
	}

	/**
	 *
	 * @param bytes
	 */
	public synchronized void addRevBytes(long bytes) {
		this.bytes_rev += bytes;
	}

	/**
	 * 插入新代理记录
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao dao = Daos.get(this.getClass());

		List<Proxy> existProxys = dao.queryBuilder()
				.where()
				.eq("host", host)
				.and()
				.eq("port", port)
				.and()
				.eq("username", username)
				.and()
				.eq("password", password)
				.query();

		if (existProxys.size() > 0) return false;

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 更新代理记录
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception{

		update_time = new Date();

		Dao dao = Daos.get(this.getClass());

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}

	public String getId() {
		return String.valueOf(id);
	}

	public String getInfo() {
		return host + ":" + port;
	}

	public java.net.Proxy toProxy() {
		InetSocketAddress address = new InetSocketAddress(host, port);
		return new java.net.Proxy(java.net.Proxy.Type.HTTP, address);
	}

	public HttpHost toHttpHost() {
		return new HttpHost(host, port, getProtocol());
	}

	public InetSocketAddress getInetSocketAddress() {
		return new InetSocketAddress(host, port);
	}

	public String getProtocol() {
		return https ? "https" : "http";
	}

	public String getHost() {
		return host;
	}

	public boolean needAuth() {
		return this.username != null && this.password != null;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public int getRequestPerSecondLimit() {
		return request_per_second_limit;
	}

	public String getAuthenticationHeader() {
		String authParam = getUsername() + ":" + getPassword();
		authParam = new String(Base64.encodeBase64(authParam.getBytes()));
		return "Basic " + authParam;
	}

	public boolean isValid() {
		return status == Status.Free;
	}

	public boolean success() throws Exception {
		return false;
	}

	/**
	 * TODO
	 */
	public boolean failed() throws Exception {
		return false;
	}

	/**
	 * TODO
	 */
	public boolean timeout() throws Exception {
		return false;
	}

	/**
	 * TODO
	 */
	public String toJSON() {
		return JSON.toJson(this);
	}
}
