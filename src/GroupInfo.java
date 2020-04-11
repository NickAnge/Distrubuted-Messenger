import java.beans.Transient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupInfo implements Serializable {
    private  String groupName;
    private List<EachMemberInfo> Members;
    private int id;

    public CoorditatorInfo getCoInfo() {
        return coInfo;
    }

    public void setCoInfo(CoorditatorInfo coInfo) {
        this.coInfo = coInfo;
    }

    private CoorditatorInfo coInfo;

    public GroupInfo() {
        Members = new ArrayList<>();
    }

    public GroupInfo(String groupName, int newid, CoorditatorInfo Coinfo) {
        this.groupName = groupName;
        Members = new ArrayList<EachMemberInfo>();
        id = newid;
        this.coInfo = Coinfo;
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
    int position;


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
    private String name;
    private String changeViewMessage;


    public String getChangeViewMessage() {
        return changeViewMessage;
    }

    public void setChangeViewMessage(String changeViewMessage) {
        this.changeViewMessage = changeViewMessage;
    }

    public UdpMessage getMessage() {
        return Message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public  Message(String type, GroupInfo view){
        this.type = type;
        View = view;
    }
    public Message(String type, GroupInfo view, String msg) {
        this.type = type;
        View = view;
        this.changeViewMessage = msg;
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
    private List<Integer> membersSend;
    private  int startingSender;
    private  int selfDelivered;
    private int totaldelivered;
    private  int total;
    private int dontSendAgain;
    private int rMessageDontSend;

    public int getSelfDelivered() {
        return selfDelivered;
    }

    public void setSelfDelivered(int selfDelivered) {
        this.selfDelivered = selfDelivered;
    }

    public UdpMessage() {
        this.membersSend = new ArrayList<>();
    }

    public int getStartingSender() {
        return startingSender;
    }

    public void setStartingSender(int startingSender) {
        this.startingSender = startingSender;
    }

    public UdpMessage(String message, int seqno, int senderPort, int groupId, int startingSender,int total,int sended,int totaldelivered) {
        Message = message;
        seqNo = seqno;
        this.senderPort = senderPort;
        this.groupId = groupId;
        this.membersSend = new ArrayList<>();
        this.startingSender = startingSender;
        this.selfDelivered =sended;
        this.totaldelivered = totaldelivered;
        this.total = total;
        dontSendAgain = -1;
        rMessageDontSend = -1;
    }

    public int getrMessageDontSend() {
        return rMessageDontSend;
    }

    public void setrMessageDontSend(int rMessageDontSend) {
        this.rMessageDontSend = rMessageDontSend;
    }

    public int getDontSendAgain() {
        return dontSendAgain;
    }

    public void setDontSendAgain(int dontSendAgain) {
        this.dontSendAgain = dontSendAgain;
    }

    public int getTotaldelivered() {
        return totaldelivered;
    }

    public void setTotaldelivered(int totaldelivered) {
        this.totaldelivered = totaldelivered;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Integer> getMembersSend() {
        return membersSend;
    }

    public void setMembersSend(List<Integer> membersSend) {
        this.membersSend = membersSend;
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

class GroupMessages implements  Serializable {
    List<Message> msgs;
    List<Message> viewsOfTheTeam;
    HashMap<Integer,Integer> fifoOrders;
    int delivered;

    public int getDelivered() {
        return delivered;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
    }

    public HashMap<Integer, Integer> getFifoOrders() {
        return fifoOrders;
    }

    public void setFifoOrders(HashMap<Integer, Integer> fifoOrders) {
        this.fifoOrders = fifoOrders;
    }

    public GroupMessages() {
        this.msgs = new ArrayList<>();
        this.viewsOfTheTeam = new ArrayList<>();
        this.fifoOrders = new HashMap<>();
        delivered = 0;
    }

    public List<Message> getMsgs() {
        return msgs;
    }

    public void setMsgs(List<Message> msgs) {
        this.msgs = msgs;
    }

    public List<Message> getViewsOfTheTeam() {
        return viewsOfTheTeam;
    }

    public void setViewsOfTheTeam(List<Message> viewsOfTheTeam) {
        this.viewsOfTheTeam = viewsOfTheTeam;
    }
}

class CoorditatorInfo implements  Serializable {
    private int deliverno;
    private  EachMemberInfo coMember;

    public CoorditatorInfo(int deliverno, EachMemberInfo coMember) {
        this.deliverno = deliverno;
        this.coMember = coMember;
    }

    public int getDeliverno() {
        return deliverno;
    }

    public void setDeliverno(int deliverno) {
        this.deliverno = deliverno;
    }

    public EachMemberInfo getCoMember() {
        return coMember;
    }

    public void setCoMember(EachMemberInfo coMember) {
        this.coMember = coMember;
    }
}