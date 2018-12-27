package one.rewind.db.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import one.rewind.db.FastDFSAdapter;
import one.rewind.txt.StringUtil;
import org.apache.commons.io.FilenameUtils;

import java.util.LinkedHashMap;

/**
 * 文件记录信息类
 *
 * 1. id生成机制
 *   外部给定 或 随机
 * 2. 先存FastDFS 还是 先入库？
 *
 *
 * @author scisaga@gmail.com
 * @date 2018/12/26
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "sources")
public abstract class Source extends Model {

	@DatabaseField(dataType = DataType.STRING, width = 32, id = true)
	public String id;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String url;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String file_name;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String content_type;

	public transient byte[] src;

	@DatabaseField(dataType = DataType.LONG)
	public long size;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String group_name;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String file_path;

	public Source() {}

	/**
	 * 外部指定id
	 * @param id
	 * @param url
	 * @param file_name
	 * @param content_type
	 * @param src
	 */
	public Source(String id, String url, String file_name, String content_type, byte[] src) {

		this.id = id;
		this.url = url;
		this.file_name = file_name;
		this.content_type = content_type;
		this.src = src;
		this.size = src.length;
	}

	/**
	 *
	 * @param file_name
	 * @param content_type
	 * @param src
	 */
	public Source(String file_name, String content_type, byte[] src) {

		this.id = StringUtil.MD5(file_name + "::"+ content_type + "::" + System.currentTimeMillis());
		this.file_name = file_name;
		this.content_type = content_type;
		this.src = src;
		this.size = src.length;
	}

	/**
	 * 插入
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {

		Dao dao = DaoManager.getDao(this.getClass());

		// 如果有对应记录 不在保存
		if (dao.queryForId(id) != null) {

			throw new Exception("Source:" + id + " exist.");
		}

		// 生成meta信息
		LinkedHashMap<String, String> meta = new LinkedHashMap<>();
		meta.put("id", id);
		meta.put("url", url);
		meta.put("file_name", file_name);
		meta.put("content_type", content_type);
		meta.put("size", String.valueOf(size));

		String[] info = FastDFSAdapter.getInstance().put(src, FilenameUtils.getExtension(file_name), meta);

		this.group_name = info[0];
		this.file_path = info[1];

		return super.insert();
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception {
		throw new Exception("Source do not support update.");
	}

}
