package one.rewind.io.docker.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.DaoManager;
import one.rewind.io.ssh.RemoteShell;

import java.util.Date;

/**
 * 容器
 */
public abstract class DockerContainer implements RemoteShell {

	public enum Status {
		STARTING, // 启动中
		IDLE, // 空闲
		OCCUPIED, // 占用
		FAILED, // 出错
		TERMINATED // 已删除
	}

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false, generatedId = true)
	public int id;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String imageName;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String ip;

	@DatabaseField(dataType = DataType.ENUM_STRING, width = 16, canBeNull = false)
	public Status status = Status.STARTING;

	public transient DockerHost host = null;

	// 插入时间
	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	// 更新时间
	@DatabaseField(dataType = DataType.DATE, index = true)
	public Date update_time = new Date();

	/**
	 *
	 */
	public DockerContainer() {}

	/**
	 *
	 * @param cmd
	 * @return
	 */
	public String exec(String cmd) {
		String output = "";
		if(host != null) {
			output = host.exec("docker exec " + name + " " + cmd);
		}
		return output;
	}

	public void setIdle() throws Exception {
		this.status = Status.IDLE;
		this.update();
	}

	public void setOccupied() throws Exception {
		this.status = Status.OCCUPIED;
		this.update();
	}

	public abstract void create() throws Exception;

	/**
	 * 删除容器
	 */
	public void rm() throws Exception {

		String cmd = "docker rm -f " + name + "\n";

		if (host != null) {

			String output = host.exec(cmd);
			// TODO 根据output 判断是否执行成功

			DockerHost.logger.info(output);

			host.minusContainerNum();

			status = Status.TERMINATED;

		} else {
			throw new Exception("DockerHost is null");
		}
	}

	/**
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
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 * 删除当前container
	 *
	 * @throws Exception
	 */
	public void delete() throws Exception {

		if (this.status == Status.TERMINATED) {
			Dao dao = DaoManager.getDao(this.getClass());
			dao.delete(this);
		} else {
			throw new IllegalStateException("Terminate docker container before delete.");
		}
	}
}

