package one.rewind.json;

import java.io.Serializable;

/**
 * Created by Luke on 1/19/16. 
 * mailto:stormluke1130@gmail.com
 */
public interface JSONable<T> extends Serializable {

	String toJSON();
}
