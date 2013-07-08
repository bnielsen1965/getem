/*
* Copyright (C) 2013 Bryan Nielsen - All Rights Reserved
*
* Author: Bryan Nielsen (bnielsen1965@gmail.com)
*
*
*/

/*
This file is part of GetEm.

GetEm is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GetEm is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GetEm.  If not, see <http://www.gnu.org/licenses/>.
*/

package getem;

import java.net.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;


/**
 * The CommunicationEngine is a Thread class used to encapsulate the variables and functions
 * used for game engine communication between clients and the server.
 *
 * @author			Bryan Nielsen
 */
class CommunicationEngine extends Thread {
	// declare variables
	int socketRequest, serverPort, timerDelayMS;
	SocketStates socketState;
	GameEngine getemEngine;
	GameMessage getemMessage;
	DatagramSocket communicationSocket;
	DatagramPacket receivePacket, sendPacket;
	String joinHost, joinPlayerName;
	InetAddress serverAddress;
	InetSocketAddress serverSocketAddress;
	Gson gson;
	
	
	// enumerate possible socket states
	public enum SocketStates {
		SERVER_OPEN, JOIN_REQUEST, JOIN_OPEN, DISCONNECT, UNKNOWN;
	}
	
	
	/**
	 * The communication engine constructor requires a reference to the game engine
	 * creating this instance so it can react with the game engine players and components.
	 *
	 * @param ge				A reference to the parent game engine.
	 */
  public CommunicationEngine(GameEngine ge) {
  	// set variables
  	getemEngine = ge;
  	socketState = SocketStates.DISCONNECT;
  	socketRequest = 0;
  	serverPort = 3000;
  	timerDelayMS = 100;
  	
  	gson = new Gson();
  	
	}
	
	
	/**
	 * The run function for the Thread will run continuously and process all socket communication
	 * functions. The timing of this thread also determines the game play and refresh frequency.
	 */
	public void run() {
		while(true) {
			// loop delay
			try {
				Thread.sleep(timerDelayMS);
			}
			catch(InterruptedException e) {}
			
			
			// socket request actions
			switch(socketRequest) {
				// open server socket request
				case 1:
				// if socket open then close socket
				if (socketState != SocketStates.DISCONNECT) closeSocket();
				
				// open socket
				try {
					communicationSocket = new DatagramSocket(serverPort);
					communicationSocket.setSoTimeout(5);
					socketState = SocketStates.SERVER_OPEN;
					System.out.println("Server socket opened");
					
					// set player 0 color index to 0
					getemEngine.resetMePlayer(0);
					
					// repaint the MeCanvas to show new color
					getemEngine.meCanvas.repaint();
					
					getemEngine.playerItIndex = 0;
					getemEngine.who.repaint();
				}
				catch (SocketException se) {
					System.out.println("Socket Exception: " + se);
				}
				break;
				
				
				// open socket to join game
				case 2:
				// if socket open then close socket
				if (socketState != SocketStates.DISCONNECT) closeSocket();
				
				// open socket
				try {
					communicationSocket = new DatagramSocket();
					communicationSocket.setSoTimeout(5);
					socketState = SocketStates.JOIN_REQUEST;
					System.out.println("Client socket opened");
					
					// send join request
					getemMessage = new GameMessage("JOIN");
					getemMessage.payload.put("name", joinPlayerName);
					sendMessage(getemMessage, serverSocketAddress);
				}
				catch (SocketException se) {
					System.out.println("Socket Exception: " + se);
				}
				break;
			} // end of socket request switch
			
			// reset request
			socketRequest = 0;
			
			
			// if socket is open then read any incoming packets
			if (socketState != SocketStates.DISCONNECT) readSocketPackets();
			
			
			// server outgoing message processing
			if (socketState == SocketStates.SERVER_OPEN) {
				// move players in the game engine
				getemEngine.calculatePlayerMoves();
				
				// check if a new player is tagged
				int newPlayerItIndex = getemEngine.playerTagged();
								
				// build a game message frame for all players
				getemMessage = new GameMessage("PLAYERS_FRAME");
				ArrayList<Map<String, Object>> playerMaps = new ArrayList<Map<String, Object>>();
				
				// iterate through players adding their hash map to the a list of player maps
				Iterator<Player> playerIterator = getemEngine.playerList.iterator();
				while (playerIterator.hasNext()) {
					Player next = playerIterator.next();
					
					if (next == null) playerMaps.add(null);
					else playerMaps.add(next.getPlayerMap("current"));
				}
				
				// load player hash maps into the message payload
				getemMessage.payload = new HashMap<>();
				getemMessage.payload.put("playerMaps", playerMaps);
				getemMessage.payload.put("playerItIndex", newPlayerItIndex);
				
				// send message to clients
				playerIterator = getemEngine.playerList.iterator();
				while (playerIterator.hasNext()) {
					Player next = playerIterator.next();
					
					if (next != null && next.address != null) {
						sendMessage(getemMessage, next.address);
					}
				}
				
				// if it player has changed then update on server
				if (newPlayerItIndex != getemEngine.playerItIndex) getemEngine.setPlayerItIndex(newPlayerItIndex);
				
				// repaint game field
				getemEngine.display.repaint();
			}
			
			// else if client then send me map update
			else if (socketState == SocketStates.JOIN_OPEN) {
				getemMessage = new GameMessage("PLAYER_FRAME");
				
				// fill payload with player map
				getemMessage.payload = getemEngine.me.getPlayerMap("goto");
							
				// add the map type
				getemMessage.payload.put("mapType", "frame");
				
				// send frame to server
				sendMessage(getemMessage, serverSocketAddress);
			}
		} // end of run while loop
	} // end of Thread run function
	
	
	// read packets
	private void readSocketPackets() {
		InetSocketAddress clientAddress;
		
		if (socketState != SocketStates.DISCONNECT) {
			// loop to read all packets
			while (true) {
				try {
					receivePacket = new DatagramPacket(new byte[1024], 1024);
					communicationSocket.receive(receivePacket);
					
					// get packet data as string and trim to avoid JSON exception
					String s = new String(receivePacket.getData());
					s = s.trim();
					
					// convert JSON to game message
					getemMessage = gson.fromJson(s, GameMessage.class);
					
					switch (getemMessage.getMessageType()) {
						// received a join request message
						case JOIN:
						// assign slot
						Player newPlayer = getemEngine.addPlayer(
							(String)getemMessage.payload.get("name"), 
							new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort())
						);
						
						if (newPlayer == null) {
							// join failed
							getemMessage = new GameMessage(GameMessage.MessageTypes.JOIN_REJECT);
						}
						else {
							// send accept message
							getemMessage = new GameMessage(GameMessage.MessageTypes.JOIN_ACCEPT);
						
							// fill payload with new player info
							getemMessage.payload = newPlayer.getPlayerMap("current");
							
							// add the map type
							getemMessage.payload.put("mapType", "join");
						}
						
						// send message to client
						clientAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
						sendMessage(getemMessage, clientAddress);
						break;
						
						
						// join request accepted
						case JOIN_ACCEPT:
						// remove an pre-existing player list
						getemEngine.playerList.removeAll(getemEngine.playerList);
						
						// apply the map in the message payload to player 0
						getemEngine.me.applyMap(getemMessage.payload);
						
						// redraw the MeCanvas to update name and color
						getemEngine.meCanvas.repaint();
						
						getemEngine.display.clearField();
						
						// socket state set to game joined
						socketState = SocketStates.JOIN_OPEN;
						break;
						
						
						// player frame
						case PLAYER_FRAME:
						// apply map in frame to player with same address
						clientAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
						int playerIndex = getemEngine.getPlayerIndexByAddress(clientAddress);
						
						if (playerIndex > 0) {
							getemEngine.applyPlayerMap(playerIndex, getemMessage.payload);
						}
						break;
						
						
						// players frame
						case PLAYERS_FRAME:
						getemEngine.applyPlayerMaps((ArrayList<Map<String, Object>>)getemMessage.payload.get("playerMaps"));
						
						if (getemMessage.payload.containsKey("playerItIndex")) 
							getemEngine.setPlayerItIndex(((Double)getemMessage.payload.get("playerItIndex")).intValue());
						break;
						
						
						// disconnect
						case DISCONNECT:
						// if client socket open then server disconnected
						if (socketState == SocketStates.JOIN_OPEN) {
							// close our connection
							closeSocket();
							
							// reset player
							getemEngine.resetMePlayer(0);
						}
						// if server socket open then a client disconnected
						else if (socketState == SocketStates.SERVER_OPEN) {
							clientAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
							getemEngine.nullPlayer(clientAddress);
						}
						
						break;
						
						
						// ping packet
						case PING:
						clientAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
						
						// build a game message frame for all players
						getemMessage = new GameMessage("PING_RESPONSE");
						ArrayList<Map<String, Object>> playerMaps = new ArrayList<Map<String, Object>>();
						
						Iterator<Player> playerIterator = getemEngine.playerList.iterator();
						while (playerIterator.hasNext()) {
							Player next = playerIterator.next();
							
							if (next == null) playerMaps.add(null);
							else playerMaps.add(next.getPlayerMap("current"));
						}
						
						getemMessage.payload = new HashMap<>();
						getemMessage.payload.put("playerMaps", playerMaps);
						getemMessage.payload.put("playerItIndex", getemEngine.playerItIndex);
						
						// send response
						sendMessage(getemMessage, clientAddress);
						break;
						
					}
				}
				catch (IOException ioe) {
					// nothing left to read
					
					return;
				}
			}
		}
	}
	
	
	/**
	 * The send message function will prepare the provided message instance for transmission
	 * and send it through the socket. The message is converted to a JSON formatted string so 
	 * it can easily be used by alternative clients and message consumers.
	 *
	 * @param msg				A game message ready to be sent.
	 * @param sockAdd	The socket address where the message should be sent.
	 */
	private void sendMessage(GameMessage msg, InetSocketAddress sockAdd) {
		try {
			// convert message to a JSON string
			String s = gson.toJson(msg);
			
			// create datagram packet to send
			sendPacket = new DatagramPacket(s.getBytes(), s.length(), sockAdd);
			
			// send the packet
			sendSocketPacket(sendPacket);
		}
		catch (SocketException se) {
			System.out.println("Socket Exception: " + se);
		}
	}


	/**
	 * The sendSocketPacket is a general datagram packet send function used
	 * for all communications.
	 *
	 * @param sendDP				The datagram packet to send over the communication socket.
	 */
	private void sendSocketPacket(DatagramPacket sendDP) {
		if (socketState != SocketStates.DISCONNECT) {
			try {
				communicationSocket.send(sendDP);
			}
			catch (IOException ioe) {
				System.out.println("IO Exception: " + ioe);
			}
		}
	}
	
	
	/**
	 * The close socket function will handle any required pre-closing messages and
	 * set communication states as needed before closing the communication socket.
	 */
	private void closeSocket() {
		// close socket if open
		if (socketState != SocketStates.DISCONNECT) {
			// create a disconnect message
			getemMessage = new GameMessage("DISCONNECT");
			
			// if client then send to server
			if (socketState == SocketStates.JOIN_OPEN) {
				sendMessage(getemMessage, serverSocketAddress);
			}
			
			// if server then send to all clients
			if (socketState == SocketStates.SERVER_OPEN) {
				// iterate through player list
				Iterator<Player> playerIterator = getemEngine.playerList.iterator();
				while (playerIterator.hasNext()) {
					Player next = playerIterator.next();
					if (next != null && next.address != null) {
						sendMessage(getemMessage, next.address);
					}
				}
			}

			socketState = SocketStates.DISCONNECT;
			communicationSocket.close();
			System.out.println("Socket closed");
		}
	}
	
	
	
	/**
	 * The startServer function is used by the game engine interface to request the
	 * communiation thread set up the socket as a server.
	 */
	public void startServer() {
		socketRequest = 1;
	}
	
	
	/**
	 * A helper function that can be used to inform other functions if the communication thread
	 * is running as a server.
	 *
	 * @return					A boolean is returned noting true if this is a server.
	 */
	public boolean isServer() {
		if (socketState ==	SocketStates.SERVER_OPEN) return true;
		else return false;
	}
	
	
	/**
	 * The joinServer function is used by the game engine interface to request that the communication
	 * thread connect to a game server as a client.
	 *
	 * @param serverName				The host name or IP address of the game server.
	 * @param playerName				The name to use for this player when they connect.
	 */
	public void joinServer(String serverName, String playerName) {
		joinHost = serverName;
		joinPlayerName = playerName;
		
		// try to create server socket address
		try {
			// look up server name
			serverAddress = InetAddress.getByName(serverName);
			
			// build socket address from lookup and server port
			serverSocketAddress = new InetSocketAddress(serverAddress, serverPort);
			
			// set message to join server
			socketRequest = 2;
		}
		catch (UnknownHostException uhe) {
			System.out.println("Host Exception: " + uhe);
		}
	}
	
	
	// shut down communication engine
	public void shutDown() {
		closeSocket();
	}
}