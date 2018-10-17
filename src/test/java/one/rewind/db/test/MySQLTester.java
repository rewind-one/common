package one.rewind.db.test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MySQLTester extends Thread {

	Connection conn;
	Statement stmt;
	PreparedStatement pstmt;

	int col_num;
	String type;
	int data_vol;
	int batch_vol;

	private final CountDownLatch doneSignal;

	/**
	 *
	 * @param host
	 * @param port
	 * @param dbname
	 * @param user
	 * @param passwd
	 */
	public MySQLTester(String host, int port, String dbname, String user,
					   String passwd, String type, int col_num, int data_vol,
					   int batch_vol, CountDownLatch doneSignal) {

		try {
			/* Load Connector/J driver */
			Class.forName("com.mysql.jdbc.Driver").newInstance();

			/* Establish MySQL connection */
			this.conn = DriverManager
					.getConnection(
							"jdbc:mysql://"
									+ host
									+ ":"
									+ port
									+ "/"
									+ dbname
									+ "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useCompression=true",
									user, passwd);

			/* Execute SQL queries */
			this.stmt = this.conn.createStatement();

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.type = type;
		this.col_num = col_num;
		this.data_vol = data_vol;
		this.batch_vol = batch_vol;
		this.doneSignal = doneSignal;

		createTable();
	}

	/**
	 * 创建表
	 */
	void createTable() {

		try {

			this.stmt.executeUpdate("DROP TABLE IF EXISTS `test`");

			/*
			 * stmt.executeUpdate("CREATE TABLE `test` ("
			 * + "`id` INT NOT NULL AUTO_INCREMENT,"
			 * +
			 * "`val` DOUBLE NOT NULL DEFAULT '0', PRIMARY KEY (`id`), INDEX `Index 2` (`val`))"
			 * + "COLLATE='utf8_general_ci'"
			 * + "ENGINE=MyISAM;");
			 */

			String sql_create = "CREATE TABLE `test` (";
			String sql_pstat = "INSERT INTO test VALUES(";

			for (int i = 0; i < this.col_num; i++) {
				sql_create += "`val" + i + "` DOUBLE NOT NULL DEFAULT '0',";
				sql_pstat += "?,";
			}

			sql_create = sql_create.substring(0, sql_create.length() - 1);
			sql_pstat = sql_pstat.substring(0, sql_pstat.length() - 1);

			sql_create += ") " + "COLLATE='utf8_general_ci' "
					+ "ROW_FORMAT=FIXED " + "ENGINE=MyISAM";

			sql_pstat += ")";

			this.stmt.execute(sql_create);

			this.stmt.setFetchSize(Integer.MIN_VALUE);

			this.pstmt = this.conn.prepareStatement(sql_pstat);
			this.pstmt.setFetchSize(Integer.MIN_VALUE);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 逐笔插入测试
	 *
	 * @param num
	 */
	public void test(long num) {

		double d;
		Random r = new Random();

		try {

			for (int i = 0; i < num; i++) {

				d = r.nextDouble();

				String sql = "INSERT INTO test VALUES(";

				for (int j = 0; j < this.col_num; j++) {
					sql += d + ",";
				}

				sql = sql.substring(0, sql.length() - 1);

				sql += ")";

				this.stmt.executeUpdate(sql);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * sql连接测试
	 *
	 * @param num
	 */
	public void testJoin(long num) {

		try {

			double d;
			Random r = new Random();
			for (int i = 0; i < num / this.batch_vol; i++) {

				String sql = "INSERT INTO test VALUES ";

				for (int j = 0; j < this.batch_vol
						&& i * this.batch_vol + j < num; j++) {

					d = r.nextDouble();

					sql += "(";

					for (int k = 0; k < this.col_num; k++) {
						sql += d + ",";
					}

					sql = sql.substring(0, sql.length() - 1);
					sql += "),";
				}

				sql = sql.substring(0, sql.length() - 1);

				this.stmt.executeUpdate(sql);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * pstat测试
	 *
	 * @param num
	 */
	public void testBatch(long num) {

		try {
			this.conn.setAutoCommit(false);

			double d;
			Random r = new Random();
			for (int i = 0; i < num / this.batch_vol; i++) {

				// System.out.println(i);

				for (int j = 0; j < this.batch_vol
						&& i * this.batch_vol + j < num; j++) {

					d = r.nextDouble();

					for (int k = 0; k < this.col_num; k++) {
						this.pstmt.setDouble(k + 1, d);
					}
					this.pstmt.addBatch();
				}

				try {
					this.pstmt.executeBatch();
					this.conn.commit();
					this.pstmt.clearBatch();

				} catch (SQLException e) {
					e.printStackTrace();
				}

			}

			this.conn.setAutoCommit(true);

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 */
	@Override
	public void run() {

		// long t1 = System.currentTimeMillis();

		switch (this.type) {
			case "":
				test(this.data_vol);
				break;
			case "batch":
				testBatch(this.data_vol);
				break;
			case "join":
				testJoin(this.data_vol);
				break;
		}

		try {
			this.pstmt.close();
			this.stmt.close();
			this.conn.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}

		this.doneSignal.countDown();

		// System.out.println(this.getName() + " : " + ((double)
		// (System.currentTimeMillis()-t1))/1000 + "\t");
	}

	/**
	 * 单次测试
	 *
	 * @param col_num
	 * @param batch_vol
	 * @param total_data_vol
	 * @param tester_count
	 * @param type
	 */
	public static void makeTest(int col_num, int batch_vol,
			long total_data_vol, int tester_count, String type) {

		int data_vol = (int) total_data_vol / tester_count;
		List<MySQLTester> testers = new ArrayList<MySQLTester>();

		CountDownLatch doneSignal = new CountDownLatch(tester_count);

		for (int i = 0; i < tester_count; i++) {
			testers.add(new MySQLTester("10.0.0.76", 3306, "market_data",
					"data", "data", type, col_num, data_vol, batch_vol,
					doneSignal));
		}

		long t1 = System.currentTimeMillis();

		for (MySQLTester tester : testers) {
			tester.start();
		}

		try {

			doneSignal.await();
			System.out.println((double) (System.currentTimeMillis() - t1)
					/ 1000 + "\t");

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 主测试方法
	 *
	 * @param args
	 */
	public static void main(String[] args) {

		// int col_num = 6; // 插入数据 = 8 * col_num + 1 (byte)
		// int batch_vol = 10; // 分批插入时候的编组数量

		long total_data_vol = 100000; // 总数据量
		// int tester_count = 8; // 客户端线程数

		String type = "join";

		// int col_nums[] = {1, 2, 4, 8, 16};
		int col_nums[] = { 8, 16 };
		// int batch_vols[] = {5, 10, 20, 50, 100, 200, 500, 1000};
		int batch_vols[] = { 10, 20, 50, 100 };
		// int tester_counts[] = {1, 2, 3, 4, 5, 6, 7, 8};
		int tester_counts[] = { 4, 8 };

		for (int col_num : col_nums) {
			for (int batch_vol : batch_vols) {
				for (int tester_count : tester_counts) {

					System.out.print("C: " + col_num + ", B: " + batch_vol
							+ ", T: " + tester_count + ". R: ");

					makeTest(col_num, batch_vol, total_data_vol, tester_count,
							type);
				}
			}
		}

		/**/
		/*
		 * int nums[] = {10*1000, 20*1000, 50*1000, 100*1000, 200*1000,
		 * 500*1000, 1*1000*1000};
		 * System.out.println("BEGIN SIMPLE INSERT");
		 * for(int num : nums){
		 * tester.createTable();
		 * t1 = System.currentTimeMillis();
		 * tester.test(num);
		 * System.out.print(((double) (System.currentTimeMillis()-t1))/1000 +
		 * "\t");
		 * }
		 * System.out.println();
		 * System.out.println("BEGIN BATCH INSERT");
		 * for(int num : nums){
		 * tester.createTable();
		 * t1 = System.currentTimeMillis();
		 * tester.testBatch(num);
		 * System.out.print(((double) (System.currentTimeMillis()-t1))/1000 +
		 * "\t");
		 * }
		 * System.out.println();
		 * /*
		 */
	}

}
