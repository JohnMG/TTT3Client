/*
 * Author: John Massy-Greene
 * Program: TicTacTo 3.0 - Internet Multiplayer - Client
 * Date: 25/8/13
 * Comments: In an MVC design this would be the part of the controller. It communicates between
 * 			 with the view(to implement the players actions) and the class InterThreadComm which is
 *           the other part of the controller.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JButton;
import javax.swing.JOptionPane;


public class TTTClientControl {
	
	//global variables	
		private final int NUMSQUARES = 9;
		private final int VALID2PLAY = 4;
		private final int MINPORT = 1023;
		private final int MAXPORT = 65536;
		private final String DEFAULTIP = "localhost";
		private final String DEFAULTPORT = "2064";
		
	//a class for the variety of messages that the controller needs to give to the
	//player to let them know about the status of the game
		private ClientMessages pMSG = new ClientMessages();
		
	//variables for establishing connection with a server
		private InetAddress serverIP;
		private int serverPort;
		
	//variables for making the game
		private TTTView view;
		private ServerCommunication communication;
		private LinkedList<String> msgToSend;
		private String name;
		
	//thread communication
		private TTTClientTalker serverTalk;
		private InterthreadComm interComm;
		
		public TTTClientControl() {
			this.view = new TTTView();
			this.communication = new ServerCommunication();
			this.msgToSend = new LinkedList<String>();
			setUpButtonListeners();
			defaultIPnPort();
		}
		
		//starts the game for the player. Deactivate the buttons
		//until they're ready to be used.
		public void runZaProgramu() {
			deactivateButtons();
			this.view.boardFrame.setVisible(false);
			this.view.initScreen.setVisible(true);
		}
		
		
		private void setUpButtonListeners() {
			//this is the listener for the Ok button when the users first input their names
			view.getConnecter().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					getNameAndServer();
				}
			});

	//this adds listeners for all nine buttons of the tictacto board
			for(int i=0; i<NUMSQUARES; i++) {
				view.getSquare(i).addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
	//you have to get the source in order to perform actions on these buttons
	//because the board buttons themselves are not named. They're in an array.
						JButton clickBut = (JButton) event.getSource();
						boardButtonClicked(clickBut);
					}
				});
			}
	//Listener for the quit game button		
			view.getQuit().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					quitGame();
				}
			});
	//listener for the reset game button
			view.getReset().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					resetGame();
				}
			});
	//listener for the new game button
			view.getNewgame().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					newGame();
				}
			});
		}

		
		private void defaultIPnPort() {
			view.setIP(DEFAULTIP);
			view.setPort(DEFAULTPORT);
		}

		//method for deactivating the buttons
		//this really only happens at the end of a game or when the player quits.
		private void deactivateButtons()  {
			for(int i=0; i<NUMSQUARES; i++) {
				view.getSquare(i).setEnabled(false);
			}
			view.getReset().setEnabled(false);
			view.getNewgame().setEnabled(false);
		}

//This function takes in the IP address and the port number the player has supplied
//and checks to see if 1) They are in the correct format 2) the player is able to connect
//to a server with this information
		private void getNameAndServer() {
			String name = view.getName();
			String ip = view.getIP();
			String port = view.getPort();
			
			boolean validity = false;
			
			validity = checkNPI(name, ip, port);
			if(validity) {
				this.name = name;
				try {
					serverIP = InetAddress.getByName(ip);
				} catch(IOException noConn) {
					System.out.println("Cannot connect to this server");
					
				}
				//if everything is valid and you can connect then continue to the main game
				serverPort = Integer.parseInt(port);
				view.initScreen.setVisible(false);
				view.boardFrame.setVisible(true);
				continueToGame();
			} else {
				while (!msgToSend.isEmpty()) {
					JOptionPane.showMessageDialog(this.view.initScreen, msgToSend.remove());
				}
			}
		}
		
//this is the function that sets up the connection and goes on to actually play the game.
//it is only called after checking whether the values the player has given are valid and that
//a connection can be established to the server
		private void continueToGame() {
			
			interComm = new InterthreadComm(name);
			serverTalk = new TTTClientTalker(serverIP, serverPort, interComm, view);
			serverTalk.start();
			
		}
		
		
//This function does 4 checks
//It checks whether the users name, IP address and port are all valid
//It checks whether the IP address given is reachable
//if any of these checks fail the appropriate error message is given
//validness increments to 4 when all the variables are considered worthy
//if it reaches 4 then result becomes true;
		
		private boolean checkNPI(String name, String ip, String port) {
			boolean result = false;
			boolean reachable = false;
			int validness = 0;
			
			String patternN = "(^\\w+$)";
			String patternI = "(^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$)";
			String patternP = "(^\\d{4,5}$)";
			
			Pattern pn = Pattern.compile(patternN);
			Pattern pi = Pattern.compile(patternI);
			Pattern pp = Pattern.compile(patternP);
			Matcher m;
			
			m = pn.matcher(name);
			if(m.find()) {
				validness++;
			} else {
				msgToSend.add(pMSG.PNAMEWRONG);
			}
			
			m = pi.matcher(ip);
			if(ip.equals(DEFAULTIP)) {
				validness++;
			}else {
				if(m.find()) {
					validness++;
				}else {
					msgToSend.add(pMSG.IPWRONG);
				}
			}
			
			m = pp.matcher(port);
			if(m.find()) {
				int porty = Integer.parseInt(port);
				if((porty >= MINPORT) && (porty <= MAXPORT)) {
					validness++;
				} else {
					msgToSend.add(pMSG.PORTWRONG);
				}
			} else {
				msgToSend.add(pMSG.PORTWRONG);
			}
			
			try {
				InetAddress address = InetAddress.getByName(ip);
				reachable = address.isReachable(5000);
			} catch(IOException testConn) {
				System.out.println("Cannot connect to the specified address");
			}
			if(reachable) {
				validness++;
			} else {
				this.msgToSend.add(pMSG.IPCANTCONNECT);
			}
			
			if(validness == VALID2PLAY) {
				result = true;
			}

			return result;
		}

//this is the function for when one of the tictacto buttons get pressed
//it first checks to see if the game has ended. If the game is still going then it checks to
//see if its the players turn and finally it checks to see if the square has already been taken.
		private void boardButtonClicked(JButton clickBut) {
			if(!interComm.getEnd()) {
				if(interComm.getCurrentPiece() == interComm.getMyPiece()) {
					if(!view.isButtonTaken(clickBut)) {
						interComm.addMsgToSend(communication.cMove+clickBut.getName()+pMSG.NLINE);
						view.doMove(clickBut, interComm.getMyPiece());
						interComm.setCurrentPiece(interComm.getOtherPiece());
					} else {
						JOptionPane.showMessageDialog(view.boardFrame, pMSG.INVALIDMOVE);
					}
				} else {
					JOptionPane.showMessageDialog(view.boardFrame, pMSG.NOTTURN);
				}
			} else {
				JOptionPane.showMessageDialog(view.boardFrame, pMSG.GAMEEND);
			}
		}

//function to quit the game. Before quitting the game and shutting down the server
//it sends a message to the server saying it wants to quit which in turn tells the other
//player that their opponent is quitting. If there is no connection the game can shutdown immediately
		private void quitGame() {
			if(interComm.getConnect().isConnected()) {
				interComm.addMsgToSend(communication.cQuit+pMSG.NLINE);
			} else {
				System.exit(0);
			}
		}

//when you click the new game button it sends a 
//message to the other player that you wish to have a new game.
		private void newGame() {
			if(!interComm.getNRCalled(0)) {
				interComm.addMsgToSend(communication.cNew+pMSG.NLINE);
				interComm.setNRCalled(0, true);
			} else {
				JOptionPane.showMessageDialog(view.boardFrame, pMSG.NALREADYPRESSED);
			}
		}

//when the reset button is pressed it sends a message to the other player
//requesting that you reset the game. Meaning that you'll reset the board and the
//victories that each player has. A clean slate.
		private void resetGame() {
			if(!interComm.getNRCalled(1)) {
				interComm.addMsgToSend(communication.cReset+pMSG.NLINE);
				interComm.setNRCalled(1, true);
			} else {
				JOptionPane.showMessageDialog(view.boardFrame, pMSG.RALREADYPRESSED);
			}
		}
		
}