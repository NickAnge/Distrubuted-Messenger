import java.beans.Transient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GroupInfo implements Serializable {
    private  String groupName;
    private List<EachMemberInfo> Members;

    public GroupInfo(String groupName) {
        this.groupName = groupName;
        Members = new ArrayList<EachMemberInfo>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<EachMemberInfo> getMembers() {
        return Members;
    }

}


class EachMemberInfo implements Serializable {
    private String Name;
    private transient  Socket AppSocket ;
    private String MemberAddress;
    int MemberPort;

    public EachMemberInfo(String name, Socket appSocket, String memberAddress, int memberPort) {
        Name = name;
        AppSocket = appSocket;
        MemberAddress = memberAddress;
        MemberPort = memberPort;
    }
    public Socket getAppSocket() {
        return AppSocket;
    }

    public void setAppSocket(Socket appSocket) {
        AppSocket = appSocket;
    }

    public EachMemberInfo(String name, String memberAddress, int memberPort) {
        Name = name;
        MemberAddress = memberAddress;
        MemberPort = memberPort;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getMemberAddress() {
        return MemberAddress;
    }

    public void setMemberAddress(String memberAddress) {
        MemberAddress = memberAddress;
    }

    public int getMemberPort() {
        return MemberPort;
    }

    public void setMemberPort(int memberPort) {
        MemberPort = memberPort;
    }
}