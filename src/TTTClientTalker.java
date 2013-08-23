import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


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

//make this swing util friendly
	public void run() {
		boolean properConnect = false;
		try {
			client = new Socket(serverIP, serverPort);
			interComm.setConnect(client);
			
			inServer = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
			outServer = new DataOutputStream(client.getOutputStream());
			properConnect = true;
		
		} catch(IOException noConn2) {
			properConnect = false;
		}
		
		if(properConnect) {
			boolean setUp2 = false;
			//need to add the boolean condition for quitter here
			while((client.isConnected()) && (!interComm.getQuit())) {
				if(setUp2 == false) {
					String playMSG = "";
					givePlayerMSG(pMSG.SETUP);
					
					playMSG = setUpPartTwo();						
					setUp2 = true;
					
					activateMainButtons();
					givePlayerMSG(playMSG);
					try {
						client.setSoTimeout(1000);
					} catch (SocketException e) {
						givePlayerMSG("Unable to set timeout");
					}
				}
				
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
		}
	}

	
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
			while(input.equals(comms.sWaitName)) {
				input = inServer.readLine();
			}
			handlePlayerDetail(input);
			
			input = inServer.readLine();
			handlePlayerDetail(input);
			
			output = comms.scOK+pMSG.NLINE;
			outServer.writeBytes(output);
			
			input = inServer.readLine();
			if(input.equals(comms.sPlay)) {
				interComm.setCurrentPiece(interComm.getMyPiece());
				result = pMSG.YOURTURN;
			} else if(input.equals(comms.sWaitMove)) {
				interComm.setCurrentPiece(interComm.getOtherPiece());
				result = pMSG.NOTTURN;
			}
			
		} catch(IOException nonStartCom) {
			givePlayerMSG("Cannot communicate names");
		}
		return result;
	}


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
			
			name = m.group(1);
			piece = m.group(2);
			victories = m.group(3);
			
			msg = "Starting game. You are ";
			
			if(this.pDetail == 0) {
				interComm.setMyPiece(piece.charAt(0));
				interComm.setOtherPiece(otherTurnPiece());
				if(piece.charAt(0) == CROSS) {
					msg += "CROSSES";
				} else {
					msg += "NOUGHTS";
				}
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

	
	private void setPlayerInfo(String pname, String piece, String vic) {
		String pnv = "Piece: "+piece+" Won: "+vic+" times";
		String playerMsg =
				BORDERB+pname+ENDBBR+pnv+EBORDER;
		String name = interComm.getName();
		
		if(name.equals(pname)) {
			setPlayerText(PLAYERONE, playerMsg);
		} else {
			setPlayerText(PLAYERTWO, playerMsg);
		}
	}
	
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
	
	public void receiveMessages() {
		String input;
		String g1;
		String g2;
		String pattern = "^([A-Z]+:)([a-zA-Z0-9:\\s.!]*)$";
		
		try {
			input = inServer.readLine();
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
		} catch(InterruptedIOException tiemout) {
			
		} catch (IOException nonRec) {
			givePlayerMSG("Cannot receceive msg");
		}
	}
	
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
	
	private void handleYourTurn() {
		givePlayerMSG(pMSG.YOURTURN);
		if(interComm.getEnd()) {
			interComm.setEnd(false);
			interComm.setCurrentPiece(interComm.getMyPiece());
		}
	}
	private void handleNotTurn() {
		givePlayerMSG(pMSG.NEWNOTTURN);
		if(interComm.getEnd()) {
			interComm.setEnd(false);
			interComm.setCurrentPiece(interComm.getOtherPiece());
		}
	} 
	
	private void handleNewGame(String g2) {
		NRHandler(g2, pMSG.NEWGAMEDIALOG, comms.cNew, pMSG.OTHERYESNEW, pMSG.OTHERNONEW, 0);
	}
	
	private void handleReset(String g2) {
		NRHandler(g2, pMSG.RESETDIALOG, comms.cReset, pMSG.OTHERYESRESET, pMSG.OTHERNORESET, 1);
	}
	
	private void handleQuit() {
		for(int i = 0; i<NUMSQUARES; i++) {
			activateButtons(SQUAREBUTTON, "false", i);
		}
		activateButtons(NEWBUTTON, "false", 0);
		activateButtons(RESETBUTTON, "false", 0);
		givePlayerMSG(pMSG.OTHERQUIT);
	}
	
	private void handleEnding(String g2) {
		int vicType;
		String msg;
		char vicPiece;
		String pattern = "^(\\d+):([\\w\\s]+):([X|O])$";
		String pattern2 = "^(\\d+):([\\w\\s.!]+)$";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(g2);
		
		if(m.find()) {
			vicType = Integer.parseInt(m.group(1));
			msg = m.group(2);
			vicPiece = m.group(3).charAt(0);
			endTheGame(vicType, vicPiece);
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

	private void NRHandler(String g2, String dialog, String cMsg,
			String OTHERYES, String OTHERNO, int nr) {
		int answer = -1;
		String msg;
		if(g2.equals("")) {
			while(answer == -1) {
				answer = givePlayerYNDialog(dialog);
			}
			
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

	private void activateMainButtons() {
		for(int i=0; i<NUMSQUARES; i++) {
			activateButtons(SQUAREBUTTON, "true", i);
		}
		activateButtons(RESETBUTTON, "true", 0);
	}
	
	private char otherTurnPiece() {
		char result;
		if(interComm.getMyPiece() == NOUGHT) {
			result = CROSS;
		} else {
			result = NOUGHT;
		}
		return result;
	}
	
}

//LINKED LIST IS FIFO
