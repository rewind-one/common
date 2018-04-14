package one.rewind.monitor;

import one.rewind.monitor.sensors.LocalSensor;

import java.util.Date;

public class MemInfo extends SysInfo {

	public float total = 0;

	public float free = 0;

	public float buffer = 0;

	public float cached = 0;

	public float avail = 0;
	/**
	 * 本节点内存使用情况
	 */
	public float usage = 0;

	public Date time = new Date();

	@Override
	public void probe() {
		String src = LocalSensor.getLocalShellOutput("cat /proc/meminfo");

		String[] lines = src.split("\n");

		for (String line : lines) {
			line = line.trim();
			// MemTotal: 7857556 kB
			String[] memInfo = line.split("\\s+");
			if (memInfo[0].startsWith("MemTotal")) {
				total = Float.parseFloat((memInfo[1])) / 1000;
			}
			if (memInfo[0].startsWith("MemFree")) {
				free = Float.parseFloat((memInfo[1])) / 1000;
			}
			if (memInfo[0].startsWith("Buffers")) {
				buffer = Float.parseFloat((memInfo[1])) / 1000;
			}
			if (memInfo[0].startsWith("Cached")) {
				cached = Float.parseFloat((memInfo[1])) / 1000;
			}
			if (memInfo[0].startsWith("MemAvailable")) {
				avail = Float.parseFloat((memInfo[1])) / 1000;
			}
			/*
			 * 如果服务器不能直接得到内存available的值，available=free+buffer+cached
			 */
			if(avail == 0){
				avail = free + buffer + cached;
			}
			if(avail != 0 && total != 0){
				usage = 1 - avail / total;
			}
		}
	}
}
