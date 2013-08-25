/* Author: John Massy-Greene
 * Program: TicTacTo 3.0
 * New Functionality: Internet Capability
 * Date: 25/8/13
 * Comment: In the context of MVC design, this is part of the controller. The other part
 * 			of the controller is TTTClientControl. This is the bridge between what the server is
 *          sending to TTTClientTalker and what the player has done to the view with TTTClientControl
 */


import java.net.Socket;
import java.util.LinkedList;


public class InterthreadComm {

//final global constants
	private final char NOUGHT = '0';
	private final char CROSS = 'X';
	
//variables for name and whether the game is ending or if someone is quitting
	private String name;
	private boolean quitter;
	private boolean endGame;
	
//a linked list of the messages that the client needs to send to the server
	private LinkedList<String> msgToSend;
	private char myPiece; 
	private char otherPiece;
	//use the piece to keep track of whose move
	private char currentTurnPiece;
	//boolean array to check if New game(N) or Reset(R) has been called
	private boolean[] NRCalled = {false, false};
	
	private Socket connect;
	
	public InterthreadComm(String name) {
		this.name = name;
		this.quitter = false;
		this.msgToSend = new LinkedList<String>();
		this.endGame = false;
	}
	
	
//getters and setters
	public String getName() {
		return this.name;
	}
	public synchronized void setName(String name) {
		this.name = name;
	}
	
	public boolean getQuit() {
		return this.quitter;
	}
	public synchronized void setQuit(Boolean quit) {
		this.quitter = quit;
	}
	
	public synchronized boolean isMTSEmpty() {
		return this.msgToSend.isEmpty();
	}
	public synchronized String getMsgToSend() {
		return this.msgToSend.remove();
	}
	public synchronized void addMsgToSend(String msg) {
		this.msgToSend.add(msg);
	}
	
	public char getMyPiece() {
		return this.myPiece;
	}
	public synchronized void setMyPiece(char piece) {
		this.myPiece = piece;
	}
	
	public char getOtherPiece() {
		return this.otherPiece;
	}
	public synchronized void setOtherPiece(char piece) {
		this.otherPiece = piece;
	}
	
	public char getCurrentPiece() {
		return this.currentTurnPiece;
	}
	public synchronized void setCurrentPiece(char piece) {
		this.currentTurnPiece = piece;
	}
	public synchronized void switchCurrentPiece() {
		if(this.currentTurnPiece == NOUGHT) {
			this.currentTurnPiece = CROSS;
		} else {
			this.currentTurnPiece = NOUGHT;
		}
	}
	
	public boolean getNRCalled(int type) {
		return this.NRCalled[type];
	} 
	public synchronized void setNRCalled(int type, boolean val) {
		this.NRCalled[type] = val;
	}
	
	public Socket getConnect() {
		return this.connect;
	}
	public void setConnect(Socket connect) {
		this.connect = connect;
	}
	
	public boolean getEnd() {
		return this.endGame;
	}
	public synchronized void setEnd(boolean switcher) {
		this.endGame = switcher;
	}
	
	
}
