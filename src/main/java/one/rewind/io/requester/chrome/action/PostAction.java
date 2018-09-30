package one.rewind.io.requester.chrome.action;

import one.rewind.io.requester.chrome.ChromeAgent;
import one.rewind.json.JSON;

import java.util.Map;

public class PostAction extends Action {

	static String script =
		"function post(path, params, method) {\n" +
		"    method = method || \"post\";\n" +
		"\n" +
		"    var form = document.createElement('form');\n" +
		"    form.setAttribute('method', method);\n" +
		"    form.setAttribute('action', path);\n" +
		"\n" +
		"    for(var key in params) {\n" +
		"        if(params.hasOwnProperty(key)) {\n" +
		"            var hiddenField = document.createElement(\"input\");\n" +
		"            hiddenField.setAttribute(\"type\", \"hidden\");\n" +
		"            hiddenField.setAttribute(\"name\", key);\n" +
		"            hiddenField.setAttribute(\"value\", params[key]);\n" +
		"\n" +
		"            form.appendChild(hiddenField);\n" +
		"        }\n" +
		"    }\n" +
		"\n" +
		"    document.body.appendChild(form);\n" +
		"    form.submit();\n" +
		"};\n" +
		"post('{{url}}', {{data}});";

	public String url;

	public Map<String, String> data;

	public long sleepTime = 0;

	public PostAction() {}

	public PostAction(String url, Map<String, String> data) {
		this.url = url;
		this.data = data;
	}

	public PostAction(String elementPath, long sleepTime) {
		this.url = url;
		this.data = data;
		this.sleepTime = sleepTime;
	}

	public boolean run(ChromeAgent agent) {

		try {

			script = script
					.replaceAll("\\{\\{url\\}\\}", url)
					.replaceAll("\\{\\{data\\}\\}", JSON.toJson(data));

			agent.getDriver().executeScript(script);

			if (sleepTime > 0L) {
				Thread.sleep(sleepTime);
			}

			return true;

		} catch (Exception e) {
			logger.error("Exec post script error. ", e);
		}

		return false;
	}
}