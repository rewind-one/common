package one.rewind.io.requester.exception;

public class AccountException extends Exception {

	public static class Failed extends Exception {};

	public static class Frozen extends Exception {};

}
