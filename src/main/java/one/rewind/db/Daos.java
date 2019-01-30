package one.rewind.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * OrmLite 数据对象类连接管理器
 * @author karajan@tfelab.org
 * 2016年3月26日 下午4:17:05
 */
public class Daos {
	
	public final static Logger logger = LogManager.getLogger(Daos.class.getName());
	
	private static Map<Class<?>, Dao<?, String>> daoMap = new HashMap<Class<?>, Dao<?, String>>();
	
	/**
	 * 获取指定OrmLite 数据对象类的 Dao对象
	 * @param clazz
	 * @return
	 * @throws Exception 
	 * @throws SQLException
	 */
	public static synchronized <T> Dao<T, String> get(Class<T> clazz) throws DBInitException, SQLException {
		
		if (daoMap.containsKey(clazz)) {
			return (Dao<T, String>) daoMap.get(clazz);
		}

		String dbName;

		try {
			dbName = clazz.getAnnotation(DBName.class).value();
		} catch (Exception e) {
			logger.error("Error get dbName annotation for {}.", clazz.getName(), e);
			throw new DBInitException("Error get dbName annotation for " + clazz.getName() + ".");
		}
		
		ConnectionSource source = new DataSourceConnectionSource(
				PooledDataSource.getDataSource(dbName),
				PooledDataSource.getDataSource(dbName).getJdbcUrl()
		);

		Dao<T, String> dao = com.j256.ormlite.dao.DaoManager.createDao(source, clazz);
		daoMap.put(clazz, dao);
		return dao;
	}

	/**
	 * 直接执行SQL语句，需要指定数据库
	 *
	 * @param dbName
	 * @param sql
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static boolean exec(String dbName, String sql) throws DBInitException, SQLException {

		ConnectionSource source = new DataSourceConnectionSource(
				PooledDataSource.getDataSource(dbName),
				PooledDataSource.getDataSource(dbName).getJdbcUrl()
		);

		DatabaseConnection conn = source.getReadWriteConnection();

		boolean result = conn.executeStatement(sql, DatabaseConnection.DEFAULT_RESULT_FLAGS) == DatabaseConnection.DEFAULT_RESULT_FLAGS;
		conn.close();
		return result;
	}
}