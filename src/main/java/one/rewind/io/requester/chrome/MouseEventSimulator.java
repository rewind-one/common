package one.rewind.io.requester.chrome;

import one.rewind.simulator.mouse.Action;
import one.rewind.simulator.mouse.MouseEventTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于Tracker序列化记录，模拟苏杭表操作
 */
public class MouseEventSimulator {

	private static final Logger logger = LogManager.getLogger(MouseEventTracker.class.getName());

	//
	List<Action> actions;
	Actions mouseActions;


	/**
	 *
	 * @throws AWTException
	 */
	public MouseEventSimulator(List<Action> actions, RemoteWebDriver driver) throws AWTException {

		this.actions = actions;
		mouseActions = new Actions(driver);
	}


	/**
	 * 执行事件
	 */
	public void procActions() {

		int dx = actions.get(0).x;
		int dy = actions.get(0).y;
		long t_ = 0;

		for(Action action : this.actions) {

			dx = action.x - dx;
			dy = action.y - dy;

			// 按下鼠标左键
			if(action.type.equals(Action.Type.Press)) {
				mouseActions.clickAndHold();
			}
			// 释放鼠标左键
			else if(action.type.equals(Action.Type.Release)) {
				mouseActions.release();
			}
			// 移动鼠标
			else if(action.type.equals(Action.Type.Move)) {
				mouseActions.moveByOffset(dx, dy);
			}
			// 拖拽
			else {
				mouseActions.moveByOffset(dx, dy);
			}

			mouseActions.pause(action.time - t_);
			t_ = action.time;
		}

		mouseActions.build().perform();

		logger.info("Final position --> x:{}, y:{}.", actions.get(actions.size()-1).x, actions.get(actions.size()-1).y);
	}
}
