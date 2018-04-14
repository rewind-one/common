package one.rewind.simulator.mouse;

import com.google.gson.reflect.TypeToken;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.FileUtil;
import org.apache.commons.lang3.math.Fraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.FileUtil;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 对鼠标事件进行建模分析
 * @Author scisaga@gmail.com
 * @Date 2018/4/3
 *
 * 1. 加载鼠标事件
 * 2. 对鼠标轨迹进行分析
 * 2.1 将轨迹进行分组
 *   找到较为平滑的阶段
 *   较为平滑的阶段可以伸缩操作，以自适应不同的位移和时间输入
 *   非平滑阶段可进行相应变形，保留人工关键特征
 * 3. 设计方法，随机选择已有轨迹，对轨迹进行缩减或拓展，生成新的轨迹
 * 4. 对人工响应时间进行找到对应模型，生成事件列表（轨迹）
 */
public class MouseEventModeler {

	protected static MouseEventModeler instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static MouseEventModeler getInstance() {

		if (instance == null) {

			synchronized (MouseEventModeler.class) {
				if (instance == null) {
					instance = new MouseEventModeler();

				}
			}
		}

		return instance;
	}

	public static final Logger logger = LogManager.getLogger(MouseEventModeler.class.getName());

	private List<Model> models = new ArrayList<>();

	public Map<Integer, List<Model>> range_models = new HashMap<>();

	/**
	 * 初始化
	 */
	public MouseEventModeler() {

		for(List<Action> actions : loadData()) {

			Model model = new Model(actions);

			models.add(model);

			if(model.x_sum == 47) {

			}

			for(int i=model.x_sum_lb; i<=model.x_sum_ub; i++) {

				if(!range_models.containsKey(i)) {
					range_models.put(i, new ArrayList<Model>());
				}
				range_models.get(i).add(model);
			}
		}
	}

	/**
	 * 返回指定px的 action 列表
	 * @param offset
	 * @return
	 * @throws Exception
	 */
	public List<Action> getActions(int x_init, int y_init, int offset) throws Exception {

		List<Action> actions = null;

		if(range_models.get(offset) != null) {

			int seed = new Random().nextInt(range_models.get(offset).size());
			Model model = range_models.get(offset).get(seed);

			int dx = offset - model.x_sum;
			logger.info("Select model --> x_sum:{}, y_sum:{}, dx:{}", model.x_sum, model.y_sum, dx);

			// TODO 复制这个Model 而不应该修改原始Model
			Model new_model = new Model(model.actions);
			new_model.morph(dx);
			new_model.setInitPosition(x_init, y_init);

			logger.info("Final model --> x_sum:{}, y_sum:{}.", new_model.x_sum, new_model.y_sum);

			return new_model.buildActions();

		} else {
			throw new NoSuitableModelException();
		}
	}

	class NoSuitableModelException extends Exception {}

	/**
	 * Step 刻画两个Actions之间的过程
	 */
	public static class Step implements JSONable<Step> {

		// 时间差
		int dt = 0;

		// x方向速度
		transient Fraction v_x = Fraction.ONE;
		// y方向速度
		transient Fraction v_y = Fraction.ONE;

		int dx;
		int dy;

		// 是否为平滑阶段
		boolean flat_phase = false;

		boolean flat_phase_edge = false;

		/**
		 * 通过 Actions 构建 Step
		 * @param from 起始事件
		 * @param to 终止事件
		 * @throws Exception 起始时间和终止时间的时间差为0时 抛出异常
		 */
		Step (Action from, Action to) throws Exception {

			dt = (int) (to.time - from.time);
			if(dt == 0) {
				throw new Exception("Two actions with same timestamp.");
			}
			dx = to.x - from.x;
			dy = to.y - from.y;
			v_x = Fraction.getFraction(dx, dt);
			v_y = Fraction.getFraction(dy, dt);
		}

		/**
		 * 通过具体参数 构建 Step
		 * @param dx x位移
		 * @param dy y位移
		 * @param dt 时间
		 * @param flat_phase 是否为平滑阶段
		 */
		Step (int dx, int dy, int dt, boolean flat_phase) {
			this.dx = dx;
			this.dy = dy;
			this.dt = dt;
			v_x = Fraction.getFraction(dx, dt);
			v_y = Fraction.getFraction(dy, dt);
			this.flat_phase = flat_phase;
		}

		/**
		 * Add 1px
		 */
		void addOnePixel() {
			// 限平滑阶段
			if(flat_phase) {

				dx = dx + 1;

				v_x = Fraction.getFraction(dx, dt);
				v_y = Fraction.getFraction(dy, dt);
			}
		}

		/**
		 * Subtract 1px
		 */
		void subtractOnePixel() throws StepCannotSubtractException {
			// 限平滑阶段 && X方向速度 > 0
			if(flat_phase && dx > 0) {

				dx = dx - 1;
				dy = dx != 0 ? (int) Math.round(dy*(dx-1)/dx) : 0;

				v_x = Fraction.getFraction(dx, dt);
				v_y = Fraction.getFraction(dy, dt);
			}
			else if(flat_phase && dx < 0) {
				dx = dx - 1;
				dy = dx != 0 ? (int) Math.round(dy*(dx-1)/dx) : 0;

				v_x = Fraction.getFraction(dx, dt);
				v_y = Fraction.getFraction(dy, dt);
			}
			else {
				logger.warn(this.toJSON());
				throw new StepCannotSubtractException();
			}
		}

		/**
		 * 随机变异
		 *
		 * 非平滑过程 Step 时间间隔，速度联动变化 （特征变化）
		 * 保证Step期间总位移不变，但轨迹特征在可控范围内随机变化
		 * dt' * v' = dt * v
		 */
		void mutation() {

			// 限非平滑阶段，阶段时间为10ms以上
			if(!flat_phase && dt > 10) {

				Random generator = new Random();
				double f = generator.nextDouble() * 0.4 - 0.2;

				dt = (int) (dt * (1 + f));
				if(dt < 7) dt = 7;
				v_x = Fraction.getFraction(dx, dt);
				v_y = Fraction.getFraction(dy, dt);
			}
		}

		public enum MutationType {
			Slower, Faster
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}

		public static class StepCannotSubtractException extends Exception {}
	}

	/**
	 * 用于保存一个轨迹模型
	 *
	 *
	 * 轨迹模型的基本变化思路
	 * 1. 平滑拖拽过程的速度变化
	 * 2. 非平滑拖拽过程的采样点间隔变化
	 *
	 * 首先找到平滑阶段的Actions
	 *
	 * 三种变化过程
	 *
	 * A. 平滑过程增减Step （总位移变化，但变化有一定限制）
	 *   由于平滑过程的时间间隔是8ms 增加点速度可以是 1/8 px/ms 的整倍数
	 *   增加一个step，相当于增加了一个Action，整体轨迹被拉长或压缩
	 *   新增的Step 其速度应该与相邻的 Step 差别不大（正负50%）
	 *
	 *   TODO 找到一个平滑过程的像素位移分布
	 *
	 * B. 平滑过程Step 增减速度 （总位移变化）
	 *   增减速度为 1/8 px/ms 的整倍数，速度不能小于0，速度增量不能大于50px/ms，不能大于当前step速度的50%
	 *   这个变化过程相当于对整体轨迹进行局部拉伸或压缩
	 *
	 * C. 非平滑过程 Step 时间间隔，速度联动变化 （总轨迹不变，时间变化，特征变化）
	 *    保证Step期间总位移不变，但轨迹特征在可控范围内随机变化
	 *    dt' * v' = dt * v
	 *
	 * 对于一个轨迹，可适应的总位移变化是有限的，暂定 正负10px
	 * 针对一个Model 和 dx 变化
	 *
	 */
	public static class Model {

		int x_init, y_init = 0;
		long t0 = 0;

		boolean init = false;

		List<Action> actions = new ArrayList<>();

		// 事件间间隔
		List<Step> steps = new ArrayList<>();

		// 平滑阶段的索引，只包含非边缘点
		List<Step> flat_steps = new ArrayList<>();
		// 非平滑阶段索引
		List<Step> non_flat_steps = new ArrayList<>();

		// 总时间，总x方向长度，总y方向长度
		long t_sum = 0;
		public int x_sum, y_sum = 0;

		// 可支持的 x 拓展范围
		public int x_sum_ub, x_sum_lb = 0;

		// 基于位移的平滑阶段索引，用于根据位移找到对应的平滑阶段
		TreeMap<Integer, List<Step>> dx_to_flat_steps = new TreeMap<>();

		/**
		 * 初始化
		 * @param actions
		 */
		public Model(List<Action> actions) {

			logger.trace("Init model...");

			this.actions = actions;

			// A. 根据actions 生成 steps
			for(int i=1; i<actions.size(); i++) {

				if(i == 1) {
					x_init = actions.get(i-1).x;
					y_init = actions.get(i-1).y;
					t0 = actions.get(i-1).time;
				}

				try {
					Step step = new Step(actions.get(i-1), actions.get(i));
					steps.add(step);
					t_sum += step.dt;
					x_sum += step.dx;
					y_sum += step.dy;
				} catch (Exception e) {
					logger.warn("Two actions with same timestamp.");
				}
			}

			// 计算 x_sum_ub x_sum_lb
			x_sum_ub = x_sum + 10;
			x_sum_lb = x_sum > 20? x_sum - 10: x_sum;

			// B. 遍历 steps 找到 平滑拖拽阶段
			for(int i=0; i < steps.size() - 2; i++) {
				if(steps.get(i).dt <= 8
						&& steps.get(i + 1).dt <= 8
						&& steps.get(i + 2).dt <= 8) {
					steps.get(i).flat_phase = true;
					steps.get(i + 1).flat_phase = true;
					steps.get(i + 2).flat_phase = true;
				}
			}

			// C. 初始化 平滑step 和 非平滑step 相关索引
			for(int i=0; i < steps.size(); i++) {

				if(steps.get(i).flat_phase) {

					// C1. 判断该阶段是否是平滑阶段的边缘
					if(i > 0 && !steps.get(i).flat_phase) {
						steps.get(i).flat_phase_edge = true;
					} else if(i < steps.size() - 1 && !steps.get(i+1).flat_phase) {
						steps.get(i).flat_phase_edge = true;
					} else {
						// 只包含非边缘点
						flat_steps.add(steps.get(i));

						// C2. 创建平滑阶段的时速度索引
						addStepIntoDxMap(steps.get(i));
					}

				} else {
					non_flat_steps.add(steps.get(i));
				}
			}

			init = true;
		}

		/**
		 * 设定初始位置
		 * @param x_init
		 * @param y_init
		 */
		public void setInitPosition(int x_init, int y_init) {
			this.x_init = x_init;
			this.y_init = y_init;
		}

		/**
		 * 将一个新的step 插入到 位移step 索引
		 * @param step 新生成 step
		 */
		private void addStepIntoDxMap(Step step) {

			int dx = step.dx;
			if (!dx_to_flat_steps.containsKey(dx)) {
				dx_to_flat_steps.put(dx, new ArrayList<>());
			}
			dx_to_flat_steps.get(dx).add(step);
		}

		/**
		 * 变形
		 *
		 * 需要根据具体的变形像素数决定策略
		 *
		 * @param dx 需要变形的像素数
		 */
		public void morph(int dx) throws Exception {

			logger.info("Morph: {}px", dx);

			// 用于储存随机挑选的索引
			Set<Integer> excluded_pool = new HashSet<>();

			// A 拉伸情况
			if (dx > 0 && dx <= x_sum_ub - x_sum) {

				logger.info("\tStretching...");

				int px_stretch = dx;

				while (px_stretch > 0) {

					int seed = new Random().nextInt(2);

					// 在已有的平滑阶段中增加一个step
					if(seed == 0) {

						// 随机生成这个step所位移的像素
						// 插入的新 step 其dx 不应该过大，不能大于 flatPhaseMaxPx + 1
						int new_seed = Math.min(new Random().nextInt(getFlatPhaseMaxPx()) + 1, px_stretch);

						logger.info("\tInsert new step with {}px", new_seed);

						addOneStepToFlatPhase(new_seed);

						px_stretch -= new_seed;

					// 随机找到一个平滑阶段 step，位移增加 1px
					}
					else {

						Step step = getRandomFlatStep(false, excluded_pool);
						excluded_pool.add(flat_steps.indexOf(step));

						// 更改统计信息 Part 1
						y_sum -= step.dy;

						step.addOnePixel();

						logger.info("\tAdd 1px: {}", step.toJSON());

						// 更改统计信息 Part 2
						x_sum += 1;
						y_sum += step.dy;

						x_sum_ub = x_sum + 10;
						x_sum_lb = x_sum > 20? x_sum - 10: x_sum;

						px_stretch --;
					}

				}

			}
			// B 压缩情况
			else if (dx < 0 && dx >= x_sum_lb - x_sum) {

				for(int i=0; i<-dx; i++) {

					Step step = getRandomFlatStep(true, excluded_pool);
					excluded_pool.add(flat_steps.indexOf(step));

					// 更改统计信息 Part 1
					y_sum -= step.dy;

					logger.warn("\tbefore: {}", step.toJSON());
					step.subtractOnePixel();
					logger.warn("\tSubtract 1px: {}, {}", step.toJSON());

					// 更改统计信息 Part 2
					x_sum -= 1;
					y_sum += step.dy;

					x_sum_ub = x_sum + 10;
					x_sum_lb = x_sum > 20? x_sum - 10: x_sum;

				}

			}
			// C Do nothing.
			else if (dx == 0) {
				logger.info("\tX not change.");
			}
			else {
				logger.warn("x_sum_ub:{}, x_sum_lb:{}", x_sum_ub, x_sum_lb);
				throw new MorphException();
			}

			// 不改变总x位移进行轨迹变换
			//mutation(); // 不需要修复 dx_to_flat_steps 不需要修复 统计信息
		}

		/**
		 * 随机找到一个平滑Step
		 * @param hasVelocity 该阶段是否有速度
		 *                    没有速度的平滑阶段无法减少速度，不能用于轨迹压缩
		 * @return 随机找到的平滑阶段
		 */
		Step getRandomFlatStep(boolean hasVelocity, Set<Integer> excluded_pool) throws NoFlatPhaseStepException {

			Step step = null;
			int search_count = 0;
			while (step == null && search_count < 5) {

				List<Integer> random_poll = new ArrayList<>();

				for (int i=0; i<flat_steps.size(); i++) {
					if(!excluded_pool.contains(i))
						random_poll.add(i);
				}

				int rnd = new Random().nextInt(random_poll.size());

				step = flat_steps.get(random_poll.get(rnd));

				if(step.dx == 0 && hasVelocity) step = null;
				search_count ++;
			}

			if(step == null) throw new NoFlatPhaseStepException();

			return step;
		}

		/**
		 * 在平滑阶段插入一个 Step
		 * 1. 先随机找到一个平滑Step(i)
		 * 2. 获取Step(i) 和 Step(i+1) 的速度，此时 Step(i+1) 也是一个平滑 Step（由于索引的设定）
		 * 3. 在Step(i) Step(i+1) 中间插入一个Step*，Step*的速度为：
		 *     1/8 px/ms 的整倍数，根据实际增加像素数决定
		 *
		 * 新增加的Step* 其速度应该与相邻Step 存在一定关系
		 */
		void addOneStepToFlatPhase(int px) throws Exception {

			logger.info("Add one step into flat phase, {}px.", px);

			if(flat_steps.size() == 0) {
				throw new NoFlatPhaseStepException();
			}

			// 插入位置Step Step* 插到 Step 之后位置
			Step step;

			// 根据px 找到合适的插入位置
			if(px >= getFlatPhaseMaxPx()) {

				logger.info("{}px > flat phase max px.", px);

				/*for(Step i : dx_to_flat_steps.lastEntry().getValue()) {
					logger.info(steps.indexOf(i));
				}*/

				// 找到最大位移的 Step
				List<Step> max_v_steps = dx_to_flat_steps.lastEntry().getValue();
				int seed = new Random().nextInt(max_v_steps.size());
				step = dx_to_flat_steps.lastEntry().getValue().get(seed);

			}
			else {

				List<Step> steps_tmp = new ArrayList<>();

				// 具有相同px位移的 step
				Step step_0 = getStepByPx(px);
				if(step_0 != null) {
					steps_tmp.add(step_0);
					logger.info("\tSame px, step: {}, {}", steps.indexOf(step_0), step_0.toJSON());
				}

				// 逐渐增加px 搜索具有该px 的 step
				Step step_1 = getCloseByStepByPx(px, true);
				if(step_1 != null) {
					steps_tmp.add(step_1);
					logger.info("\tLarger px, step: {}, {}", steps.indexOf(step_1), step_1.toJSON());
				}

				// 逐渐减少px 搜索具有该px 的 step
				Step step_2 = getCloseByStepByPx(px, false);
				if(step_2 != null) {
					steps_tmp.add(step_2);
					logger.info("\tSmaller px, step: {}, {}", steps.indexOf(step_2), step_2.toJSON());
				}

				// 随机找一个
				int seed = new Random().nextInt(steps_tmp.size());
				step = steps_tmp.get(seed);
			}

			if (step == null) throw new NoSuitableOffsetStepException();

			logger.info("\tChosen step: {}, {}", steps.indexOf(step), step.toJSON());

			// 构建新的step
			int dt = 8;
			int dy = step.dy;

			Step new_step = new Step(px, dy, dt, true);
			logger.info("\tNew step: {}", new_step.toJSON());

			int i = flat_steps.indexOf(step);

			// 插入new_step，维护相关索引
			flat_steps.add(i + 1, new_step);

			int i_ = steps.indexOf(step);
			steps.add(i_ + 1, new_step);

			addStepIntoDxMap(step);

			// 更改统计信息
			x_sum += new_step.dx;
			y_sum += new_step.dy;
			t_sum += new_step.dt;

			x_sum_ub = x_sum + 10;
			x_sum_lb = x_sum > 20? x_sum - 10: x_sum;
		}

		/**
		 * 随机选取 1/3 的 non_flat_phase的step
		 * 进行 mutation
		 */
		void mutation() {

			logger.info("Random mutation...");

			int[] mutation_indices = new Random()
					.ints(0, non_flat_steps.size())
					.distinct().limit((int)Math.ceil(non_flat_steps.size() * 0.33)).toArray();

			for(int i : mutation_indices) {

				// 更改统计信息
				t_sum -= non_flat_steps.get(i).dt;

				logger.trace("\tBefore: {}", non_flat_steps.get(i).toJSON());

				non_flat_steps.get(i).mutation();

				logger.trace("\tAfter: {}", non_flat_steps.get(i).toJSON());

				// 更改统计信息
				t_sum += non_flat_steps.get(i).dt;
			}
		}

		/**
		 * @return 平滑阶段最大x像素位移
		 */
		private int getFlatPhaseMaxPx() {

			return dx_to_flat_steps.lastKey();
		}

		/**
		 * @return 平滑阶段最小x像素位移
		 */
		private int getFlatPhaseMinPx() {

			return dx_to_flat_steps.firstKey();
		}

		/**
		 * 根据px参数，在现有的flat_steps中找到一个位移差不多的step
		 * @param px 初始位移
		 * @param searchUp 逐步增加px搜索
		 * @return
		 */
		Step getCloseByStepByPx(int px, boolean searchUp) {

			int px_ = px;

			Step step = null;

			int offset = 1;

			while(px_ <= getFlatPhaseMaxPx() && px_ >=0 && step == null) {

				px_ = searchUp? px + (offset ++) : px - (offset ++);

				List<Step> v_steps = dx_to_flat_steps.get(px_);

				if(v_steps != null && v_steps.size() > 0) {

					int i = new Random().nextInt(v_steps.size());
					step = v_steps.get(i);
				}
			}

			return step;
		}

		/**
		 * 根据指定位移，随机找一个Step
		 * @param px 位移
		 * @return
		 */
		Step getStepByPx(int px) {

			Step step = null;

			List<Step> v_steps = dx_to_flat_steps.get(px);

			if(v_steps != null && v_steps.size() > 0) {

				int i = new Random().nextInt(v_steps.size());
				step = v_steps.get(i);
			}

			return step;
		}

		/**
		 * 通过steps重新构建actions
		 * @return actions 列表
		 */
		public List<Action> buildActions() throws ModelNoInitException {

			logger.info("Build actions...");

			if(!init) throw new ModelNoInitException();

			List<Action> actions = new ArrayList<>();

			// 构建第一个Action
			Action a0 = new Action(Action.Type.Press, x_init, y_init, t0);
			actions.add(a0);

			int c_x = a0.x;
			int c_y = a0.y;
			long c_t = a0.time;

			// 遍历Step 生成Action
			for(Step step : steps) {

				c_x += step.dx;
				c_y += step.dy;
				c_t += step.dt;

				Action a = new Action(Action.Type.Drag, c_x, c_y, c_t);
				actions.add(a);
			}

			actions.get(actions.size()-1).type = Action.Type.Release;

			return actions;
		}

		class NoFlatPhaseStepException extends Exception {}

		class MorphException extends Exception {}

		class NoSuitableOffsetStepException extends Exception {}

		class ModelNoInitException extends Exception {}

	}

	/**
	 * 对鼠标左键按下之前的事件进行清理
	 * @param actions 清理后的actions
	 */
	public static void removePreMoveActions(List<Action> actions) {

		List<Action> actions_ = new ArrayList<>();

		for(Action action : actions) {

			if(! action.type.equals(Action.Type.Move)) {
				actions_.add(action);
			}
		}

		actions = actions_;
}

	/**
	 * 加载数据
	 * @return
	 */
	public static List<List<Action>> loadData() {

		List<List<Action>> actions = new ArrayList<>();

		Type type = new TypeToken<ArrayList<Action>>(){}.getType();

		File folder = new File(MouseEventTracker.serPath);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {

				logger.trace("File: " + listOfFiles[i].getPath());

				List<Action> as = JSON.fromJson(
						FileUtil.readFileByLines(listOfFiles[i].getPath()),
						type
				);

				actions.add(as);

			} else if (listOfFiles[i].isDirectory()) {
				logger.info("Directory: " + listOfFiles[i].getName());
			}
		}

		return actions;
	}

	/**
	 * 读取单个Action 序列
	 * @param path
	 * @return
	 */
	public static List<Action> loadData(String path) {

		List<Action> actions = new ArrayList<>();

		Type type = new TypeToken<ArrayList<Action>>(){}.getType();

		actions = JSON.fromJson(
				FileUtil.readFileByLines(path),
				type
		);

		return actions;
	}

	/**
	 * 生成 Action 列表的 Mathematica List
	 * @param actions
	 * @return
	 */
	public static String toMathematicaListStr(List<Action> actions) {
		String output = "{";
		for(Action action : actions) {
			output += "{" + action.time + ", " + action.x + ", " + action.y + "}, ";
		}
		output = output.substring(0, output.length() - 2);
		output += "}";
		return output;
	}
}
