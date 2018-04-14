package one.rewind.io.server;

public interface User {

	boolean isValid();

	boolean isEnabled();

	String getPrivateKey();
}
