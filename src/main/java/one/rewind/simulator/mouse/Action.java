package one.rewind.simulator.mouse;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.io.Serializable;

/**
 * 基础鼠标事件
 */
public class Action implements JSONable, Serializable {

	/**
	 *
	 */
	public static enum Type {
		Press, // 左键按下
		Release, // 左键释放
		Move, // 鼠标移动
		Drag // 鼠标拖拽
	}

	public Action.Type type;
	public int x;
	public int y;
	public long time;

	public Action() {}

	public Action(Action.Type type, int x, int y, long time) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.time = time;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}