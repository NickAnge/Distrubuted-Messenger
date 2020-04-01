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
    private int id;

    public GroupInfo(String groupName,int newid) {
        this.groupName = groupName;
        Members = new ArrayList<EachMemberInfo>();
        id = newid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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


class Message implements  Serializable {
    private String type; //Add, Leave, GroupView
    private GroupInfo View;
    private UdpMessage Message;

    public UdpMessage getMessage() {
        return Message;
    }

    public void setMessage(UdpMessage message) {
        Message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GroupInfo getView() {
        return View;
    }

    public void setView(GroupInfo view) {
        View = view;
    }



    public Message(String type, GroupInfo view, UdpMessage message) {
        this.type = type;
        View = view;
        Message = message;
    }

    public Message(String type, GroupInfo view) {
        this.type = type;
        View = view;
    }

    public Message(String type, UdpMessage message) {
        this.type = type;
        Message = message;
    }
}

class UdpMessage implements  Serializable{
    private String Message;
    private int seqNo;
    private int senderPort;
    private int groupId;

    public UdpMessage(String message, int seqno,int senderPort,int groupId) {
        Message = message;
        seqNo = seqno;
        this.senderPort = senderPort;
        this.groupId = groupId;

    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }
}