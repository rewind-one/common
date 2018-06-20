package one.rewind.io.ssh;

public interface RemoteShell {

	public String exec(String cmd);

	public String getHost();

	public int getVncPort();
}
