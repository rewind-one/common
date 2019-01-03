package one.rewind.io.requester.parser;

import one.rewind.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;

/**
 *
 */
public class Evaluator {

	public static final Logger logger = LogManager.getLogger(Evaluator.class.getName());

	public static Evaluator instance;

	/**
	 * 单例方法
	 * @return
	 */
	public static Evaluator getInstance() throws ScriptException {

		if (instance == null) {

			synchronized(Evaluator.class) {
				if (instance == null) {
					instance = new Evaluator();
				}
			}
		}

		return instance;
	}

	//串行执行脚本的最大次数
	public static int EvalMaxNum = 100;

	private ScriptEngine jsEngine;

	private volatile int evalCount;

	/**
	 *
	 * @throws ScriptException
	 */
	private Evaluator() throws ScriptException {
		jsEngine = getJSEngine();
	}

	/**
	 *
	 * 串行执行脚本，执行次数超过EvalMaxNum时,创建新的ScriptEngine对象
	 * @param expr
	 * @return
	 * @throws ScriptException
	 */
	public Object serialEval(String expr) throws ScriptException {

		if(evalCount ++ > EvalMaxNum) {
			jsEngine = getJSEngine();
		}

		return jsEngine.eval(expr);
	}

	/**
	 *
	 * 并行执行脚本
	 * @param expr
	 * @return
	 * @throws ScriptException
	 */
	public Object parallelEval(String expr) throws ScriptException {

		return getJSEngine().eval(expr);
	}

	/**
	 *
	 * 创建ScriptEngine对象，执行scripts目录下所有的javascript脚本代码
	 * @return
	 * @throws ScriptException
	 */
	public static ScriptEngine getJSEngine() throws ScriptException {

		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

		for (final File fileEntry : new File("scripts").listFiles()) {
			if (!fileEntry.isDirectory()) {

				engine.eval(FileUtil.readFileByLines(fileEntry.getAbsolutePath()));
			}
		}

		return engine;
	}

}
