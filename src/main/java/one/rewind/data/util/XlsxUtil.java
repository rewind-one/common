package one.rewind.data.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class XlsxUtil {

	public static final Logger logger = LogManager.getLogger(XlsxUtil.class.getName());

	/**
	 * 将名称读取到set中
	 * @param fileName
	 * @param index
	 * @return
	 */
	public static Set<String> getSet(String fileName, int index) {

		Set<String> set = new HashSet<>();

		try {

			FileInputStream excelFile = new FileInputStream(new File(fileName));
			Workbook workbook = new XSSFWorkbook(excelFile);

			Sheet datatypeSheet = workbook.getSheetAt(index);

			for (Row currentRow : datatypeSheet) {

				// 非空行
				if(currentRow.getCell(0) != null) {

					String value = currentRow.getCell(0).toString();

					// "c" 是默认的第一列名称
					if (!value.equals("c") && value.trim().length() > 0) {

						set.add(value);
					}

				}
				else {

					break;
				}
			}

		} catch (IOException e) {
			logger.error(e);
		}

		return set;
	}


	/**
	 * 将名称-标签读取到Map中
	 * @param fileName
	 * @param index
	 * @return
	 */
	public static Map<String, Set<String>> getMap(String fileName, int index) {

		Map<String, Set<String>> map = new HashMap<>();

		try {

			/**
			 * 读取专有名词
			 */
			FileInputStream excelFile = new FileInputStream(new File(fileName));
			Workbook workbook = new XSSFWorkbook(excelFile);

			Sheet datatypeSheet = workbook.getSheetAt(index);
			Iterator<Row> iterator = datatypeSheet.iterator();

			while (iterator.hasNext()) {

				Row currentRow = iterator.next();

				// 非空行
				if(currentRow.getCell(0) != null) {

					String value = currentRow.getCell(0).toString();

					// "c" 是默认的第一列名称
					if (!value.equals("c") && value.trim().length() > 0) {

						String tag = currentRow.getCell(1).toString();

						map.computeIfAbsent(value, k -> new HashSet<>());

						if(!map.get(value).contains(tag)) {
							map.get(value).add(tag);
						}
					}
				} else {
					break;
				}
			}

		} catch (IOException e) {
			logger.error(e);
		}

		return map;
	}

	/**
	 * 将名称-值读到Map中
	 * @param fileName
	 * @param index
	 * @return
	 */
	public static Map<String, Float> getMapNumeric(String fileName, int index) {

		Map<String, Float>  map = new HashMap();

		try {

			FileInputStream excelFile = new FileInputStream(new File(fileName));
			Workbook workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(index);

			for (Row row : datatypeSheet) {

				// 非空行
				if(row.getCell(0) != null) {

					String value = row.getCell(0).toString();
					// "c" 是默认的第一列名称
					if (!value.equals("c") && value.trim().length() > 0) {
						float number = (float) row.getCell(1).getNumericCellValue();
						map.put(value, number);
					}

				} else {
					break;
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}

		return map;
	}
}
