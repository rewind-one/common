package one.rewind.captcha;

/**
 * Capcha Bypass API接口
 * @author karajan
 *
 */
public class ApiResult {
	
	public boolean IsCallOk = false;
	public String DecodedValue = "";
	public String TaskId = "-1";
	public String Error = "";
	public String LeftCredits = "-1";

	public static ApiResult Extract(String data) {

		ApiResult ret = new ApiResult();

		data = data.trim().intern();
		if (data == "OK") // for feedback api
		{
			ret.IsCallOk = true;
			return ret;
		}

		String[] lines = data.split("\n");
		int count = lines.length;

		for (int i = 0; i < count; i++) {
			String line = lines[i];
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}

			int idx = line.indexOf(" ");
			if (idx < 0) {
				continue;
			}

			String name = line.substring(0, idx).trim().intern();
			String val = line.substring(idx + 1).trim();

			if (name == "TaskId") {
				ret.TaskId = val;
			} else if (name == "Value") {
				ret.DecodedValue = val;
			} else if (name == "Error") {
				ret.Error = val;
			} else if (name == "Left") {
				ret.LeftCredits = val;
			}
		}

		if (ret.TaskId != "-1" || ret.LeftCredits != "-1") {
			ret.IsCallOk = true;
		}
		return ret;
	}
}
