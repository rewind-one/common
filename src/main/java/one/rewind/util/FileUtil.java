package one.rewind.util;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * 文件工具类
 * @author scisaga@gmail.com
 * @date 2015.3.7
 */
public class FileUtil {

	/**
	 * 逐行读取文件
	 * @param fileName
	 * @return
	 */
	public static String readFileByLines(String fileName) {
		String output = "";
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				output += tempString + "\n";
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return output;
	}
	
	/**
	 * 读取文件
	 * @param fileName
	 * @return
	 */
	public static byte[] readBytesFromFile(String fileName) {
		File file = new File(fileName);
		return readBytesFromFile(file);
	}

	/**
	 * 读取文件
	 * @param file
	 * @return
	 */
	public static byte[] readBytesFromFile(File file) {
		try {
			FileInputStream fin = new FileInputStream(file);
			ByteBuffer nbf = ByteBuffer.allocate((int) file.length());
			byte[] array = new byte[1024];
			int offset = 0, length = 0;
			while ((length = fin.read(array)) > 0) {
				if (length != 1024)
					nbf.put(array, 0, length);
				else
					nbf.put(array);
				offset += length;
			}
			fin.close();
			byte[] content = nbf.array();
			return content;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 将byte数组写入文件
	 * TODO: 当文件较大时候可能发生问题
	 * @param fileName
	 * @param fileBytes
	 * @return
	 */
	public static boolean writeBytesToFile(byte[] fileBytes, String fileName) {
		
		try {

			// 文件夹不存在创建文件夹
			String folder_path = fileName.replaceAll("/[^/]+?$", "");

			if(folder_path.length() > 0) {

				File directory = new File(folder_path);
				if (!directory.exists()) {
					directory.mkdir();
				}
			}

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
			bos.write(fileBytes);
			bos.flush();
			bos.close();
			return true;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean appendLineToFile(String line, String fileName) {

		// 文件夹不存在创建文件夹
		String folder_path = fileName.replaceAll("/[^/]+?$", "");

		if(folder_path.length() > 0) {

			File directory = new File(folder_path);
			if (!directory.exists()) {
				directory.mkdir();
			}
		}

		try {

			BufferedWriter output = new BufferedWriter(new FileWriter(fileName, true));
			output.write(line);
			output.newLine();
			output.close();
			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
}
