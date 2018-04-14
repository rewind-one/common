package one.rewind.monitor;

import one.rewind.monitor.sensors.LocalSensor;
import one.rewind.monitor.sensors.LocalSensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CPUInfo extends SysInfo {

	/**
	 * io等待时间，取值范围0-1，值越大证明io读写等待时间越长
	 */
	public float io_wait = 0;

	/**
	 * cpu等待时间，取值范围0-1，值越大证明cpu空闲时间越长
	 */
	public float idle = 0;

	/**
	 * cpu平均温度
	 */
	public float avg_temp = 0;

	/**
	 * cpu最高温度
	 */
	public float max_temp = 0;

	/**
	 * 当前使用百分比
	 */
	public float usage = 0;

	public Date time = new Date();

	@Override
	public void probe() {

		String timeInfo = LocalSensor.getLocalShellOutput("sar -u 1 1");
		String tempInfo = LocalSensor.getLocalShellOutput("sensors");

		String[] lines = null;
		lines = timeInfo.split("\n");

		for(String line : lines){
			line = line.trim();
			String[] res = line.split("\\s+");

			if(res[0].equalsIgnoreCase("Average:")){

				io_wait = Float.parseFloat(res[5]) / 100;
				idle = Float.parseFloat(res[7]) / 100;
			}
		}

		lines = tempInfo.split("\n");

		int i = 0;
		float currTemp = 0;
		// 记录每一台主机多个cpu的温度
		List<Float> temps = new ArrayList<Float>();

		for(String line : lines){

			if(line.startsWith("Core")){

				i++;
				String[] res = line.split("\\s+");
				currTemp += Float.parseFloat(res[2].replaceAll("\\+|°C", ""));
				temps.add(Float.parseFloat(res[2].replaceAll("\\+|°C", "")));
			}
		}

		Collections.sort(temps);
		max_temp = temps.get(temps.size()-1);
		avg_temp = currTemp / (i);

	}
}
