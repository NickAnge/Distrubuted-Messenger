import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class GroupInfo {
    String groupName;
    List<EachMemberInfo> Members;

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


class EachMemberInfo {
    String Name;
    InetAddress MemberAddress;
    int MemberPort;

    public EachMemberInfo(String name, InetAddress memberAddress, int memberPort) {
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

    public InetAddress getMemberAddress() {
        return MemberAddress;
    }

    public void setMemberAddress(InetAddress memberAddress) {
        MemberAddress = memberAddress;
    }

    public int getMemberPort() {
        return MemberPort;
    }

    public void setMemberPort(int memberPort) {
        MemberPort = memberPort;
    }
}