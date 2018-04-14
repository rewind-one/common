package one.rewind.captcha;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class BypassCaptchaApi {

	private BypassCaptchaApi() {
	}

	private static String url_encode(String str) {
		try {
			return java.net.URLEncoder.encode(str, "UTF-8");
		} catch (Exception ex) {
			return "";
		}
	}

	private static String base64_encode(byte[] bs) {
		return base64_encode(bs, 0, bs.length);
	}

	private static byte[] encodeData;
	private static String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	static {
		encodeData = new byte[64];
		for (int i = 0; i < 64; i++) {
			byte c = (byte) charSet.charAt(i);
			encodeData[i] = c;
		}
	}

	private static String base64_encode(byte[] src, int start, int length) {
		byte[] dst = new byte[(length + 2) / 3 * 4 + length / 72];
		int x = 0;
		int dstIndex = 0;
		int state = 0;  // which char in pattern
		int old = 0;  // previous byte
		int len = 0;  // length decoded so far
		int max = length + start;
		for (int srcIndex = start; srcIndex < max; srcIndex++) {
			x = src[srcIndex];
			switch (++state) {
				case 1:
					dst[dstIndex++] = encodeData[x >> 2 & 0x3f];
					break;
				case 2:
					dst[dstIndex++] = encodeData[old << 4 & 0x30 | x >> 4 & 0xf];
					break;
				case 3:
					dst[dstIndex++] = encodeData[old << 2 & 0x3C | x >> 6 & 0x3];
					dst[dstIndex++] = encodeData[x & 0x3F];
					state = 0;
					break;
			}
			old = x;
			if (++len >= 72) {
				dst[dstIndex++] = (byte) '\n';
				len = 0;
			}
		}

		switch (state) {
			case 1:
				dst[dstIndex++] = encodeData[old << 4 & 0x30];
				dst[dstIndex++] = (byte) '=';
				dst[dstIndex++] = (byte) '=';
				break;
			case 2:
				dst[dstIndex++] = encodeData[old << 2 & 0x3c];
				dst[dstIndex++] = (byte) '=';
				break;
		}
		return new String(dst);
	}

	private static ApiResult Post(String urls, String[] datas) {
		try {
			String con = "";
			for (int i = 0; i < datas.length; i += 2) {
				if (i > 0) {
					con += "&";
				}
				con += url_encode(datas[i]) + "=" + url_encode(datas[i + 1]);
			}

			String response = "";
			URL url = new URL(urls);
			URLConnection conn = url.openConnection();
			// Set connection parameters.
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			// Make server believe we are form data...
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			// Write out the bytes of the content string to the stream.
			out.writeBytes(con);
			out.flush();
			out.close();
			// Read response from the input stream.
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String temp;
			while ((temp = in.readLine()) != null) {
				response += temp + "\n";
			}
			temp = null;
			in.close();

			return ApiResult.Extract(response);
		} catch (Exception ex) {
			ApiResult ret = new ApiResult();
			ret.Error = "Error: " + ex.getMessage();
			return ret;
		}
	}
	
	public static ApiResult Submit(String key, byte[] src) {

		try {
			String url = "http://bypasscaptcha.com/upload.php";
			String enc_file = base64_encode(src);
			String[] data = new String[]{"key", key, "file", enc_file,
					"submit", "Submit", "gen_task_id", "1", "base64_code", "1"};
			return Post(url, data);
		} catch (Exception ex) {
			ApiResult ret = new ApiResult();
			ret.Error = "Error: " + ex.getMessage();
			return ret;
		}
	}

	public static ApiResult Submit(String key, InputStream image_file_stream) {

		try {
			String url = "http://bypasscaptcha.com/upload.php";

			ByteArrayOutputStream outp = new ByteArrayOutputStream();
			int ch = 0;
			while ((ch = image_file_stream.read()) != -1) {
				outp.write(ch);
			}

			byte[] buf = outp.toByteArray();
			image_file_stream.read(buf, 0, buf.length);
			image_file_stream.close();
			String enc_file = base64_encode(buf);
			String[] data = new String[]{"key", key, "file", enc_file,
					"submit", "Submit", "gen_task_id", "1", "base64_code", "1"};
			return Post(url, data);
		} catch (Exception ex) {
			ApiResult ret = new ApiResult();
			ret.Error = "Error: " + ex.getMessage();
			return ret;
		}
	}

	public static ApiResult Submit(String key, String image_file) {
		try {
			String url = "http://bypasscaptcha.com/upload.php";
			File file = new File(image_file);
			InputStream input = new FileInputStream(file);
			byte[] buf = new byte[(int) file.length()];
			input.read(buf, 0, buf.length);
			input.close();
			String enc_file = base64_encode(buf);
			String[] data = new String[]{"key", key, "file", enc_file,
					"submit", "Submit", "gen_task_id", "1", "base64_code", "1"};
			return Post(url, data);
		} catch (Exception ex) {
			ApiResult ret = new ApiResult();
			ret.Error = "Error: " + ex.getMessage();
			return ret;
		}
	}

	public static ApiResult SendFeedBack(String key, ApiResult r, boolean is_correct) {
		String url = "http://bypasscaptcha.com/check_value.php";
		return Post(url, new String[]{"key", key, "task_id", r.TaskId, "cv",
				is_correct ? "1" : "0", "submit", "Submit"});
	}

	public static ApiResult GetLeft(String key) {
		String url = "http://bypasscaptcha.com/ex_left.php";
		return Post(url, new String[]{"key", key});
	}
}
