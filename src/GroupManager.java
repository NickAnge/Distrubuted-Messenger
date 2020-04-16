

import java.io.*;
import java.net.*;
import java.util.*;

public class GroupManager {
    public String MultiCastAddress = "230.0.0.0";
    public int MultiCastPort = 4321;


    private List<GroupInfo> ListOfGroupsIntoManager;
    private int numberOfGroups;
    private List<Socket> activeMembers;

    public List<Socket> getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(List<Socket> activeMembers) {
        activeMembers = activeMembers;
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public List<GroupInfo> getListOfGroupsIntoManager() {
        return ListOfGroupsIntoManager;
    }

    public void setListOfGroupsIntoManager(List<GroupInfo> listOfGroupsIntoManager) {
        ListOfGroupsIntoManager = listOfGroupsIntoManager;
    }

    public GroupManager() {
        ListOfGroupsIntoManager = new ArrayList<GroupInfo>();
        numberOfGroups = 0;
        activeMembers = new ArrayList<Socket>();
    }

    public  String getMsgFromSocket(Socket socket){
        String data = "NoMessage";

        try {
            socket.setSoTimeout(500);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = in.readLine();
//            System.out.println("\r\nMessage from " + socket.getInetAddress().getHostAddress() + ": " + data);
        }
        catch (SocketTimeoutException e) {
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
    public   void sendMsgFromSocket(Socket socket,String msg)  {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(msg);
            out.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return ;
    }

    public  void sendNewMessageToSocket(Socket socket,Object object){
        ObjectInputStream in = null;
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Trying to send Message Group");
            out.writeObject(object);
            out.flush();
        } catch (SocketException e) {
        }
        catch (IOException e){

        }
        return ;
    }

    public Object getNewMessageFromSocket(Socket socket){
        ObjectInputStream in = null;
        try {
            socket.setSoTimeout(500);
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Middleware receiving new VIEW ");
            GroupInfo newVIew = (GroupInfo) in.readObject();
            return  newVIew;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public  void addMemberToGroups(Socket socket ,String Member){ ///function gia na prosthesoume ena melos eite se nea omada eite se mia palia

        String []splitMsg = Member.split(" ",4);
        EachMemberInfo NewMember = new EachMemberInfo(splitMsg[1],socket,splitMsg[2],Integer.parseInt(splitMsg[3]));
        int flag = 0;
        System.out.println("Info of the new member sended...");


        Iterator<GroupInfo> it = this.ListOfGroupsIntoManager.iterator();
        while(it.hasNext()){
            GroupInfo temp = it.next();
            if (temp.getGroupName().equals(splitMsg[0])) {
                temp.getMembers().add(NewMember);
                flag = 0;
                System.out.println("send GroupView");
                String addMsg = new String("New Member added to the Group:" + NewMember.getName());
                temp.getCoInfo().setDeliverno(-1);
                Message newView = new Message("Add",temp,addMsg);
                this.informTheGroup(temp,newView,NewMember.getName());
//                Message mesg = new Message("Add",temp);
                sendNewMessageToSocket(NewMember.getAppSocket(),newView);
                break;
            } else {
                flag = 1;
            }
        }
        if(flag == 1 || ListOfGroupsIntoManager.isEmpty()){
            flag = 0;
            System.out.println("NEW GROUP");
            int gsock =getNumberOfGroups();
            System.out.println("GSOCK:"+ gsock);
            setNumberOfGroups(++gsock);
            CoorditatorInfo coordinator = new CoorditatorInfo(-1,NewMember);
            GroupInfo NewGroup =  new GroupInfo(splitMsg[0],getNumberOfGroups(),coordinator);

            NewGroup.getMembers().add(NewMember);

            ListOfGroupsIntoManager.add(NewGroup);
            String addMsg = new String("New Member added to the Group:" + NewMember.getName());

            Message mesg = new Message("Add",NewGroup,addMsg);

            sendNewMessageToSocket(socket,mesg);//apostolh new View
        }

    }

    public void printList(List<GroupInfo> groups){

        for(int i = 0; i <groups.size();i++){
            System.out.println("GroupName: " + groups.get(i).getGroupName());
            System.out.println("ID : " + groups.get(i).getId());
            for(int j =0 ;j <groups.get(i).getMembers().size();j++){
                System.out.println("MemberName: " +groups.get(i).getMembers().get(j).getName());
                System.out.println("MemberAddress: "+ groups.get(i).getMembers().get(j).getMemberAddress());
                System.out.println("MemberPort: "+ groups.get(i).getMembers().get(j).getMemberPort());
            }

        }
    }//function gia na ektupwnoume ola ta groups me ta teams
    public  void informTheGroup(GroupInfo group,Message newView,String NoSend){

        for(int i = 0; i < group.getMembers().size();i++){
            if(group.getMembers().get(i).getName().equals(NoSend)){
                continue;
            }
            System.out.println("Sends new VIew to Members");
            this.sendNewMessageToSocket(group.getMembers().get(i).getAppSocket(),newView);
        }
    } //eidopoiei ayto to group gia to kainourgio view

    public GroupInfo findMembersGroup(int idOfGroup){
        Iterator<GroupInfo> it = ListOfGroupsIntoManager.iterator();

        while(it.hasNext()){
            GroupInfo temp = it.next();

            if(temp.getId() == idOfGroup){
                return temp;
            }
        }
        return null;
    }//briskei to group me ayto to id kai to epistreefei

    public  void removeFromAllGroups(Socket specificSocket){
//        System.out.println("MPHKA mesa sto remove all");

        Iterator<GroupInfo> it = ListOfGroupsIntoManager.iterator();
        int tim2 = ListOfGroupsIntoManager.size();

        for(int j =0;j<tim2;j++){
            GroupInfo temp = ListOfGroupsIntoManager.get(j);
            if(temp == null){
                break;
            }
            int tim = temp.getMembers().size();
            for(int i =0;i<tim;i++){
                EachMemberInfo member = temp.getMembers().get(i);
                if(member.getAppSocket().equals(specificSocket)){
                    String errorMsg = new String("This member got an error and left the Group: "+ member.getName());

                    if(temp.getCoInfo().getCoMember().getMemberPort() == member.getMemberPort()){
                        temp.getMembers().remove(member);
                        temp = this.changeCoordinator(temp);
                    }
                    else{
                        temp.getMembers().remove(member);
                    }
//                    if(temp != null ){
                    if(temp.getMembers().size() == 0){
                        ListOfGroupsIntoManager.remove(temp);
                        break;
                    }
                    Message view = new Message("Error",temp,errorMsg);
                    informTheGroup(temp,view,member.getName());
                    break;
                }
            }
            if(ListOfGroupsIntoManager.size() < tim2) {
                tim2--;
                j--;
            }
        }

        return;
    } //afairei to socket-melos apo oles ties omades stis opoies brisketai se periptvsh blabhs

    public GroupInfo changeCoordinator(GroupInfo oldView){

        if(oldView.getMembers().size() > 0){
            CoorditatorInfo newCord = new CoorditatorInfo(-1,oldView.getMembers().get(0));
            oldView.setCoInfo(newCord);
            return oldView;
        }
        else {
            return oldView;
        }
//        return  null;
    }
}


