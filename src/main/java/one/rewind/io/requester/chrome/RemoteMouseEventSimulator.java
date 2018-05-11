package one.rewind.io.requester.chrome;

import one.rewind.io.ssh.RemoteShell;
import one.rewind.simulator.mouse.Action;
import one.rewind.simulator.mouse.MouseEventSimulator;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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

		NumberFormat nf = new DecimalFormat("0.000");

		String cmd = "";

		cmd += "xdotool mousemove " + actions.get(0).x + " " + actions.get(0).y  + "; ";

		cmd += "sleep " + (float) actions.get(0).time/1000 + ";";

		for(int i=0; i<actions.size(); i++) {

			Action action = actions.get(i);

			// 按下鼠标左键
			if(action.type.equals(Action.Type.Press)) {
				cmd += "xdotool mousedown 1;";
			}
			// 释放鼠标左键
			else if(action.type.equals(Action.Type.Release)) {
				cmd += "xdotool mouseup 1;";
			}
			// 移动鼠标
			else if(action.type.equals(Action.Type.Move)) {
				cmd += "xdotool mouseup 1;";
			}
			// 拖拽
			else {
				cmd += "xdotool mousemove " + action.x + " " + action.y  + ";";
			}

			if(i < actions.size() - 1) {

				float sleepTime = ((float) (actions.get(i+1).time - action.time)) / 1000;

				cmd += "sleep " + nf.format(sleepTime) + "; ";
			}
		}

		return cmd;
	}


	/**
	 * 执行事件
	 */
	public void procActions() {

		String output = remoteShell.exec(buildShellCmd());

		logger.info(output);
	}
}
