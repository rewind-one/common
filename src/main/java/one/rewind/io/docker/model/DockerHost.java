package one.rewind.io.docker.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.typesafe.config.Config;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import one.rewind.io.ssh.SshManager;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Date;
public class DockerHost {

	public static final Logger logger = LogManager.getLogger(DockerHost.class.getName());

	public static int MAX_CONTAINER_NUM = 40;
	public static File PEM_FILE;

	static {
		Config config = Configs.getConfig(DockerHost.class);
		MAX_CONTAINER_NUM = config.getInt("maxContainerNum");
		PEM_FILE = new File(config.getString("privateKey"));
	}

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false, generatedId = true)
	public int id;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String ip;

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int port;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String username;

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int container_num = 0;

	public static enum Status {
		RUNNING,
		STOPPED
	}

	@DatabaseField(dataType = DataType.ENUM_STRING, width = 16, canBeNull = false)
	public Status status = Status.RUNNING;

	// 插入时间
	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	// 更新时间
	@DatabaseField(dataType = DataType.DATE, index = true)
	public Date update_time = new Date();

	public DockerHost() {}

	public DockerHost(String ip, int port, String username) {
		this.ip = ip;
		this.port = port;
		this.username = username;
	}

	public String exec(String cmd) {

		// 秘钥登录
		//SshManager.Host sshHost = new SshManager.Host(ip, port, username, DockerHost.PEM_FILE);

		SshManager.Host sshHost = new SshManager.Host(ip, port, username, "sdyk");
		String output = null;

		try {

			// 同时连接不能超过10个，否则会抛出异常    http://www.ganymed.ethz.ch/ssh2/FAQ.html
			// java.io.IOException: Could not open channel (The server refused to open the channel (SSH_OPEN_ADMINISTRATIVELY_PROHIBITED, 'open failed'))
			sshHost.connect();
			output = sshHost.exec(cmd);

		} catch (Exception e) {

			// 无法连接服务器，则程序停止运行
			e.printStackTrace();

		} finally {
			if(sshHost.conn != null) {
				sshHost.conn.close();
			}
		}

		return output;
	}

	/**
	 *
	 */
	public void delAllDockerContainers() {

		String cmd = "docker stop $(docker ps -a -q) && docker rm $(docker ps -a -q)\n";

		String output = this.exec(cmd);

		logger.info(output);
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {
		Dao dao = DaoManager.getDao(this.getClass());
		return dao.create(this) == 1;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception {
		Dao dao = DaoManager.getDao(this.getClass());
		return dao.update(this) == 1;
	}

	/**
	 *
	 *
	 * @throws Exception
	 */
	public ChromeDriverDockerContainer createChromeDriverDockerContainer() throws Exception {

		ChromeDriverDockerContainer container = null;

		int currentContainerNum = this.addContainerNum();

		int seleniumPort = (ChromeDriverDockerContainer.SELENIUM_BEGIN_PORT + currentContainerNum);
		int vncPort = (ChromeDriverDockerContainer.VNC_BEGIN_PORT + currentContainerNum);
		String containerName = "ChromeContainer-" + this.ip + "-" + currentContainerNum;

		container = new ChromeDriverDockerContainer(this, containerName, seleniumPort, vncPort);
		container.create();

		return container;
	}

	public int addContainerNum() throws Exception {

		this.container_num ++;

		return this.container_num;
	}


	/**
	 * 统计container数量
	 */
	/*public int addContainerNum() throws Exception {

		RLock lock = redisson.getLock("Docker-Host-Access-Lock-" + ip);

		lock.lock(10, TimeUnit.SECONDS);

		Dao<DockerHost, String> dao = DaoManager.getDao(DockerHost.class);
		// 刷新内存数据
		dao.refresh(this);

		this.container_num ++;
		this.update();

		lock.unlock();

		return this.container_num;
	}*/

	public int minusContainerNum() throws Exception {
		this.container_num --;

		return this.container_num;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	/*public int minusContainerNum() throws Exception {

		RLock lock = redisson.getLock("Docker-Host-Access-Lock-" + ip);

		lock.lock(10, TimeUnit.SECONDS);

		Dao<DockerHost, String> dao = DaoManager.getDao(DockerHost.class);
		// 刷新内存数据
		dao.refresh(this);

		this.container_num --;
		this.update();

		lock.unlock();

		return this.container_num;
	}*/
}
