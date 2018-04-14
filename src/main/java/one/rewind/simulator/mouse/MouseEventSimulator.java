package one.rewind.simulator.mouse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	// 用于模拟鼠标操作
	Robot bot;

	/**
	 *
	 * @throws AWTException
	 */
	public MouseEventSimulator(List<Action> actions) throws AWTException {

		this.actions = actions;
		bot = new Robot();
	}

	/**
	 *
	 * @param offset
	 */
	public void simulate(int offset) {

		offset = offset + ThreadLocalRandom.current().nextInt(-4, 4 + 1);
		long ts = 0;

		int[] p1 = new int[2];
		int[] p2 = new int[2];

		for(Action action : this.actions) {
			if(action.type.equals(Action.Type.Press)) {
				p1[0] = action.x;
				p1[1] = action.y;
			} else if(action.type.equals(Action.Type.Release)) {
				p2[0] = action.x;
				p2[1] = action.y;
			}
		}

		double width = p2[0] - p1[0];
		double scale = (double) offset / width;
		logger.info("Original width:{}, scale:{}.", width, scale);

		for(Action action : this.actions) {

			// 按下鼠标左键
			if(action.type.equals(Action.Type.Press)) {
				bot.mousePress(InputEvent.BUTTON1_MASK);
			}
			// 释放鼠标左键
			else if(action.type.equals(Action.Type.Release)) {
				bot.mouseRelease(InputEvent.BUTTON1_MASK);
			}
			// 移动鼠标
			else if(action.type.equals(Action.Type.Move)) {
				bot.mouseMove(action.x, action.y);
			}
			//
			else {
				bot.mouseMove(p1[0] + (int) Math.round ((action.x-p1[0]) * scale),
						p1[1] + (int) Math.round ((action.y-p1[1]) * scale));
			}
			bot.delay((int) (action.time - ts));
			ts = action.time;

		}
	}

	/**
	 * 执行事件
	 */
	public void procActions() {

		long ts = 0;

		for(Action action : this.actions) {

			// 按下鼠标左键
			if(action.type.equals(Action.Type.Press)) {
				bot.mousePress(InputEvent.BUTTON1_MASK);
			}
			// 释放鼠标左键
			else if(action.type.equals(Action.Type.Release)) {
				bot.mouseRelease(InputEvent.BUTTON1_MASK);
			}
			// 移动鼠标
			else if(action.type.equals(Action.Type.Move)) {
				bot.mouseMove(action.x, action.y);
			}
			// 拖拽
			else {
				bot.mouseMove(action.x, action.y);
			}

			bot.delay((int) (action.time - ts));
			ts = action.time;
			//System.err.println(action.toJSON());
		}

		logger.info("Final position --> x:{}, y:{}.", actions.get(actions.size()-1).x, actions.get(actions.size()-1).y);
	}
}
