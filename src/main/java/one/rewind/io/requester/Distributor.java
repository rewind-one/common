package one.rewind.io.requester;

import com.typesafe.config.Config;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.chrome.ChromeDistributor;
import one.rewind.io.requester.scheduler.ScheduledTask;
import one.rewind.io.requester.scheduler.TaskScheduler;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.Configs;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author scisaga@gmail.com
 * @date 2018/10/14
 */
public abstract class Distributor {

	public static final Logger logger = LogManager.getLogger(ChromeDistributor.class.getName());

	// 连接超时时间
	public static int CONNECT_TIMEOUT;

	// 读取超时时间
	public static int READ_TIMEOUT;

	// 本地IP
	public static String LOCAL_IP;

	// 配置设定
	static {

		Config ioConfig = Configs.getConfig(BasicRequester.class);

		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");
		LOCAL_IP = ioConfig.getString("requesterLocalIp");
		LOCAL_IP = NetworkUtil.getLocalIp();
	}

	public TaskScheduler taskScheduler = new TaskScheduler(this);

	public Distributor() {}

	public TaskScheduler getScheduler() {
		return taskScheduler;
	}

	public abstract SubmitInfo submit(TaskHolder th) throws Exception;

	/**
	 *
	 * @param holder
	 * @param cron
	 * @return
	 * @throws Exception
	 */
	public SubmitInfo schedule(TaskHolder holder, String cron) throws Exception {

		return taskScheduler.schedule(holder, cron);
	}

	/**
	 *
	 * @param holder
	 * @param crons
	 * @return
	 * @throws Exception
	 */
	public SubmitInfo schedule(TaskHolder holder, List<String> crons) throws Exception {

		return taskScheduler.schedule(holder, crons);
	}

	/**
	 *
	 * @param id
	 * @throws Exception
	 */
	public synchronized void unschedule(String id) throws Exception {

		taskScheduler.unschedule(id);
	}

	/**
	 *
	 */
	public static class SubmitInfo implements JSONable<SubmitInfo> {

		public boolean success = true;

		String localIp;
		String domain;
		String account;
		String id;

		Map<String, Object> agent;

		/**
		 *
		 */
		public SubmitInfo() {
		}

		/**
		 *
		 * @param success
		 */
		public SubmitInfo(boolean success) {
			this.success = success;
		}

		/**
		 *
		 * @param localIp
		 * @param domain
		 * @param account
		 * @param id
		 * @param agentInfo
		 */
		public SubmitInfo(String localIp, String domain, String account, String id, Map<String, Object> agentInfo) {
			this.localIp = localIp;
			this.domain = domain;
			this.account = account;
			this.id = id;
			this.agent = agentInfo;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

}
