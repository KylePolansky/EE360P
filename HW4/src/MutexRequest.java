import java.io.DataOutputStream;
import java.util.Comparator;

public class MutexRequest implements Comparator<MutexRequest> {
	int ServerID;
	int TimeStamp;
	String command;
	DataOutputStream outputStream;

	public MutexRequest(int ServerID, int TimeStamp, String command) {
		this(ServerID, TimeStamp, command, null);
	}

	public MutexRequest(int ServerID, int TimeStamp, String command, DataOutputStream outputStream) {
		this.ServerID = ServerID;
		this.TimeStamp = TimeStamp;
		this.command = command;
		this.outputStream = outputStream;
	}

	@Override
	public int compare(MutexRequest o1, MutexRequest o2) {
		if (o1.TimeStamp > o2.TimeStamp) {
			return 1;
		}
		if (o1.TimeStamp < o2.TimeStamp) {
			return -1;
		}
		if (o1.ServerID > o2.ServerID) {
			return 1;
		}
		if (o1.ServerID < o2.ServerID) {
			return -1;
		}
		return 0;
	}
}