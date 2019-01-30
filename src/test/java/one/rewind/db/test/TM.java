package one.rewind.db.test;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelL;
import one.rewind.db.persister.JSONablePersister;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.ArrayList;
import java.util.List;

@DatabaseTable(tableName = "tm")
@DBName(value = "raw")
public class TM extends ModelL {

	class J1 implements JSONable<J1> {

		public String f1;

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	class J2 implements JSONable<J2> {

		public float k2;

		public List<String> l1 = new ArrayList<>();

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	@DatabaseField(persisterClass = JSONablePersister.class, columnDefinition = "MEDIUMTEXT")
	public J1 j1;

	@DatabaseField(persisterClass = JSONablePersister.class, columnDefinition = "MEDIUMTEXT")
	public J2 j2;

	public TM() {}

	/**
	 *
	 * @param f1
	 * @param k2
	 */
	public TM(String f1, float k2) {
		this.j1 = new J1();
		j1.f1 = f1;

		this.j2 = new J2();
		j2.k2 = k2;
		j2.l1.add(f1);
		j2.l1.add(f1);
		j2.l1.add(f1);
	}
}
