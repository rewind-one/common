package one.rewind.monitor.sensors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.monitor.SysInfo;
import one.rewind.util.EnvUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LocalSensor<Info extends SysInfo> {

	private static Logger logger = LogManager.getLogger(LocalSensor.class.getName());

	/**
	 *
	 * @param cmd
	 * @return
	 */
	public static String getLocalShellOutput(String cmd) {

		String output = "";

		Process pro = null;
		BufferedReader reader = null;

		try {

			Runtime r = Runtime.getRuntime();
			pro = r.exec(cmd);
			reader = new BufferedReader(new InputStreamReader(pro.getInputStream()));

			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				output += tempString + "\n";
			}

		} catch (IOException e) {

			logger.error(e);

		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
			if(pro != null)
				pro.destroy();
		}

		return output;
	}

	public Info get(Info info) {

		if(EnvUtil.isHostLinux()){
			info.probe();
		}
		return info;
	}
}
