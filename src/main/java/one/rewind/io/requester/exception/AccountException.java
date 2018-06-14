package one.rewind.io.requester.exception;

import one.rewind.io.requester.account.Account;

/**
 * 账户异常
 */
public class AccountException extends Exception {

	/**
	 * 账户失效，账号被永久封禁，账号密码遗失等
	 */
	public static class Failed extends Exception {

		public Account account;

		public Failed(Account account) {
			this.account = account;
		}
	}

	/**
	 * 账户被冻结
	 */
	public static class Frozen extends Exception {

		public Account account;

		public Frozen(Account account) {
			this.account = account;
		}
	}

	public static class NotFound extends Exception {}
}
