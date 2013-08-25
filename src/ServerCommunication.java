/*
 * Author: John Massy-Greene
 * Program: TicTacTo3.0 - Internet Multiplayer
 * Date: 25/8/13
 * Comments: This is the protocol or way the client and server understand each other
 */
public class ServerCommunication {
	//------------------Server messages------------------------------------
	//a move has been made
	public String sMove = "MOVE:";
	//a particular players details
	//PLAYERDETAIL:name:piece:victory count
	public String sPDetail = "PLAYERDETAIL:";
	//The game has ended. Also the details of how its ended
	//ENDGAME:victory type:message as to who won and how they did it
	public String sEnd = "ENDGAME:";
	//message for either player wanting or responding to a new game
	public String sNew = "NEWGAME:";
	//message from the sever to wait for the opponents name. Used only in the set up
	public String sWaitName = "WAIT:NAME";
	//message from the server to wait for the opponent to move. Used only in set up
	public String sWaitMove = "WAIT:MOVE";
	//Wait for the opponents move. Used only in newgames and resets.
	public String sWaitBegin = "WAIT:";
	//It is now your turn. Used in set up, new games and resets
	public String sPlay = "PLAY:";
	//asking for the players name
	public String sNamePlease = "NAMEPLEASE";
	//message for either player wanting or responding to a reset
	public String sReset = "RESET:";
	//message telling the client that their opponent has quit
	public String sTheyQuit = "QUIT:";
	//the server has too many players and that the client is being rejected
	public String sReject = "REJECT:";
	
	//-----------------Client messages----------------------------------
	//communicating the players name
	public String cName = "PLAYERNAME:";
	//a move has been made
	public String cMove = "MOVE:";
	//response or request for a reset
	public String cReset = "RESET:";
	//letting the opponent know that your quitting
	public String cQuit = "QUIT:";
	//response or request for a new game
	public String cNew = "NEWGAME:";

	//------------------Server and Client messages------------------------------
	//approval of a request
	public String scOK = "OK";
	//denial of a request
	public String scNOT = "NOT";
	
}
