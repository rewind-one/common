package one.rewind.io.requester.exception;

public class TaskException extends Exception {

	public static class NoMoreStepException extends Exception {}

	public static class LessThanMinIntervalException extends Exception {}

	public static class DuplicateContentException extends Exception {}
}
