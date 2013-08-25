/*
 * Author: John Massy-Greene
 * Program: TicTacTo 3.0 - Internet Multiplayer - Client
 * Date: 25/8/13
 * Comments: In an MVC design this would be the part of the model. It is the part of the program
 * That communicates the actions of the player to the server. And tells the controller what the server
 * has sent to it
 */

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;


public class TTTClientTalker extends Thread {

//final variables
	private final int NUMSQUARES = 9;
	private final int PLAYERONE = 0;
	private final int PLAYERTWO = 1;
	private final String BORDERB = "<html><table border=\"1\"><tr><td><b>";
	private final String EBORDER = "</td></td></html>";
	private final String ENDBBR = "</b><br>";
	private final String NEWBUTTON = "NEW";
	private final String RESETBUTTON = "RESET";
	private final String SQUAREBUTTON = "SQUARE";
	private final char NOUGHT = 'O';
	private final char CROSS = 'X';
	
//variables given by TTTClientControl	
	private InetAddress serverIP;
	private int serverPort;
	private Socket client;
	private InterthreadComm interComm;
	private TTTView viewer;

//variables to send and receive messages in the socket
	private BufferedReader inServer;
	private DataOutputStream outServer;

//variables for communication and protocol
	private ServerCommunication comms = new ServerCommunication();
	private ClientMessages pMSG = new ClientMessages();
	private LinkedList<String> swingUtilMsg =  new LinkedList<String>();
	
//variable to determine which players details are being process during setup
	private int pDetail;

//-----------------------------------------------------------------------------------//	
	public TTTClientTalker(InetAddress serverIP, int serverPort,
			InterthreadComm interComm, TTTView viewer) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.interComm = interComm;
		this.viewer = viewer;
		this.pDetail = 0;
	}

//this is the main part of the program. The model is a thread that is run from the
//controller
	public void run() {
		boolean properConnect = false;
		try {
			client = new Socket(serverIP, serverPort);
			interComm.setConnect(client);
			
			inServer = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
			outServer = new DataOutputStream(client.getOutputStream());
			properConnect = true;
		//establishing a connection with the server
		} catch(IOException noConn2) {
			properConnect = false;
		}
		
		if(properConnect) {
			boolean setUp2 = false;
			
			while((client.isConnected()) && (!interComm.getQuit())) {
				if(setUp2 == false) {
//this part of the while loop only runs once. When the connection has been established with
//the server the names of each player are communicated as well as who is noughts, crosses and
//who goes first
					String playMSG = "";
					givePlayerMSG(pMSG.SETUP);
					
					playMSG = setUpPartTwo();						
					setUp2 = true;
//activate the board after setup has been done					
					if(!interComm.getQuit()) {
						activateMainButtons();
						givePlayerMSG(playMSG);
					} else {
						givePlayerMSG(pMSG.TOOMANY);
					}
//the socket is set to a timeout of 1 second. So the inputStreamReader doesn't block indefinately
					try {
						client.setSoTimeout(1000);
					} catch (SocketException e) {
						givePlayerMSG("Unable to set timeout");
					}

//this sets the default closing operation of the frame. So that when the user closes it, the model
//tells the server first and then shuts down.
					this.viewer.boardFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					this.viewer.boardFrame.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							quitGame();
						}
					});
				}
	
//the main part of the server client communication. Sending messages and receiving messages
				sendMessages();
				if(!interComm.getQuit()) {
					receiveMessages();
				}
				
				
			}
			try {
				inServer.close();
				outServer.close();
				client.close();
				System.exit(0);
			} catch(IOException nonClose) {
				givePlayerMSG("Can't close the connection");
			}
		} else {
			givePlayerMSG("Bad Connection. Shutting down");
			System.exit(0);
		}
	}

/*this is second part of the setting up of the game.
 * it involves the communicating of the players name to the server.
 * the server depending on if its full or not will tell the player to wait or send a rejection
 * then it will send the name of the other player as well as who is noughts and crosses
 * and who goes first.
 */
	private String setUpPartTwo() {
		String input = comms.sWaitName;
		String output;
		String result = pMSG.NOTTURN;
		
		output = comms.cName+interComm.getName()+pMSG.NLINE;
		
		try {
			//give your name to the server and then wait until
			//it gives information about what piece you using
			//as well as the name and piece of the other player
			outServer.writeBytes(output);
			input = inServer.readLine();
			//this input receives the command of WAIT or REJECT
			if(!input.equals(comms.sReject)) {
				input = inServer.readLine();
				handlePlayerDetail(input);
				
				input = inServer.readLine();
				handlePlayerDetail(input);
//lets the server know that the client has correctly received the
//each players details
				output = comms.scOK+pMSG.NLINE;
				outServer.writeBytes(output);
//if the server tells the client to play then its their turn
//otherwise it is the other players turn and they need to wait
				input = inServer.readLine();
				if(input.equals(comms.sPlay)) {
					interComm.setCurrentPiece(interComm.getMyPiece());
					result = pMSG.YOURTURN;
				} else if(input.equals(comms.sWaitMove)) {
					interComm.setCurrentPiece(interComm.getOtherPiece());
					result = pMSG.NOTTURN;
				}
			} else {
//if the server has sent a rejection then the player must quit.
				interComm.setQuit(true);
			}
			
			
		} catch(IOException nonStartCom) {
			givePlayerMSG("Cannot communicate names");
		} 
		return result;
	}

//function for handling of the players details when the game first starts
	private void handlePlayerDetail(String info) {
		String patterName = "^PLAYERDETAIL:(\\w+):([XO]):(\\d+)$";
		String name;
		String piece;
		String victories;
		String msg;
		
		Pattern p = Pattern.compile(patterName);
		Matcher m = p.matcher(info);
		if(m.find()) {
			String myName = interComm.getName();

//the details of each player includes their name, piece and victories
//the programmer realises that victories aren't really needed here
//however protocol for sending player information has been streamlined
//for the sake of programming simplicity.
			name = m.group(1);
			piece = m.group(2);
			victories = m.group(3);
			
			msg = "Starting game. You are ";
//since the first player details that the server sends is the players own then
//this gives the controller the information of which player is NOUGHTS and CROSSES
			if(this.pDetail == 0) {
				interComm.setMyPiece(piece.charAt(0));
				interComm.setOtherPiece(otherTurnPiece());
				if(piece.charAt(0) == CROSS) {
					msg += "CROSSES";
				} else {
					msg += "NOUGHTS";
				}
//If the first player detail's name is not the same as the name recorded by the controller
//it simply means that the other player has chosen the same name so the server has
//differentiated each player which a single digit at the end of their name.
				if(!myName.equals(name)) {
					givePlayerMSG("You and the other player have the same name.\n"
							+"So we have altered your name to: "+name);
					interComm.setName(name);
				}
				givePlayerMSG(msg);
				this.pDetail++;
			}
			setPlayerInfo(name, piece, victories);
		}
	}

//function for extracting the player information from the server and then diplaying
//it in the view
	private void setPlayerInfo(String pname, String piece, String vic) {
		String pnv = "Piece: "+piece+" Won: "+vic+" times";
		String playerMsg =
				BORDERB+pname+ENDBBR+pnv+EBORDER;
		String name = interComm.getName();
//always display the opponents name second.		
		if(name.equals(pname)) {
			setPlayerText(PLAYERONE, playerMsg);
		} else {
			setPlayerText(PLAYERTWO, playerMsg);
		}
	}

//Method for sending the server messages. Messages are obtained from the InterThreadComm controller
//who has been given messages to send from TTTClientControl depending on which buttons the player has
//pressed.
	public void sendMessages() {
		String output;
		try {
			while(!interComm.isMTSEmpty()) {
				output = interComm.getMsgToSend();
				outServer.writeBytes(output);
				
				if(output.equals(comms.cQuit+pMSG.NLINE)) {
					interComm.setQuit(true);
				}
			}
		} catch(IOException noSend) {
			givePlayerMSG("Can't send to server");
		}
	}

//method for receiving messages from the server and then
//depending on their contents executing a certain course of action
	public void receiveMessages() {
		String input;
		String g1;
		String g2;
		String pattern = "^([A-Z]+:)([a-zA-Z0-9:\\s.!]*)$";
		
		try {
			input = inServer.readLine();
			if(input != null) {
				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(input);
				if(m.find()) {
					g1 = m.group(1);
					g2 = m.group(2);
					
					if(g1.equals(comms.sMove)) {
						handleMove(g2);
					} else if(g1.equals(comms.sPlay)) {
						handleYourTurn();
					} else if(g1.equals(comms.sWaitBegin)) {
						handleNotTurn();
					} else if(g1.equals(comms.sNew)) {
						handleNewGame(g2);
					} else if(g1.equals(comms.sReset)) {
						handleReset(g2);
					} else if(g1.equals(comms.sTheyQuit)) {
						handleQuit();
					} else if(g1.equals(comms.sEnd)) {
						handleEnding(g2);
					} else if(g1.equals(comms.sPDetail)) {
						handleSPDetail(g2);
					}
				}
			}
		} catch(InterruptedIOException tiemout) {
			
		} catch (IOException nonRec) {
			givePlayerMSG("Cannot receceive msg");
		}
	}

//function to handle moves from the opponent
	private void handleMove(String g2) {
		String pattern = "(^[0-9]$)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(g2);
		
		if(m.find()) {						
			int move = Integer.parseInt(m.group(0));
			doViewMove(move);
			interComm.setCurrentPiece(interComm.getMyPiece());
			
			//givePlayerMSG(pMSG.YOURTURN);
		}
	}
	
//the server will let the player know when it is their turn.
	private void handleYourTurn() {
		givePlayerMSG(pMSG.YOURTURN);
		if(interComm.getEnd()) {
			interComm.setEnd(false);
			interComm.setCurrentPiece(interComm.getMyPiece());
		}
	}
	
//this function isn't really used outside of new games and resets. Its to let the player
//know that it is their opponents turn.
	
	private void handleNotTurn() {
		givePlayerMSG(pMSG.NEWNOTTURN);
		if(interComm.getEnd()) {
			interComm.setEnd(false);
			interComm.setCurrentPiece(interComm.getOtherPiece());
		}
	} 

//the code for the new game and reset handling is the same with the exception of the
//variables they use or need.
	private void handleNewGame(String g2) {
		NRHandler(g2, pMSG.NEWGAMEDIALOG, comms.cNew, pMSG.OTHERYESNEW, pMSG.OTHERNONEW, 0);
	}
	private void handleReset(String g2) {
		NRHandler(g2, pMSG.RESETDIALOG, comms.cReset, pMSG.OTHERYESRESET, pMSG.OTHERNORESET, 1);
	}

//lets the player know that their opponent has quit.
	private void handleQuit() {
		for(int i = 0; i<NUMSQUARES; i++) {
			activateButtons(SQUAREBUTTON, "false", i);
		}
		activateButtons(NEWBUTTON, "false", 0);
		activateButtons(RESETBUTTON, "false", 0);
		givePlayerMSG(pMSG.OTHERQUIT);
	}

//the server will tell the player when the game has ended
	private void handleEnding(String g2) {
		int vicType;
		String msg;
		char vicPiece;
		String pattern = "^(\\d+):([\\w\\s]+):([X|O])$";
		String pattern2 = "^(\\d+):([\\w\\s.!]+)$";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(g2);
	
//if the first pattern has been found it means that one player has won
//otherwise there has been a tie.
		if(m.find()) {
			vicType = Integer.parseInt(m.group(1));
			msg = m.group(2);
			vicPiece = m.group(3).charAt(0);
//this sorta violates the MVC principles but this tells the view
//how the game has ended and how to show that to the player
			endTheGame(vicType, vicPiece);
//lets the controller know the game has ended
			this.interComm.setEnd(true);
			givePlayerMSG(msg);
			activateButtons(NEWBUTTON, "true", 0);
		} else {
			Pattern p2 = Pattern.compile(pattern2);
			Matcher m2 = p2.matcher(g2);
			if(m2.find()) {
				this.interComm.setEnd(true);
				givePlayerMSG(m2.group(2));
				activateButtons(NEWBUTTON, "true", 0);
			}
		}
	}

//grabs the player details from the server to update the view.
	private void handleSPDetail(String g2) {
		String name;
		String piece;
		String victory;
		
		String pattern = "^(\\w+):([XO]):(\\d+)$";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(g2);
		if(m.find()) {
			name = m.group(1);
			piece = m.group(2);
			victory = m.group(3);
			setPlayerInfo(name, piece, victory);
		}
	}
	
//a concurrent-safe method of performing a move in the view
	private void doViewMove(int i) {
		swingUtilMsg.add(Integer.toString(i));
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int move = Integer.parseInt(swingUtilMsg.remove());
				viewer.doMove(viewer.getSquare(move), interComm.getOtherPiece());
			}
		});
	}
		
//a concurrent-safe method of updating the players text in the view
	private void setPlayerText(int player, String msg) {
		String playerNo = Integer.toString(player);
		swingUtilMsg.add(playerNo);
		swingUtilMsg.add(msg);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int playNo = Integer.parseInt(swingUtilMsg.remove());
				viewer.getPlayer(playNo).setText(swingUtilMsg.remove());
			}
 		});
	}
 
//concurrent-safe way of giving the player a message
	private void givePlayerMSG(String msg) {
		swingUtilMsg.add(msg);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(viewer.boardFrame, swingUtilMsg.remove());
				}
			});
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(viewer.boardFrame, "Could not show message");
		} catch (InvocationTargetException e) {
			JOptionPane.showMessageDialog(viewer.boardFrame, "Could not show message");
		}
	}

//concurrent-safe way of presenting the player with a yes/no dialog	
	private int givePlayerYNDialog(String dialog) {
		int result;
		swingUtilMsg.add(dialog);
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					int answer;
					String dialog = swingUtilMsg.remove();
					
					answer = JOptionPane.showConfirmDialog(null,
							dialog,
							"Message",
							JOptionPane.YES_NO_OPTION);
					
					swingUtilMsg.add(Integer.toString(answer));
				}
			});
		} catch (InterruptedException e) {
			givePlayerMSG("Cannot handle dialog call");
			result = 1;
		} catch (InvocationTargetException e) {
			givePlayerMSG("Cannot handle dialog call");
			result = 1;
		}
		result = Integer.parseInt(swingUtilMsg.remove());
		return result;
	}
	
//concurrent safe way of switching buttons on or off
	private void activateButtons(String button, String switched, int i) {
		swingUtilMsg.add(button);
		swingUtilMsg.add(switched);
		swingUtilMsg.add(Integer.toString(i));
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String butts = swingUtilMsg.remove();
				boolean onOff = Boolean.valueOf(swingUtilMsg.remove());
				int i = Integer.parseInt(swingUtilMsg.remove());
				
				if(butts.equals(SQUAREBUTTON)) {
					viewer.getSquare(i).setEnabled(onOff);
				} else if(butts.equals(RESETBUTTON)) {
					viewer.getReset().setEnabled(onOff);
				} else  if(butts.equals(NEWBUTTON)) {
					viewer.getNewgame().setEnabled(onOff);
				}
			}
		});
	}
//a concurrent safe method of resetting the board
	public void resetTheBoard() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				viewer.resetBoard();
			}
		});
	}

//concurrent safe method of doing endgame tasks
	public void endTheGame(int vicType, char currentPiece){
		swingUtilMsg.add(Integer.toString(vicType));
		swingUtilMsg.add(String.valueOf(currentPiece));
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int vicType = Integer.parseInt(swingUtilMsg.remove());
				char currentPiece = swingUtilMsg.remove().charAt(0);
				viewer.endGame(vicType, currentPiece);
			}
		});
		
	}

//function that handles the reset and new game tasks that the server is telling the
//client about
	private void NRHandler(String g2, String dialog, String cMsg,
			String OTHERYES, String OTHERNO, int nr) {
		int answer = -1;
		String msg;

//if there is nothing in g2 it means that the server is telling the player
//that their opponent wants either a new game or want to reset.
		if(g2.equals("")) {
			while(answer == -1) {
				answer = givePlayerYNDialog(dialog);
			}
//the player is give a yes/no dialog and their choice is then sent to the server.		
			if(answer == 0) {
				msg = comms.scOK;
				resetTheBoard();
				activateButtons(NEWBUTTON, "false", 0);
				interComm.switchCurrentPiece();
				if(nr == 1) {
					this.interComm.setEnd(true);
				}
			} else {
				msg = comms.scNOT;
			}
			interComm.addMsgToSend(cMsg+msg+pMSG.NLINE);
//if g2 has either a OK or NOT message in it, it means that the server is telling
//the client that their opponent is RESPONDING to a new game or reset that the player
//asked for.
		} else {
			if(g2.equals(comms.scOK)) {
				givePlayerMSG(OTHERYES);
				resetTheBoard();
				activateButtons(NEWBUTTON, "false", 0);
				interComm.switchCurrentPiece();
				if(nr == 1) {
					this.interComm.setEnd(true);
				}
			} else if(g2.equals(comms.scNOT)) {
				givePlayerMSG(OTHERNO);
			}
			interComm.setNRCalled(nr, false);
		}
	}
//activates the board buttons and the reset button
//the newgame button is only accessible when a game has finished.
	private void activateMainButtons() {
		for(int i=0; i<NUMSQUARES; i++) {
			activateButtons(SQUAREBUTTON, "true", i);
		}
		activateButtons(RESETBUTTON, "true", 0);
	}

//tells the controller what piece your opponent has based upon
//which piece the client has been given by the server
	private char otherTurnPiece() {
		char result;
		if(interComm.getMyPiece() == NOUGHT) {
			result = CROSS;
		} else {
			result = NOUGHT;
		}
		return result;
	}
//method to quit the game. If still connected to the server
//then tell the server that your quitting and then shutdown.
//if no connection then just shut down.
	private void quitGame() {
		if(this.interComm.getConnect().isConnected()) {
			this.interComm.addMsgToSend(comms.cQuit+pMSG.NLINE);
		} else {
			System.exit(0);
		}
	}
}
