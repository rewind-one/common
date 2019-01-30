package one.rewind.io.requester.parser.test;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ESIndex;
import one.rewind.db.persister.JSONableListPersister;

import java.util.List;

@DBName(value = "raw")
@DatabaseTable(tableName = "media")
public class MediaModelTest extends ESIndex {

	public static String index = "media";

	public static String type = "media";

	public static String mapping_file = "index_mappings/media.json";

	@DatabaseField(dataType = DataType.INTEGER, width = 11, index = true)
	public int platform_id; // 平台id

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String platform; // 平台简称

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String src_id; // 源id

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String avatar; // 头像

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String nick;

	@DatabaseField(persisterClass = JSONableListPersister.class, width = 1024)
	public List<String> tags; // 平台标签（如知乎：“物理学、量子物理 话题的优秀回答者”）

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String content; // 介绍

	@DatabaseField(dataType = DataType.INTEGER, width = 11)
	public int fav_count; // 关注数（自己关注其他）

	@DatabaseField(dataType = DataType.INTEGER, width = 11)
	public int fans_count; // 粉丝量

	@DatabaseField(dataType = DataType.INTEGER, width = 11)
	public int essay_count; // 文章量

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String subject; // 主体

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String trademark; // 商标

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String phone; // 电话

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String email; // 邮箱

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String weibo; // 微博账号

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String qq; // qq号

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String wechat; // 微信号

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String wechat_media; // 微信公众号

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String origin_url; // 原始URL

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String source_id; // 源文件ID

	public MediaModelTest() {}

	@Override
	public String getIndex() {
		return index;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getMappingFile() {
		return mapping_file;
	}
}
