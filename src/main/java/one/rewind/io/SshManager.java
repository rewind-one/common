package one.rewind.io;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SshManager {

	public static final Logger logger = LogManager.getLogger(SshManager.class.getName());
	
	private Map<String, Host> hosts = new ConcurrentHashMap<String, Host>();
	
	private static SshManager instance;

	/**
	 *
	 * @return
	 */
	public synchronized static SshManager getInstance() {
		
		if (instance == null) {
			instance = new SshManager();
		}
		return instance;
	}
	
	/**
	 * 
	 */
	private SshManager(){

	}

	/**
	 *
	 * @param ip
	 * @param port
	 * @param user
	 * @param passwd
	 * @throws IOException
	 */
	public synchronized void initConn(String ip, int port, String user, String passwd) throws IOException{

		if(hosts.get(ip + ":" + port) == null){
			Host host = new Host(ip, port, user, passwd);
			host.connect();
			hosts.put(ip + ":" + port, host);
		} else {
			logger.error("Connection to " + ip + ":" + port + " already established.");
		}
	}

	/**
	 *
	 * @param ip
	 * @param port
	 * @param user
	 * @param pemFile
	 * @throws IOException
	 */
	public synchronized void initConn(String ip, int port, String user, File pemFile) throws IOException{

		if(hosts.get(ip + ":" + port) == null){
			Host host = new Host(ip, port, user, pemFile);
			host.connect();
			hosts.put(ip + ":" + port, host);
		} else {
			logger.error("Connection to " + ip + ":" + port + " already established.");
		}
	}

	/**
	 *
	 */
	public static class Host {
		
		public String ip;
		public int port;
		private String user;
		private String passwd;
		private File pemFile;

		public Connection conn;
		
		public Host(String ip, int port, String user, String passwd) {
			
			this.ip = ip;
			this.port = port;
			this.user = user;
			this.passwd = passwd;
		}

		public Host(String ip, int port, String user, File pemFile) {

			this.ip = ip;
			this.port = port;
			this.user = user;
			this.pemFile = pemFile;
		}

		public String getKey() {
			return this.ip + ":" + this.port;
		}

		public void connect() throws IOException{

			conn = new Connection(ip, port);
			conn.connect();

			boolean isAuthenticated = false;
			if(pemFile != null) {
				isAuthenticated = conn.authenticateWithPublicKey(user, pemFile, null);
			} else {
				isAuthenticated = conn.authenticateWithPassword(user, passwd);
			}

			if (isAuthenticated == false) {
				throw new IOException("Authentication failed.");
			}
		}

		/**
		 *
		 * @param cmd
		 * @return
		 * @throws Exception
		 */
		public String exec(String cmd) throws Exception {

			String output = "";

			try {

				Session sess = conn.openSession();
				sess.execCommand(cmd + "\n");

				InputStream stdout = new StreamGobbler(sess.getStdout());
				BufferedReader in = new BufferedReader(new InputStreamReader(stdout));

				String line = null;

				while ((line = in.readLine()) != null) {
					output += line + "\n";
				}
				in.close();
				sess.close();

				return output;

			} catch (Exception e) {
				logger.error("Error open session. ", e);
				connect();
				throw e;
			}
		}

		public void upload(String localFilePath, String remoteDirectoryPath) throws Exception {

			String output = "";

			try {

				SCPClient scp = new SCPClient(conn);
				scp.put(localFilePath, remoteDirectoryPath);

			} catch (Exception e) {
				logger.error("Error open SCPClient. ", e);
				connect();
			}
		}

		public void download(String remoteFilePath, String localDirectoryPath) throws Exception {

			String output = "";

			try {

				SCPClient scp = new SCPClient(conn);
				scp.get(remoteFilePath, localDirectoryPath);

			} catch (Exception e) {
				logger.error("Error open SCPClient. ", e);
				connect();
			}
		}

	}

	public static void main(String[] args) throws Exception {

		String[] hosts = {
				"47.106.71.20"
		};

		for(String hs : hosts) {

			Host host = new Host(hs, 22, "root", "SDYK315pr");
			host.connect();

			/*String output = host.exec("jps | grep OldCrawler | awk '{print $1}' | xargs kill -9");
			System.err.println(output);*/

			host.upload("squid.sh", "/root");

			String output = host.exec("chmod +x squid.sh");
			System.err.println(output);

			// 先更新，再执行
			host.exec("apt update");
			output = host.exec("./squid.sh");
			System.err.println(output);


		}
	}
}




