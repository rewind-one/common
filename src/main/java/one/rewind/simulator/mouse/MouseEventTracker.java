package one.rewind.simulator.mouse;

import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 记录鼠标操作工具类
 */
public class MouseEventTracker {

	private static final Logger logger = LogManager.getLogger(MouseEventTracker.class.getName());

	// 记录文件夹路径
	public static final String serPath = "mouse_movements/";

	// 上一次操作时间
	private long lastTs = System.currentTimeMillis();

	// 事件监听器
	private MouseListener listener;

	private volatile boolean pressed = false;

	// 储存事件列表
	public List<Action> actions = new ArrayList<>();


	/**
	 * 继承 NativeMouseInputListener
	 * 自定义事件监听处理方法
	 * 鼠标左键按下之后 开始记录
	 */
	class MouseListener implements NativeMouseInputListener {

		// 鼠标点击
		public void nativeMouseClicked(NativeMouseEvent e) {
			/*logger.info("Mouse Clicked: " + e.getClickCount());*/
		}

		// 鼠标左键按下
		public void nativeMousePressed(NativeMouseEvent e) {

			/*logger.info("Mouse Pressed: " + e.getButton());*/
			pressed = true;
			MouseEventTracker.this.actions.add(
					new Action(Action.Type.Press, e.getX(), e.getY(),
							System.currentTimeMillis() - MouseEventTracker.this.lastTs));
		}

		// 鼠标左键释放
		public void nativeMouseReleased(NativeMouseEvent e) {

			/*logger.info("Mouse Released: " + e.getButton());*/
			MouseEventTracker.this.actions.add(
					new Action(Action.Type.Release, e.getX(), e.getY(),
							System.currentTimeMillis() - MouseEventTracker.this.lastTs));
			MouseEventTracker.this.stop();
		}

		// 鼠标移动
		public void nativeMouseMoved(NativeMouseEvent e) {

			/*logger.info("Mouse Moved: " + e.getX() + ", " + e.getY());*/
			if(pressed) {
				MouseEventTracker.this.actions.add(
						new Action(Action.Type.Move, e.getX(), e.getY(),
								System.currentTimeMillis() - MouseEventTracker.this.lastTs));
			}
		}

		// 鼠标拖拽
		public void nativeMouseDragged(NativeMouseEvent e) {

			/*logger.info("Mouse Dragged: " + e.getX() + ", " + e.getY());*/
			if(pressed) {
				MouseEventTracker.this.actions.add(
						new Action(Action.Type.Drag, e.getX(), e.getY(),
								System.currentTimeMillis() - MouseEventTracker.this.lastTs));
			}
		}
	}

	public MouseEventTracker() {}

	/**
	 * 开始记录
	 */
	public void start() {

		try {
			// 定义Logger输出
			java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
			logger.setLevel(Level.OFF);
			logger.setUseParentHandlers(false);

			// 注册Hook
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException e) {
			logger.error("There was a problem registering the native hook.", e);
			return;
		}

		// 初始化监听器
		listener = new MouseListener();
		// 注册监听器
		GlobalScreen.addNativeMouseListener(listener);
		GlobalScreen.addNativeMouseMotionListener(listener);

	}

	/**
	 * 停止监听 销毁listener 和 hook
	 */
	public void stop() {

		try {
			GlobalScreen.removeNativeMouseListener(listener);
			GlobalScreen.removeNativeMouseMotionListener(listener);
			GlobalScreen.unregisterNativeHook();
		} catch (NativeHookException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 序列化
	 */
	public void serializeMovements() {

		FileOutputStream fileOut = null;

		try {

			String path = serPath + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ".txt";
			FileUtil.writeBytesToFile(JSON.toPrettyJson(this.actions).getBytes(), path);

		} catch (Exception e) {
			logger.error("Error serialize mouse actions.");
		}
	}
}