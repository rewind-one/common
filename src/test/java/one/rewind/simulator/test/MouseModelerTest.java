package one.rewind.simulator.test;

import one.rewind.io.requester.chrome.RemoteMouseEventSimulator;
import one.rewind.simulator.mouse.Action;
import one.rewind.simulator.mouse.MouseEventModeler;
import one.rewind.simulator.mouse.MouseEventSimulator;
import org.junit.Test;

import java.util.List;

public class MouseModelerTest {

	@Test
	public void test() throws Exception {
		List<Action> actions = MouseEventModeler.getInstance().getActions(0, 0, 100);

		RemoteMouseEventSimulator simulator = new RemoteMouseEventSimulator(actions, null);

		System.err.println(simulator.buildShellCmd());
	}
}
