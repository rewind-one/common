package one.rewind.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class FastDFSAdapter {

	public final static Logger logger = LogManager.getLogger(FastDFSAdapter.class.getName());

	public static FastDFSAdapter instance;

	// 并发客户端数量 TODO 可根据配置文件定义
	private int clientNum = 4;

	private volatile boolean closing = false;

	// 空闲的连接
	private BlockingQueue<StorageClient> idleStorageClient = new LinkedBlockingQueue<>();

	private List<TrackerServer> trackerServerList = new ArrayList<>();

	private List<StorageServer> storageServerList = new ArrayList<>();

	/**
	 * 单例方法
	 * @return
	 */
	public static FastDFSAdapter getInstance() {

		if (instance == null) {

			synchronized(FastDFSAdapter.class) {
				if (instance == null) {
					instance = new FastDFSAdapter();
				}
			}
		}

		return instance;
	}

	/**
	 * 构造方法
	 */
	private FastDFSAdapter(){

		// 初始化配置
		try {
			ClientGlobal.init("fastdfs.conf");
		} catch (Exception e){
			logger.error("Error load config file, ", e);
		}

		// 初始化 StorageClient 队列
		for(int i = 0; i < clientNum; i++){
			createStorageClient();
		}
	}

	/**
	 *  创建StorageClient
	 */
	private void createStorageClient(){

		try {

			TrackerClient tracker = new TrackerClient();
			TrackerServer trackerServer = tracker.getConnection();
			trackerServerList.add(trackerServer);

			StorageServer storageServer = null;
			storageServerList.add(storageServer);

			StorageClient client = new StorageClient(trackerServer, storageServer);

			// 将 client 放入队列
			idleStorageClient.offer(client);

		} catch (Exception e){

			logger.error("Error create FastDFS client, ", e);
		}
	}

	/**
	 * 获取StorageClient
	 * @return
	 */
	private synchronized StorageClient getStorageClient() throws InterruptedException {

		// 获取链接
		if( idleStorageClient.size() == 0 ){
			createStorageClient();
		}

		StorageClient storageClient = idleStorageClient.take();

		return storageClient;
	}

	/**
	 *
	 * @param file_buff
	 * @param file_ext_name
	 * @param meta
	 * @return
	 * @throws InterruptedException
	 */
	public String[] put(byte[] file_buff, String file_ext_name, LinkedHashMap<String, String> meta) throws InterruptedException {

		NameValuePair[] meta_list = new NameValuePair[meta.size()];
		int i = 0;
		for(String key : meta.keySet()) {

			NameValuePair kv = new NameValuePair();
			kv.setName(key);
			kv.setValue(meta.get(key));

			meta_list[i++] = kv;
		}

		return put(file_buff,file_ext_name, meta_list);
	}

	/**
	 * 上传文件
	 * @param file_buff
	 * @param file_ext_name
	 * @param meta_list
	 * @return
	 * @throws Exception
	 */
	public String[] put(byte[] file_buff, String file_ext_name, NameValuePair[] meta_list) throws InterruptedException {

		if(closing) throw new InterruptedException("FastDFSAdapter is closing.");

		// 获取链接
		StorageClient storageClient = getStorageClient();

		String[] info = null;

		try{
			// 上传到 FastDFS
			info = storageClient.upload_file(file_buff, file_ext_name, meta_list);

			// 将 链接放入 队列
			idleStorageClient.put(storageClient);

		} catch (Exception e) {

			// 移除报错链接
			idleStorageClient.remove(storageClient);
			logger.error("Error upload file, {}:{}, ", file_ext_name, meta_list, e);
		}

		return info;
	}

	/**
	 * 下载文件
	 * @param groupName
	 * @param filePath
	 * @return
	 */
	public byte[] get(String groupName, String filePath) throws InterruptedException {

		if(closing) throw new InterruptedException("FastDFSAdapter is closing.");

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 下载文件
		byte[] bytes = null;

		try {
			bytes = storageClient.download_file(groupName, filePath);
		} catch (Exception e) {
			idleStorageClient.remove(storageClient);
			logger.error("Error get file:{}:{}, ", groupName, filePath, e);
		}

		return bytes;
	}

	/**
	 * 获取文件信息
	 * @param groupName
	 * @param filePath
	 * @return
	 */
	public FileInfo getInfo(String groupName, String filePath) throws InterruptedException {

		if(closing) throw new InterruptedException("FastDFSAdapter is closing.");

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 获取文件信息
		FileInfo file = null;
		try {
			file = storageClient.get_file_info(groupName, filePath);
			idleStorageClient.remove(storageClient);
		} catch (Exception e) {
			idleStorageClient.remove(storageClient);
			logger.error("Error get info {}:{}, ", groupName, filePath, e);
		}

		return file;
	}

	/**
	 * 删除文件
	 * @param groupName
	 * @param filePath
	 * @return
	 */
	public boolean delete(String groupName, String filePath) throws InterruptedException {

		if(closing) throw new InterruptedException("FastDFSAdapter is closing.");

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 删除文件
		int i = 1;

		try {
			i = storageClient.delete_file(groupName, filePath);
		} catch (Exception e) {
			logger.error("Error delete file {}:{}, ", groupName, filePath, e);
		}

		return i==0 ? true : false;
	}

	/**
	 * 获取存储统计信息
	 * @return
	 * @throws IOException
	 */
	public List<Map<String, Object>> getStat() throws IOException {

		List<Map<String, Object>> info = new ArrayList<>();

		TrackerClient tracker = new TrackerClient();
		TrackerServer trackerServer = tracker.getConnection();

		if (trackerServer == null) {
			return null;
		}

		StructGroupStat[] groupStats = tracker.listGroups(trackerServer);
		if (groupStats == null) {
			logger.error("ERROR! list groups error, error no: " + tracker.getErrorCode());
			return null;
		}

		int count = 0;
		for (StructGroupStat groupStat : groupStats) {

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", count++);
			item.put("name", groupStat.getGroupName());
			item.put("disk_total", groupStat.getTotalMB());
			item.put("disk_free", groupStat.getFreeMB());
			item.put("trunk_free", groupStat.getTrunkFreeMB());
			item.put("storage_server_count", groupStat.getStorageCount());
			item.put("active_server_count", groupStat.getActiveCount());
			item.put("storage_server_port", groupStat.getStoragePort());
			item.put("storage_HTTP_port", groupStat.getStorageHttpPort());
			item.put("store_path_count", groupStat.getStorePathCount());
			item.put("subdir_count_per_path", groupStat.getSubdirCountPerPath());
			item.put("current_write_server_index", groupStat.getCurrentWriteServer());
			item.put("current_trunk_file_id", groupStat.getCurrentTrunkFileId());

			info.add(item);
		}

		return info;
	}

	/**
	 * 关闭已有的连接，删除客户端引用
	 * @throws InterruptedException
	 */
	public void close() throws InterruptedException {

		closing = true;

		try {

			while (idleStorageClient.size() != clientNum) {
				Thread.sleep(50);
			}

			for (TrackerServer ts : trackerServerList) {
				if (ts != null) ts.close();
			}

			for (StorageServer ss : storageServerList) {
				if (ss != null) ss.close();
			}

			trackerServerList.clear();
			storageServerList.clear();
			idleStorageClient.clear();

		} catch (Exception e) {
			logger.error("Error close clients, ", e);
		}

		closing = false;
	}
}
