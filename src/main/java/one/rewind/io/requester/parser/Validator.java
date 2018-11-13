package one.rewind.io.requester.parser;

import one.rewind.io.requester.callback.TaskValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2018/11/14
 */
public class Validator {

	public List<String> contains = new ArrayList<>();

	public List<String> not_contain = new ArrayList<>();

	/**
	 *
	 * @param words
	 */
	public Validator(String... words) {
		for(String w : words) {
			contains.add(w);
		}
	}

	/**
	 *
	 * @param words
	 * @return
	 */
	public Validator addContain(String... words) {
		for(String w : words) {
			contains.add(w);
		}
		return this;
	}

	/**
	 *
	 * @param words
	 * @return
	 */
	public Validator addNotContain(String... words) {
		for(String w : words) {
			not_contain.add(w);
		}
		return this;
	}

	public TaskValidator toTaskValidator() {

		return (a, t) -> {

			String class_name = t.holder.class_name;
			int template_id = t.holder.template_id;
			String domain = t.holder.domain;
			String fingerprint = t.holder.fingerprint;

			String src = t.getResponse().getText();

			for(String w : contains) {
				if(!src.contains(w)) throw new Exception("{" + class_name + "}:[{" + template_id + "}] --> {"+domain+"}:{"+fingerprint+"} should contain " + w);
			}

			for(String w : not_contain) {
				if(src.contains(w)) throw new Exception("{" + class_name + "}:[{" + template_id + "}] --> {"+domain+"}:{"+fingerprint+"} should not contain " + w);
			}

		};
	}
}
