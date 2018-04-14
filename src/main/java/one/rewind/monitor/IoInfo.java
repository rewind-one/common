package one.rewind.monitor;

import one.rewind.monitor.sensors.LocalSensor;
import one.rewind.monitor.sensors.LocalSensor;
import one.rewind.txt.NumberFormatUtil;

import java.util.Date;

public class IoInfo extends SysInfo {

	/**
	 * 磁盘总容量以MB为单位
	 */
	public double disk_total = 0;

	/**
	 * 磁盘可用容量 MB
	 */
	public double disk_free = 0;

	/**
	 *  磁盘读取速率 MB/s
	 */
	public float read = 0;

	/**
	 * 磁盘写速率 MB/s
	 */
	public float writen = 0;

	/**
	 * 本节点io使用情况
	 */
	public float usage = 0;

	public Date time = new Date();

	@Override
	public void probe() {

		String diskInfo = LocalSensor.getLocalShellOutput("df -m");
		String rwInfo = LocalSensor.getLocalShellOutput("iostat");

		String[] lines = null;
		lines = diskInfo.split("\n");

		for (String line : lines) {
			line = line.trim();
			if (line.length() > 0) {

				String[] result = line.split("\\s+");
				if (result[5].equals("/")) {

					disk_total = NumberFormatUtil.parseDouble(result[1]);
					disk_free = NumberFormatUtil.parseDouble(result[3]);
				}
			}
		}

		lines = rwInfo.split("\n");
		boolean found = false;
		for (String line : lines) {

			if (line.length() > 0 && line.startsWith("Device") && !found) {
				found = true;
				continue;
			}
			if(found) {
				String[] result = line.split("\\s+");
				read = Float.parseFloat(result[2]) / 1000;
				writen = Float.parseFloat(result[3]) / 1000;
			}
		}

		if(disk_total != 0 && disk_free != 0){
			usage = (float) (1 - disk_free / disk_total);
		}

	}
}
