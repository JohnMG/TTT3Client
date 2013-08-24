
/*
 * Author: John Massy-Greene
 * Program: TicTacTo3.0 - Internet Multiplayer
 * Date: 28/4/13
 */
public class ServerCommunication {
	//Server messages
	public String sMove = "MOVE:";
	public String sPDetail = "PLAYERDETAIL:";
	//PLAYERDETAIL:name:piece:victory count
	public String sEnd = "ENDGAME:";
	//ENDGAME:vic type:msg
	public String sNew = "NEWGAME:";
	public String sWaitName = "WAIT:NAME";
	public String sWaitMove = "WAIT:MOVE";
	public String sWaitBegin = "WAIT:";
	public String sPlay = "PLAY:";
	public String sNamePlease = "NAMEPLEASE";
	public String sReset = "RESET:";
	public String sTheyQuit = "QUIT:";
	
	public String sReject = "REJECT:";
	
	//Client messages
	public String cName = "PLAYERNAME:";
	public String cMove = "MOVE:";
	public String cReset = "RESET:";
	public String cQuit = "QUIT:";
	public String cNew = "NEWGAME:";

	//Server and Client messages
	public String scOK = "OK";
	public String scNOT = "NOT";
	
}
