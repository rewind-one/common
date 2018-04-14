package one.rewind.monitor;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;

public abstract class SysInfo implements JSONable {
	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	public abstract void probe();
}