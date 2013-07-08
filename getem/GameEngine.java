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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;


/**
 * GameEngine encapsulates all the properties and functions required for the main user
 * interface and the game components.
 *
 * @author			Bryan Nielsen
 */
public class GameEngine extends Frame implements WindowListener {
	// declare class variables
	whoCanvas who;
	MeCanvas meCanvas;
	GameField display;
	Panel gameFieldPanel, whoPanel, buttonsPanel;
	Button start, join;
	String s;
	ArrayList<Player> playerList;
	Player me;
	int maximumPlayers, playerItIndex;
	CommunicationEngine commEngine;
	
	
	// constructor
	public GameEngine() {
		// call parent constructor
		super("getem v0.1");
		
		playerItIndex = -1;
		
		// create player list
		maximumPlayers = 8;
		playerList = new ArrayList<Player>();
		
		// create me player
		me = new Player("Me");
		
		// create communication engine
		commEngine = new CommunicationEngine(this);
		commEngine.start();
		
		// set up frame
		addWindowListener(this);
		setSize(420, 500);
		setResizable(false);
		setLayout(new BorderLayout());
		
		// create panels for frame
		gameFieldPanel = new Panel();
		gameFieldPanel.setSize(400, 400);
		whoPanel = new Panel();
		whoPanel.setSize(400, 20);
		buttonsPanel = new Panel();
		buttonsPanel.setSize(400, 50);
		
		// create elements for panels
		who = new whoCanvas(this);
		meCanvas = new MeCanvas(this);
		display = new GameField(this);
		start = new Button("Start Game");
		start.addActionListener(new buttonAction(this));
		join = new Button("Join Game");
		join.addActionListener(new buttonAction(this));
		s = new String("Get Em");
		
		// add elements to panels
		gameFieldPanel.add(display);
		whoPanel.add(who);
		buttonsPanel.add(meCanvas);
		buttonsPanel.add(start);
		buttonsPanel.add(join);
		
		// add panels to layout
		add("North", whoPanel);
		add("Center", gameFieldPanel);
		add("South", buttonsPanel);
		
		// set frame to visible
		setVisible(true);
	}
	
	
	public void windowClosing(WindowEvent e) {
		// get reference to the closing window
		Window orig = e.getWindow();
		
		// if this is our window event then shut down
		if (orig.equals(this)) {
			commEngine.shutDown();
			this.dispose();
			System.exit(0);
		}
	}
	
	
	
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e){}
	
	
	/**
	 * The addPlayer function is used to add a new player to the list of players. If
	 * a player is successfully added then an instance of the player is returned. If
	 * a player is not added then null is returned.
	 *
	 * @param playerName			A String containing the name of the player. This should be
	 *															a uniquie name. If it is not unique then the add will fail.
	 * @param playerAddress	This is the network address of the player and it also must
	 *															be unique. Attempting to add a user with the same socket 
	 *															address will fail.
	 * @return										An instance of the added player is returned on success. A
	 *															null value is returned on failure.
	 */
	// add a new player to the game engine
	public Player addPlayer(String playerName, InetSocketAddress playerAddress) {
		Iterator<Player> playerIterator;
		
		// check to see if there is an empty slot
		int emptyIndex = 0;
		boolean useEmpty = false;
		playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next == null) {
				// found an empty slot
				useEmpty = true;
				break;
			}
			else emptyIndex += 1;
		}
		
		// make sure we don't exceed maximum (-1 because 0 index is for me)
		if (useEmpty == false && playerList.size() >= maximumPlayers) return null;
		
		// make sure name is available
		playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			
			// if name matches then fail with null
			if (next != null && next.name.equals(playerName.toString())) return null;
		}

		// attempt adding player to list
		Player newPlayer = new Player(playerName);
		if (newPlayer != null) {
			// set player's address
			newPlayer.address = playerAddress;
			
			if (useEmpty) {
				newPlayer.setColorIndex(emptyIndex);
				playerList.set(emptyIndex, newPlayer);
			}
			else {
				newPlayer.setColorIndex(playerList.size());
				playerList.add(newPlayer);
			}
			
			
			return newPlayer;
		}
		
		return null;
	}
	
	
	/**
	 * This resets the game engine's me player to the specified color index value.
	 *
	 * The me player is the local player instance used for player UI interaction. This
	 * player will correspond to one of the players in the player list on the server.
	 *
	 * @param colorIndex				The player color index to be used for this game engines player.
	 */
	public void resetMePlayer(int colorIndex) {
		// set me player settings to defaults
		me.setColorIndex(colorIndex);
		meCanvas.repaint();
		
		// if zero player then assume this is a server and make player zero me
		if (colorIndex == 0) {
			playerList.removeAll(playerList);
			playerList.add(me);
		}
		
		// clear the game play field
		display.clearField();
	}
	
		
	
	/**
	 * The nullPlayer function is used to effectively remove a player from the game.
	 * This is accomplished by locating the player in the player list and setting the
	 * instance to a null value.
	 *
	 * @param playerAddress			The socket address of the player to null out.
	 */
	// null out a player in the list
	public void nullPlayer(InetSocketAddress playerAddress) {
		// iterate through the player list looking for the matching player instance to null
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null && next.address != null && next.address.toString().equals(playerAddress.toString())) {
				// if the it player is nulled then reset the it layer to me
				if (playerList.indexOf(next) == playerItIndex) setPlayerItIndex(0);
				
				playerList.set(playerList.indexOf(next), null);
			}
		}
	}
	
	
	/**
	 * Locate the instance of a player based on the player name.
	 *
	 * @param playerName			The String value of the player name to locate
	 * @return										Returns a Player instance if located, null if not found.
	 */
	public Player getPlayerByName(String playerName) {
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null && next.name.equals(playerName)) return next;
		}
		
		// did not find player
		return null;
	}
	
	
	/**
	 * Locate the instance of a player based on the player socket address
	 *
	 * @param playerAddress			The socket address of the player to locate.
	 * @return												Returns a Player instance if located, null if not found.
	 */
	public Player getPlayerByAddress(InetSocketAddress playerAddress) {
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null && next.address != null && next.address.toString().equals(playerAddress.toString())) return next;
		}
		
		// did not find player
		return null;
	}
	
	
	/**
	 * Determine the index value in the player list for the specified player socket address
	 *
	 * @param playerAddress		The socket address of the player to locate.
	 * @return											The integer index value of the player within the player list or -1 if not found.
	 */
	public int getPlayerIndexByAddress(InetSocketAddress playerAddress) {
		int playerIndex = 0;
		
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null && next.address != null && next.address.toString().equals(playerAddress.toString())) return playerIndex;
			playerIndex += 1;
		}
		
		// did not find player
		return -1;
	}
	
	
	/**
	 * Determine the index value in the player list for the specified player name.
	 *
	 * @param playerName			The String value of the player name to locate.
	 * @return										The integer index value of the player within the player list or -1 if not found.
	 */
	public int getPlayerIndexByName(String playerName) {
		int playerIndex = 0;
		
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null && next.name.equals(playerName)) return playerIndex;
			playerIndex += 1;
		}
		
		// did not find player
		return -1;
	}
	
	
	/**
	 * The applyPlayerMap function is used to apply a hash map of player values to
	 * the player in the player list specified by an index value.
	 *
	 * @param playerIndex				The index value within the player list for the player to apply the map to.
	 * @param playerMap						The hash map of player values to apply to the player.
	 */
	public void applyPlayerMap(int playerIndex, Map<String, Object> playerMap) {
		playerList.get(playerIndex).applyMap(playerMap);
	}
	
	
	/**
	 * The applyPlayerMaps function assumes a list of player hash map values need to be applied
	 * to the player list.
	 *
	 * @param playerMaps				An array of hash maps with player values to be applied to the player list.
	 */
	public void applyPlayerMaps(ArrayList<Map<String, Object>> playerMaps) {
		Player newPlayer;
		
		// iterate through the list of player maps to apply
		Iterator<Map<String, Object>> mapsIterator = playerMaps.iterator();
		int mapIndex = 0;
		while (mapsIterator.hasNext()) {
			Map<String, Object> next = mapsIterator.next();
			
			// if index out of range then create a new player for this map
			if (mapIndex + 1 > playerList.size()) {
				newPlayer = new Player("new");
				newPlayer.applyMap(next);
				playerList.add(newPlayer);
			}
			else {
				// if map is null then set player list to null
				if (next == null) {
					playerList.set(mapIndex, null);
				}
				
				// else if player list is null then create new player
				else if (playerList.get(mapIndex) == null) {
					newPlayer = new Player("new");
					newPlayer.applyMap(next);
					playerList.set(mapIndex, newPlayer);
				}
				
				// else apply map
				else {
					playerList.get(mapIndex).applyMap(next);
				}
			}
			
			mapIndex += 1;
		}
		
		// repaint the play field
		display.repaint();
	}
	
	
	/**
	 * The setPlayerItIndex function is used to set a player in the game as being it.
	 *
	 * @param playerIndex				The index value within the player list of the player to assign as it.
	 */
	public void setPlayerItIndex(int playerIndex) {
		if (playerIndex != playerItIndex) {
			// the it index has changed
			playerItIndex = playerIndex;
			resetAllPlayers();
			who.repaint();
		}
	}
	
	
	/**
	 * Get the Color object for a player within the player list.
	 *
	 * @parm playerIndex				The index value within the player list of the player to query for a Color.
	 */
	public Color getPlayerColor(int playerIndex) {
		return Player.playerColors[playerIndex];
	}
	
	
	/**
	 * Get a player's name from the player list.
	 *
	 * @param playerIndex				The index value within the player list of the player to query for a name.
	 */
	public String getPlayerName(int playerIndex) {
		return playerList.get(playerIndex).name;
	}
	
	
	/**
	 * The calculatePlayerMoves function is called at some interval to move the players in the player list.
	 */
	public void calculatePlayerMoves() {
		// if we are running as server then calculate other players
		if (commEngine.isServer()) {
			Iterator<Player> playerIterator = playerList.iterator();
			while (playerIterator.hasNext()) {
				Player next = playerIterator.next();
				
				if (next != null) {
					next.calculateMove();
				}
			}
		}
		
		// redraw display after move
		display.repaint();
	}
	
	
	/**
	 * The player tagged function is called after player movement to determine the index value of the
	 * currently it player. The index value will be a new value if a player manages to catch and tag the
	 * it player or it will be the current it value if no players manage to tag the it player.
	 *
	 * @return			The index value within the player list of the it player will be returned. This will
	 *								be a new value if a player tags the it player or it will be the current it player's
	 *								index value.
	 */
	public int playerTagged() {
		int playerIndex = 0;
		
		// get coordinates of it player
		if (playerItIndex < playerList.size()) {
			Player itPlayer = playerList.get(playerItIndex);
			
			if (itPlayer != null) {
				Iterator<Player> playerIterator = playerList.iterator();
				while (playerIterator.hasNext()) {
					Player next = playerIterator.next();
					
					if (playerIndex != playerItIndex && next != null && next.playerCollision(itPlayer.currentX, itPlayer.currentY)) return playerIndex;
					
					playerIndex += 1;
				}
			}
		}
		
		// no players have tagged it player
		return playerItIndex;
	}
	
	
	/**
	 * All players will be reset to their default coordinates based on their color index and the play field is cleared.
	 */
	public void resetAllPlayers() {
		Iterator<Player> playerIterator = playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null) next.setColorIndex(next.colorIndex);		
		}
		
		display.clearField();
	}
	
	
	
	
	/**
	 * Start the game engine.
	 */
	public static void main(String[] args) {
		GameEngine ge = new GameEngine();
	}
}



// action listener for game engine buttons
class buttonAction implements ActionListener {
	GameEngine parent;
	
	public buttonAction(GameEngine so) {
		parent = so;
	}
	
	public void actionPerformed(ActionEvent e) {
		String s = new String(e.getActionCommand());
		
		if (s.equals("Start Game")) {
			// set comm engine to server
			parent.commEngine.startServer();
		}
		
		if(s.equals("Join Game")) {
			hostDialog d = new hostDialog(parent);
			
			d.setVisible(true);
		}
	}
}



// join host dialog
class hostDialog extends Dialog {
	GameEngine getemEngine;
	Panel p;
	Button ok, cancel;
	TextField host, name;
	
	public hostDialog(GameEngine ge) {
		super((Frame) ge, "Enter Server Address", true);
		getemEngine = ge;
		
		setLocation(getemEngine.getLocation());
		
		cancel = new Button("Cancel");
		cancel.addActionListener(new hostAction(this));
		ok = new Button("Ok");
		ok.addActionListener(new hostAction(this));
		host = new TextField(40);
		host.addActionListener(new hostAction(this));
		name = new TextField(40);
		p = new Panel();
		
		p.add(new Label("Server: "));
		p.add(host);
		p.add(new Label("Name: "));
		p.add(name);
		p.add(ok);
		p.add(cancel);
		
		add(p);
		pack();
	}
}



// action listener for the host dialog
class hostAction implements ActionListener {
	hostDialog parent;
	//GameEngine getemEngine;
	
	public hostAction(hostDialog so) {
		parent = so;
		//getemEngine = parent.parent;
	}
	
	public void actionPerformed(ActionEvent e) {
		String s = new String(e.getActionCommand());
		
		if (s.equals("Cancel")) parent.dispose();
		else {
			s = new String(parent.host.getText());
			parent.getemEngine.commEngine.joinServer(s, parent.name.getText());
			parent.dispose();
		}
	}
}


class whoCanvas extends Canvas {
	GameEngine parent;
	
	public whoCanvas(GameEngine so) {
		super();
		setSize(400, 20);
		setBackground(new Color(0, 0, 0));
		parent = so;
	}
	
	public void update(Graphics g) {
		paint(g);
	}
	
	public void paint(Graphics g) {
		Dimension s = getSize();
		FontMetrics fm = g.getFontMetrics();
		
		//g.setColor(parent.cArray[parent.it]);
		if (parent.playerItIndex == -1) g.setColor(new Color(0, 0, 0));
		else g.setColor(Player.playerColors[parent.playerItIndex]);
		g.fillRect(0, 0, s.width, s.height);
		
		g.setColor(new Color(0, 0, 0));
		g.drawString(parent.s, (s.width / 2) - (fm.stringWidth(parent.s) / 2), 15);
	}
	
}


class MeCanvas extends Canvas {
GameEngine parent;
	
	public MeCanvas(GameEngine ge) {
		super();
		setSize(150, 20);
		setBackground(new Color(0, 0, 0));
		parent = ge;
	}
	
	public void update(Graphics g) {
		paint(g);
	}
	
	public void paint(Graphics g) {
		Dimension canvasDim = getSize();
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(parent.getPlayerColor(parent.me.colorIndex));
		g.fillRect(0, 0, canvasDim.width, canvasDim.height);
		
		String playerName = parent.me.name;
		g.setColor(new Color(0, 0, 0));
		g.drawString(playerName, (canvasDim.width / 2) - (fm.stringWidth(playerName) / 2), 15);
	}
}

