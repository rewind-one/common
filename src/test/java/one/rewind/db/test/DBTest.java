package one.rewind.db.test;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Test;
import one.rewind.db.MongoDBAdapter;

import java.util.List;

public class DBTest {
	@Test
	public void testMongoDBAdapter() {

		long t1 = System.currentTimeMillis();

		try {
			MongoDBAdapter.getInstance();
		} catch (Throwable err) {
			MongoDBAdapter.logger.error("MongoDB init error.");
		}

		// for(int i=0; i<10000; i++){
		//
		// Map<String, Object> dataMap = new HashMap<String, Object>();
		// dataMap.put("_id", String.valueOf(i));
		// dataMap.put("姓名", "zhh");
		// dataMap.put("地址", "北京上地");
		// dataMap.put("school", "安阳师范");
		//
		// getInstance().save("tetra", "user", dataMap);
		// }

		System.err.print((System.currentTimeMillis() - t1) / 1000 + " s");

		MongoDBAdapter mongoAdapter = MongoDBAdapter.getInstance();
		MongoDatabase mdb = mongoAdapter.mongoClient.getDatabase("raw");

		MongoCollection<Document> coll = mdb.getCollection("one.rewind.crawler.model.Essay");
		BasicDBList query = new BasicDBList();

		List<Document> documents = mongoAdapter.getDocuments("raw", "one.rewind.crawler.model.Essay",
				"2016-05-26 00:38:17", "2016-06-01 00:38:17");
		System.out.println(documents.size());

		MongoDBAdapter.getInstance().close();
	}
}
