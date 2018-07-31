package one.rewind.io.requester.account;

public class AccountImpl extends Account {

	public AccountImpl(String domain, String username, String password) {
		super(domain, username, password);
	}

	public boolean update() throws Exception{
		return true;
	}
}
