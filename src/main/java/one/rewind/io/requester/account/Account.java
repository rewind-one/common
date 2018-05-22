package one.rewind.io.requester.account;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.DaoManager;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;

public abstract class Account implements JSONable<Account> {

	private static final Logger logger = LogManager.getLogger(Account.class.getName());

	public enum Status {
		Free,
		Occupied,
		Frozen,
		Broken,
	}

	// UUID
	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	public String id;

	// domain
	@DatabaseField(dataType = DataType.STRING, width = 1024, index = true)
	public String domain;

	// 登录地址
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String login_url;

	// 用户名
	@DatabaseField(dataType = DataType.STRING, width = 1024, index = true)
	public String username;

	// 密码
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String password;

	// 手机号
	@DatabaseField(dataType = DataType.STRING, width = 16)
	public String mobile;

	// Proxy ID
	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String proxy_id;

	// Proxy Group
	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String proxy_group;

	// Proxy 状态
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Status status;

	// Proxy 状态
	@DatabaseField(dataType = DataType.BOOLEAN, width = 1)
	public boolean enabled = true;

	// 插入时间
	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	// 更新时间
	@DatabaseField(dataType = DataType.DATE, index = true)
	public Date update_time = new Date();

	public Account() {}

	public Account(String domain, String username, String password) {
		this.id = hash();
		this.username = username;
		this.password = password;
	}

	public Account setProxyId(String proxyId) {
		this.proxy_id = proxyId;
		return this;
	}

	public Account setProxyGroup(String proxyGroup) {
		this.proxy_group = proxyGroup;
		return this;
	}

	public String getDomain() {
		return this.domain;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getProxyId() {
		return this.proxy_id;
	}

	public String getProxyGroup() {
		return this.proxy_group;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	public String hash() {
		return StringUtil.MD5(this.domain + this.username );
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		try {
			dao.create(this);
			return true;
		} catch (SQLException e) {
			try {
				dao.update(this);
				return true;
			} catch (SQLException ex) {
				logger.error("insert update error {}", ex);
				return false;
			}
		}
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception{

		update_time = new Date();

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}
}
