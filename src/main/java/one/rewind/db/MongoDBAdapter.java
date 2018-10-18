package one.rewind.db;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.typesafe.config.ConfigFactory;
import one.rewind.txt.DateFormatUtil;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import one.rewind.util.Configs;
import one.rewind.txt.DateFormatUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDB 连接适配器
 * @author karajan@tfelab.org
 * 在获取单例对象前可以执行 setConfigString(cfg) 手动制定连接配置参数
 */
public class MongoDBAdapter {

	public final static Logger logger = LogManager.getLogger(MongoDBAdapter.class.getName());

	private static String configString = null;

	private static class MongoDBAdapterHandler {
		private static final MongoDBAdapter INSTANCE = new MongoDBAdapter(configString);
	}

	public static MongoDBAdapter getInstance() {
		return MongoDBAdapterHandler.INSTANCE;
	}

	/**
	 * 设置连接配置
	 * 获取单例实例前使用
	 * @param cfg
	 */
	public static void setConfigString(String cfg) {
		configString = cfg;
	}

	public final MongoClient mongoClient;

	/**
	 * 初始化MongoDB适配器
	 */
	private MongoDBAdapter(String configString) {

		List<ServerAddress> seeds = new ArrayList<>();
		MongoCredential credential = null;

		try {

			com.typesafe.config.Config config = null;

			if (configString != null) {
				config = ConfigFactory.parseString(configString);
			} else {
				config = Configs.getConfig(MongoDBAdapter.class);
			}

			// 获取 seeds
			List<? extends com.typesafe.config.Config> mongoSeeds = config.getConfigList("seeds");

			for (com.typesafe.config.Config seed : mongoSeeds) {
				ServerAddress addr = new ServerAddress(seed.getString("host"), seed.getInt("port"));
				seeds.add(addr);
			}

			// 用户名密码
			com.typesafe.config.Config mongoCredential = config.getConfig("credential");
			credential = MongoCredential.createCredential(mongoCredential.getString("user"),
					mongoCredential.getString("db"), mongoCredential.getString("password").toCharArray());

			logger.info("Connecting MongoDB...");

			mongoClient = new MongoClient(seeds, credential, getConfOptions());
			ServerAddress address = mongoClient.getAddress();

			logger.info("Connected to MongoDB:{}", address);

		} catch (Throwable err) {
			logger.error("Error connect to MongoDB, {}", err.getMessage());
			throw err;
		}
	}

	/**
	 * 获取客户端配置对象
	 * @return
	 */
	private static MongoClientOptions getConfOptions() {

		return new MongoClientOptions.Builder()
				.connectTimeout(5000) // 链接超时时间
				.socketTimeout(20000) // read数据超时时间
				.readPreference(ReadPreference.primary()) // 最近优先策略
				.connectionsPerHost(30) // 每个地址最大请求数
				.maxWaitTime(1000 * 60 * 2) // 长链接的最大等待时间
				.threadsAllowedToBlockForConnectionMultiplier(50) // 一个socket最大的等待请求数
				.writeConcern(WriteConcern.MAJORITY).build();
	}

	/**
	 * 根据条件获取数据列表
	 * 
	 * @param db
	 * @param collection
	 * @param limit
	 * @param skip
	 * @return
	 */
	public List<Document> getData(String db, String collection, int limit, int skip) {

		List<Document> documents = new ArrayList<Document>();

		MongoDatabase mdb = mongoClient.getDatabase(db);
		MongoCollection<Document> coll = mdb.getCollection(collection);
		MongoCursor<Document> cursor = coll.find().limit(limit).skip(skip).iterator();

		while (cursor.hasNext()) {

			documents.add(cursor.next());
		}

		return documents;
	}

	/**
	 * 得到指定collection中的总记录数
	 * 
	 * @param db
	 * @param collection
	 * @return
	 */
	public long getTotalRecord(String db, String collection) {

		MongoDatabase mdb = mongoClient.getDatabase(db);
		MongoCollection<Document> coll = mdb.getCollection(collection);
		return coll.count();
	}

	/**
	 * 根据开始时间和结束时间查找 Document
	 * @param db
	 * @param collection
	 * @param sd
	 * @param ed
	 * @return
	 */
	public List<Document> getDocuments(String db, String collection, String sd, String ed) {
		List<Document> documents = new ArrayList<Document>();

		MongoDatabase mdb = mongoClient.getDatabase(db);
		MongoCollection<Document> coll = mdb.getCollection(collection);

		BasicDBObject dateRange = new BasicDBObject("$gte", DateFormatUtil.parseTime(sd));
		dateRange.put("$lt", DateFormatUtil.parseTime(ed));
		BasicDBObject query = new BasicDBObject("insert_time", dateRange);
		MongoCursor<Document> cursors = coll.find(query).iterator();
		while (cursors.hasNext()) {

			documents.add(cursors.next());
		}

		return documents;
	}
	
	/**
	 * 根据日期范围查询数据
	 * @param db
	 * @param collection
	 * @param sd
	 * @param ed
	 * @return List<Document>
	 */
	
	public List<Document> getDocuments(String db, String collection, Date sd, Date ed) {
		List<Document> documents = new ArrayList<Document>();

		MongoDatabase mdb = mongoClient.getDatabase(db);
		MongoCollection<Document> coll = mdb.getCollection(collection);

		try {
			BasicDBObject dateRange = new BasicDBObject("$gte", sd);
			dateRange.put("$lt", ed);
			BasicDBObject query = new BasicDBObject("insert_time", dateRange);
			MongoCursor<Document> cursors = coll.find(query).iterator();
			while (cursors.hasNext()) {
				
				documents.add(cursors.next());
			}
			
			return documents;

		} catch (Exception e) {
			logger.error("Get documents from mongodb with time 2 time failed" + e);
		}
		return null;
	}

	/**
	 * 
	 * @param db
	 * @param collection
	 * @param dataMap
	 */
	public boolean save(String db, String collection, Map<String, Object> dataMap) {

		try {
			MongoDatabase mdb = mongoClient.getDatabase(db);
			MongoCollection<Document> coll = mdb.getCollection(collection);

			if (dataMap.get("_id") == null) {
				if (dataMap.get("id") != null) {
					dataMap.put("_id", dataMap.get("id"));
					dataMap.remove("id");
				} else {
					return false;
				}
			}

			Document doc = new Document(dataMap);

			/**
			 * 存在相同"_id"的doc即更新 不存在即插入
			 */
			coll.replaceOne(eq("_id", dataMap.get("_id")), doc, (new UpdateOptions()).upsert(true));

			// coll.insertOne(doc);
			return true;

		} catch (Throwable e) {
			logger.error("Error insert data into mongo.", e);
			return false;
		}
	}

	public void close() {
		mongoClient.close();
	}
}
