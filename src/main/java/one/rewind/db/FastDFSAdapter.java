package one.rewind.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import java.util.concurrent.ArrayBlockingQueue;

public class FastDFSAdapter {

	public final static Logger logger = LogManager.getLogger(RedissonAdapter.class.getName());

	public static FastDFSAdapter instance;

	//最大连接数
	private int size = 5;

	//空闲的连接
	private ArrayBlockingQueue<StorageClient> idleStorageClient = null;

	/**
	 * 单例方法
	 * @return
	 */
	public static FastDFSAdapter getInstance() throws Exception {

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
	public FastDFSAdapter (){

		// 初始化 StorageClient 队列
		for( int i = 0 ; i < size; i++){
			creatStorageClient();
		}

	}

	/**
	 *  创建StorageClient
	 */
	public void creatStorageClient(){

		try {

			// 定义 配置文件路径
			String conf_filename = "tmp/fastdfs.conf";

			// 创建 client链接
			ClientGlobal.init(conf_filename);

			TrackerClient tracker = new TrackerClient();
			TrackerServer trackerServer = tracker.getConnection();
			StorageServer storageServer = null;

			StorageClient client = new StorageClient(trackerServer, storageServer);

			// 将链接放入队列
			idleStorageClient.offer(client);

		} catch (Exception e){

			logger.error("error for creat FastDFS Client");
		}
	}

	/**
	 * 获取StorageClient
	 */
	public synchronized StorageClient getStorageClient(){

		// 获取链接
		if( idleStorageClient.size() == 0 ){
			creatStorageClient();
		}
		StorageClient storageClient = idleStorageClient.poll();

		return storageClient;
	}


	/**
	 * 上传文件
	 */
	String[] upload_file(byte[] file_buff, String file_ext_name, NameValuePair[] meta_list) throws Exception{

		// 获取链接
		StorageClient storageClient = getStorageClient();

		String[] info = null;

		try{
			// 上传到 FastDFS
			info = storageClient.upload_file(file_buff, file_ext_name, meta_list);

			// 将 链接放入 队列
			idleStorageClient.offer(storageClient);

		} catch (Exception e) {

			// 移除报错链接
			idleStorageClient.remove(storageClient);
			logger.error("error for upLoadFile ", e);
		}

		return info;
	}

	/**
	 * 下载文件
	 */
	public byte[] downLaodFile(String groupName, String filePath ){

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 下载文件
		byte[] bytes = null;

		try {
			bytes = storageClient.download_file(groupName, filePath);
		} catch (Exception e) {
			idleStorageClient.remove(storageClient);
			logger.error("error for downLoadFile", e);
		}

		return bytes;
	}

	/**
	 * 获取文件信息
	 */
	public FileInfo getFileInfo( String groupName, String filePath ){

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 获取文件信息
		FileInfo file = null;
		try {
			file = storageClient.get_file_info(groupName, filePath);
			idleStorageClient.remove(storageClient);
		} catch (Exception e) {
			idleStorageClient.remove(storageClient);
			logger.error("error fro getFileInfo", e);
		}

		return file;
	}

	/**
	 * 删除文件
	 */
	public boolean deleteFile( String groupName, String filePath  ){

		// 获取链接
		StorageClient storageClient = getStorageClient();

		// 删除文件
		int i = 1;
		try {
			i = storageClient.delete_file(groupName, filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return i==0 ? true : false;
	}

}
