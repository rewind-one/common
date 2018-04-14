package one.rewind.monitor.test;

import one.rewind.monitor.CPUInfo;
import one.rewind.monitor.IoInfo;
import one.rewind.monitor.MemInfo;
import one.rewind.monitor.NetInfo;
import one.rewind.monitor.sensors.LocalSensor;
import org.junit.Test;
import one.rewind.monitor.CPUInfo;
import one.rewind.monitor.IoInfo;
import one.rewind.monitor.MemInfo;
import one.rewind.monitor.NetInfo;
import one.rewind.monitor.sensors.LocalSensor;
import one.rewind.txt.StringUtil;

import static org.junit.Assert.assertEquals;

public class LocalSensorTest {
	@Test
	public void testCPUInfo() {

		LocalSensor<CPUInfo> sensor = new LocalSensor<>();
		System.out.println(sensor.get(new CPUInfo()).toJSON());
	}

	@Test
	public void testIoInfo() {

		LocalSensor<IoInfo> sensor = new LocalSensor<>();
		System.out.println(sensor.get(new IoInfo()).toJSON());
	}

	@Test
	public void testMemInfo() {

		LocalSensor<MemInfo> sensor = new LocalSensor<>();
		System.out.println(sensor.get(new MemInfo()).toJSON());
	}

	@Test
	public void testNetInfo() {

		LocalSensor<NetInfo> sensor = new LocalSensor<>();
		System.out.println(sensor.get(new NetInfo()).toJSON());
	}
}