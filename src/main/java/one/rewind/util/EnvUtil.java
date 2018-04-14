package one.rewind.util;

/**
 * 主机OS检查工具类
 * @author scisaga@gmail.com
 * @date 2017.11.12
 */
public class EnvUtil {

	private static String os;
	
	public static boolean isHostLinux() {
		if(os == null) {
			os = System.getProperty("os.name");
		}
		if (os != null && os.toLowerCase().startsWith("linux")) {
			return true;
		}
		return false;
	}
}
