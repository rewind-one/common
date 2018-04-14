package one.rewind.monitor;

import one.rewind.monitor.sensors.LocalSensor;
import one.rewind.monitor.sensors.LocalSensor;

import java.util.Date;

public class NetInfo extends SysInfo {

	/**
	 * 本节点下行速度 MB/s
	 */
	public double in_rate;

	/**
	 * 本节点上行速度 MB/s
	 */
	public double out_rate;

	/**
	 * 本节点总速度 MB/s
	 */
	public double total_rate;

	public Date time = new Date();

	@Override
	public void probe() {

		long startTime = System.currentTimeMillis();
		String netInfo = LocalSensor.getLocalShellOutput("cat /proc/net/dev");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		String netInfo2 = LocalSensor.getLocalShellOutput("cat /proc/net/dev");


		String[] lines = netInfo.split("\n");

		try {

			long inSize1 = 0, outSize1 = 0;
			for (String line : lines) {

				line = line.trim();

				if (!(line.startsWith("Inter") || line.startsWith("face") || line.startsWith("lo"))) {
					// logger.trace(line);
					String[] temp = line.split("\\s+");
					// Receive bytes,单位为Byte
					inSize1 += Long.parseLong(temp[1]);
					// Transmit bytes,单位为Byte
					outSize1 += Long.parseLong(temp[9]);
				}
				// logger.trace("in: " + inSize1 + "\tout: " + outSize1);
			}
			// 第二次采集流量数据
			long inSize2 = 0, outSize2 = 0;
			lines = netInfo2.split("\n");

			for (String line : lines) {
				line = line.trim();
				if (!(line.startsWith("Inter") || line.startsWith("face") || line.startsWith("lo"))) {
					// logger.trace(line);
					String[] temp = line.split("\\s+");
					// Receive bytes,单位为Byte
					inSize2 += Long.parseLong(temp[1]);
					// Transmit bytes,单位为Byte
					outSize2 += Long.parseLong(temp[9]);
				}
				// logger.trace("in: " + inSize1 + "\tout: " + outSize1);
			}

			if (inSize1 != 0 && outSize1 != 0 && inSize2 != 0 && outSize2 != 0) {

				float interval = (float) (endTime - startTime) / 1000;
				// 网口传输速度
				in_rate = (inSize2 - inSize1) / (1000 * 1000 * interval);
				out_rate = (outSize2 - outSize1) / (1000 * 1000 * interval);
				total_rate = in_rate + out_rate;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
