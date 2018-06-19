/**
 *
 */
package one.rewind.txt;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 字符串常用工具类
 * @author scisaga@gmail.com
 * @date 2016.7.20
 */
public class StringUtil {
	
	/**
	 * 对长出的字符串进行截断
	 */
	public static String strCrop(String in, int length) {
		if (in != null && in.length() > length) {
			return in.substring(0, length);
		} else
			return in;
	}
	
	/**
	 * 根据spliter第一次出现的位置
	 * 将输入字符串一分为二
	 * @param src
	 * @param spliter
	 * @return
	 */
	public static String[] splitFirstChar(String src, String spliter) {

		String[] res = new String[2];
		int eindex = src.indexOf(spliter);
		if (eindex < 0) {
			return null;
		}
		int slen = spliter.length();

		res[0] = src.substring(0, eindex);
		res[1] = src.substring(eindex + slen);
		return res;

	}
	
	/**
	 * 清理Ctrl-Char和反斜杠
	 *
	 * @param in
	 * @return
	 */
	public static String removeCtrlChars(String in) {
		String out = in;

		// 处理反斜杠
		out = out.replaceAll("\\\\", "\\\\\\\\");
		out = out.replaceAll("\\\\\\\"", "\\\\\\\\\"");
		out = out.replaceAll(" | ", "");

		out = out.replace("\n|\b|\f|\t|\r", "");

		// ascii 0-31 ctrl-char + ascii 127 del
		out = out.replaceAll("[\u0000-\u001f\u007f]", "");

		return out;
	}

	/**
	 * 去除HTML标记
	 *
	 * @param in
	 * @return
	 */
	public static String removeHTML(String in) {
		return in.replaceAll("<[^>]+?>", "");
	}

	/**
	 * 去除空格符
	 *
	 * @param in
	 * @return
	 */
	public static String removeBlank(String in) {
		return in.replaceAll("\\s*", "");
	}

	/**
	 * 去掉多余的空格和HTML标记
	 *
	 * @param in
	 * @return
	 */
	public static String purgeHTML(String in) {
		if (in == null) {
			return null;
		}

		return in.replaceAll("^[ |	|　]+", "").replaceAll("[ |	|　]+$", "")
				.replaceAll(">[ |	|　]+", ">").replaceAll("[ |	|　]+<", "<")
				.replaceAll("&nbsp;", "").replaceAll("style=\".+?\"", "");
	}

	/**
	 * 生成随机uuid
	 * @return
	 */
	public static String uuid() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	/**
	 * 生成16位UUID
	 *
	 * @return
	 */
	public static byte[] uuid(String src) {

		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");

			m.reset();
			m.update(src.getBytes());
			byte[] digest = m.digest();
			return digest;
		} catch (NoSuchAlgorithmException e) {

			UUID uid = UUID.randomUUID();
			return getIdAsByte(uid);
		}
	}

	/**
	 * 将UUID转换为bin(16)
	 *
	 * @param uuid
	 * @return
	 */
	public static byte[] getIdAsByte(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	/**
	 * 将bin(16)转换为UUID NOTE: 转换不是互逆的
	 *
	 * @param bytes
	 * @return
	 */
	public static UUID toUUID(byte[] bytes) {

		if (bytes.length != 16) {
			throw new IllegalArgumentException();
		}
		int i = 0;
		long msl = 0;
		for (; i < 8; i++) {
			msl = msl << 8 | bytes[i] & 0xFF;
		}
		long lsl = 0;
		for (; i < 16; i++) {
			lsl = lsl << 8 | bytes[i] & 0xFF;
		}
		return new UUID(msl, lsl);
	}

	/**
	 * 将byte[]转化为十进制字符串
	 *
	 * @param a
	 * @return
	 */
	public static String byteArrayToHex(byte[] a) {

		if (a == null) return "";

		StringBuilder sb = new StringBuilder(a.length * 2);

		for (byte b : a) {
			sb.append(String.format("%02x", b & 0xff));
		}

		return sb.toString();
	}

	/**
	 * 将十进制字符串转化为Byte[]
	 *
	 * @param s
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	/**
	 * 转换成UTF-8
	 * 
	 * @param str
	 * @return
	 */
	public static String toUTF8(String str) {
		String result = str;
		try {
			result = new String(str.getBytes(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * MD5 encode
	 * @param inStr
	 * @return
	 */
	public static String MD5(String inStr) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}
		char[] charArray = inStr.toCharArray();
		byte[] byteArray = new byte[charArray.length];

		for (int i = 0; i < charArray.length; i++)
			byteArray[i] = (byte) charArray[i];

		byte[] md5Bytes = md5.digest(byteArray);

		StringBuffer hexValue = new StringBuffer();

		for (int i = 0; i < md5Bytes.length; i++) {
			int val = ((int) md5Bytes[i]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}

		return hexValue.toString();
	}

	public static String cronPattern = "(\\*|((\\*\\/)?[1-5]?[0-9])) (\\*|((\\*\\/)?(1?[0-9]|2[0-3]))) (\\*|((\\*\\/)?([1-9]|[12][0-9]|3[0-1]))) (\\*|((\\*\\/)?([1-9]|1[0-2]))) (\\*|((\\*\\/)?[0-6]))";

	/**
	 * Validate cron pattern
	 * @param cron
	 * @return
	 */
	public static boolean validCron(String cron) {

		if(cron.matches(cronPattern)) {
			return true;
		}

		return false;
	}
}
