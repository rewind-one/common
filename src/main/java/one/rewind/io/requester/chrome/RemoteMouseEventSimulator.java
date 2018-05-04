package one.rewind.io.requester.chrome;

import one.rewind.io.ssh.RemoteShell;
import one.rewind.simulator.mouse.Action;
import one.rewind.simulator.mouse.MouseEventSimulator;

import java.awt.*;
import java.util.List;

/**
 * 基于Tracker序列化记录，模拟苏杭表操作
 */
public class RemoteMouseEventSimulator extends MouseEventSimulator {

	RemoteShell remoteShell;

	/**
	 *
	 * @throws AWTException
	 */
	public RemoteMouseEventSimulator(List<Action> actions, RemoteShell remoteShell) {

		super(actions);
		this.remoteShell = remoteShell;
	}

	/**
	 *
	 * @return
	 */
	public String buildShellCmd() {

		String cmd = null;

		cmd += "sleep " + (float) actions.get(0).time/1000 + "\n";

		for(int i=0; i<actions.size(); i++) {

			Action action = actions.get(i);

			// 按下鼠标左键
			if(action.type.equals(Action.Type.Press)) {
				cmd += "xdotool mousedown 1\n";
			}
			// 释放鼠标左键
			else if(action.type.equals(Action.Type.Release)) {
				cmd += "xdotool mouseup 1\n";
			}
			// 移动鼠标
			else if(action.type.equals(Action.Type.Move)) {
				cmd += "xdotool mouseup 1\n";
			}
			// 拖拽
			else {
				cmd += "xdotool mousemove " + action.x + " " + action.y  + "\n";
			}

			if(i < actions.size() - 1) {

				float sleepTime = ((float) (actions.get(i+1).time - action.time)) / 1000;
				cmd += "sleep " + (float) sleepTime/1000 + "\n";
			}
		}

		return cmd;
	}


	/**
	 * 执行事件
	 */
	public void procActions() {

		remoteShell.exec(buildShellCmd());
	}
}
