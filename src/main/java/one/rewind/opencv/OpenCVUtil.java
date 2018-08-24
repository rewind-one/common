package one.rewind.opencv;

import org.eclipse.jetty.websocket.api.SuspendToken;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.*;

public class OpenCVUtil {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static class Coordinate {
		public int x;
		public int y;
	}

	/**
	 * 选择5个点
	 * 本地原图片库与截图比较，选择原图进行下一步操作
	 * TODO
	 * @param bg_list
	 * @param mat
	 * @return
	 */
	public static Mat mostSimilar(List<Mat> bg_list, Mat mat) {

		int[][] a = {{7,7},{245,15},{31,145},{100,155},{253,153}};

		for (Mat bg_mat : bg_list) {
			boolean is = true;

			int sum = 0;

			for (int i = 0 ; i<5; i++) {

				double[] data_1 = mat.get(a[i][1], a[i][0]);
				double[] data_2 = bg_mat.get(a[i][1], a[i][0]);

				for (int index = 0; index<3; index++) {

					if (data_1[index] - data_2[index] < 10 && data_1[index] - data_2[index] > -10) {

					} else {
						is = false;
						break;
					}
				}
				if (!is) {
					sum++;
				}
			}

			if (sum < 3 ) {
				return bg_mat;
			}
		}
		return null;
	}

	/**
	 *  比较两张图片是否一致
	 * TODO
	 * @param sMat1
	 * @param sMat2
	 * @return
	 */
	public static boolean areEqual(Mat sMat1, Mat sMat2) {

		for (int h = 0; h < sMat1.height(); h++) {
			for (int w = 0; w < sMat1.width(); w++) {

				double[] data_1 = sMat1.get(h, w);
				double[] data_2 = sMat2.get(h, w);

				for (int i = 0; i < 3; i++) {
					if (data_1[i] != data_2[i]) {
						return false;
					}
				}

			}
		}

		return true;
	}

	/**
	 *
	 */
	public static int getOffset(Mat sMat1, Mat sMat2) {

		Mat mat = imageContrast(sMat1, sMat2);
		List<Coordinate> list_b = getList(mat);

		Mat rMat = ChangeBlock(list_b, mat);

		List<Coordinate> list_all = getList(rMat);

		return getMoveNum(list_all);
	}


	/**
	 * 最终获取位移
	 * @param geetest1_path
	 * @param geetest2_path
	 * @return
	 */
	public static int getOffset(String geetest1_path, String geetest2_path) {

		Mat sMat1 = Imgcodecs.imread(geetest1_path);
		Mat sMat2 = Imgcodecs.imread(geetest2_path);

		Mat mat = imageContrast(sMat1, sMat2);
		List<Coordinate> list_b = getList(mat);

		Mat rMat = ChangeBlock(list_b, mat);

		List<Coordinate> list_all = getList(rMat);

		return getMoveNum(list_all);
	}

	/**
	 * 获取所有黑色点的像素
	 * @param mat
	 * @return
	 */
	public static List<Coordinate> getList(Mat mat) {

		List<Coordinate> list_b = new ArrayList<>();

		for (int h = 0; h < mat.height(); h++) {
			for (int w = 0; w < mat.width(); w++) {

				double[] data = mat.get(h, w);

				if(data[0] == 0) {
					Coordinate coordinate = new Coordinate();
					coordinate.y = h; // hight
					coordinate.x = w; // width
					list_b.add(coordinate);

				}
			}
		}
		return list_b;
	}

	/**
	 * 图片对比，改色
	 * @param mat_a
	 * @param mat_b
	 */
	public static Mat imageContrast(Mat mat_a, Mat mat_b) {

		// 图片对比，
		for (int h = 0; h < mat_b.height(); h++) {
			for (int w = 0; w < mat_b.width(); w++){

				double[] data = mat_b.get(h, w);

				if (colorDifference(mat_a.get(h,w), mat_b.get(h,w), data.length, 50)) {

					for (int i = 0 ; i < data.length; i++) {
						data[i] = 255;
					}
					mat_b.put(h, w, data);
				}
				else {
					for (int i = 0 ; i < data.length; i++) {
						data[i] = 0;
					}
					mat_b.put(h, w, data);
				}
			}

			// 删除下底
			/*double[] color = new double[3];
			if (h > 200) {
				for (int i = 0 ; i < color.length; i++) {
					color[i] = 255;
				}
				for (int y = 0; y < mat_a.width(); y++ )
					mat_b.put(h, y, color);
			}*/
		}

		return mat_b;
	}

	/**
	 * 将图片中被黑色包围的白色变黑
	 * @param list_block
	 * @param mat
	 * @return
	 */
	public static Mat ChangeBlock(List<Coordinate> list_block, Mat mat) {

		// 宽
		Set<Integer> width = new HashSet<>();

		for (Coordinate coordinate_b : list_block) {
			width.add(coordinate_b.x);
		}

		// 宽相同， 找最大，最小的高
		for (int i : width) {
			int max = 0;
			int min = 100000;
			for (Coordinate coordinate : list_block) {

				if (coordinate.x == i) {
					// 找最大的height
					if (max < coordinate.y) {
						max = coordinate.y;
					}
					// 找最小的
					if (min > coordinate.y) {
						min = coordinate.y;
					}
				}
			}

			// 填充黑色
			for (int s = min; s <= max; s++) {
				double[] data = mat.get(s,i);
				for (int ss = 0; ss < data.length; ss++) {
					data[ss] = 0;
				}
				mat.put(s, i, data);
			}
		}

		return mat;
	}

	/**
	 * 两张图片对比色差
	 * @param image_a
	 * @param image_b
	 * @param index 图片像素通道
	 * @param range 色差范围
	 * @return 是否符合要求
	 */
	public static boolean colorDifference(double[] image_a, double[] image_b, int index, int range) {

		for (int r = 0; r < index; r++) {

			if (image_a[r] - image_b[r] > range) {
				return false;
			}
			if (image_b[r] - image_a[r] > range) {

				return false;
			}
		}
		return true;
	}

	/**
	 * 获取移动像素
	 * @param coordinates
	 * @return
	 */
	public static int getMoveNum(List<Coordinate> coordinates) {

		// 得到所有黑像素的hight
		List<Integer> xs = new ArrayList<>();
		for (Coordinate coordinate : coordinates) {

			if (!xs.contains(coordinate.x)) {
				xs.add(coordinate.x);
			}
		}

		Collections.sort(xs);

		int first_min = xs.get(0);
		int x_gap = 0;

		for(int i = first_min; i<=1000; i++) {
			if (!xs.contains(i)) {
				x_gap = i;
				break;
			}
		}

		List<Integer> xs_1 = new ArrayList<>();
		List<Integer> xs_2 = new ArrayList<>();
		for (Coordinate coordinate : coordinates) {

			if(coordinate.x < x_gap) {
				xs_1.add(coordinate.x);
			} else {
				xs_2.add(coordinate.x);
			}
		}

		if(xs_2.size() == 0){
			return -1;
		}

		return (int) (xs_2.stream().mapToInt(val->val).average().getAsDouble()
				- xs_1.stream().mapToInt(val->val).average().getAsDouble());

/*
		// 找到两个物体的y边界
		int first_min = width.get(0);
		int first_max = 0;

		int second_min = 0;
		int second_max = width.get(width.size() - 1);

		for (int i = 0; i < width.size(); i++) {
			if (width.get(i) + 1 != width.get(i + 1)) { // TODO 有可能出现数组越界
				first_max = width.get(i);
				second_min = width.get(i + 1);
				break;
			}
		}
*/

		/*// 获取两物体中心点的X轴数据
		int f_x = X_Num(first_min, first_max, coordinates);
		int s_x = X_Num(second_min, second_max, coordinates);

		return s_x - f_x;*/

	}

	/**
	 * 获取中心点的X轴数据
	 * @param min
	 * @param max
	 * @param list
	 * @return
	 */
	public static int X_Num(int min, int max, List<Coordinate> list) {

		int sum = 0;
		int num = 0;
		for (int i = min; i <= max; i++){
			for (Coordinate c : list) {
				if (c.y == i) {
					sum = sum + c.y;
					num++;
				}
			}
		}
		return sum/num;
	}

}


