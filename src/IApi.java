public interface IApi {
    String MultiCastGroupAddress = "230.0.0.0";
    int MultiCastPort = 4321;

    int grp_join(String grpName, String myId);
    int grp_leave(int gSock);
    int grp_send(int gSock,String msg,int len,int total);
    int grp_recv(int gSock,int type,String msg,int len, int block);

}
