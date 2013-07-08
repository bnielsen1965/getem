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

import java.util.*;
import java.awt.*;
import java.net.*;
import java.io.*;


/**
 * Player encapsulates all the properties and functions required for a game player.
 *
 * @author			Bryan Nielsen
 */
class Player {
	// declare class variables
	static Color[] playerColors = new Color[] {
		new Color(255, 0, 0),
		new Color(0, 255, 0),
		new Color(0, 0, 255),
		new Color(255, 255, 0),
		new Color(0, 255, 255),
		new Color(255, 0, 255),
		new Color(128, 64, 0),
		new Color(0, 128, 64),
		new Color(128, 0, 192),
		new Color(255, 192, 128),
		new Color(128, 128, 128),
		new Color(64, 192, 64),
		new Color(128, 0, 0),
		new Color(0, 0, 128),
		new Color(255, 255, 255),
		new Color(128, 128, 0)
	};
	
	String name;
	boolean isIt, isVisible, mousePressed, isConnected;
	int playerIndex, colorIndex, gotoX, gotoY, playerRadius, moveIncrement;
	float currentX, currentY;
	InetSocketAddress address;
	Date lastCommunicationDate;
	
	
	/**
	 * Constructor creates an instance of Player with the specified playername
	 * and defaults the player settings to the first color index.
	 *
	 * @param playerName		String containing the name for this player
	 */
	public Player(String playerName) {
		// set defaults
		name = playerName;
		isIt = false;
		isVisible = true;
		mousePressed = false;
		isConnected = true;
		colorIndex = 0;
		gotoX = 0;
		gotoY = 0;
		currentX = 0;
		currentY = 0;
		address = null;
		lastCommunicationDate = new Date();
		playerRadius = 10;
		moveIncrement = 5;
		
		// assume player 0
		setColorIndex(0);
	}
	
	
	/**
	 * Sets the color index value to use with this player and sets other player variables to
	 * the default values to be used with the specified color index.
	 *
	 * @param newColorIndex			The index value of the color from the class playerColors array to
	 *																	use for this player.
	 */
	public void setColorIndex(int newColorIndex) {
		// if color index exceeds possible values then default to 0
		if (newColorIndex >= playerColors.length) newColorIndex = 0;
		
		colorIndex = newColorIndex;
		
		// set play field coordinates
		currentX = 16 + colorIndex * 23;
		currentY = currentX;
		
		gotoX = 0;
		gotoY = 0;
		isIt = false;
		isVisible = true;
		mousePressed = false;
	}
	
	
	/**
	 * Returns the Color object for this player.
	 *
	 * @return			The Color object for this player is returned.
	 */
	public Color getColor() {
		return playerColors[colorIndex];
	}
	
	
	/**
	 * A hash map is used to collate details about a player instance when
	 * communicating with the game server or other game clients. The type
	 * of map requested determines which player parameters will be included
	 * in the map.
	 *
	 * The type of maps available include "current" which provides the current
	 * player coordinates along with other player details, and "goto" which 
	 * provides the target coordinates where the player movement will be directed
	 * along with additional player details.
	 *
	 * @param mapType			The type of map to create and return. This determines
	 *													which player values will be included in the hash map.
	 * @return								After collating the player values into a hash map the
	 *													map is returned.
	 */
	public Map<String, Object> getPlayerMap(String mapType) {
		Map<String, Object> playerMap = new HashMap<>();
		playerMap.put("name", name);
		playerMap.put("colorIndex", colorIndex);
		playerMap.put("mousePressed", mousePressed);
		playerMap.put("isVisible", isVisible);
		
		if (mapType.equals("current")) {
			playerMap.put("currentX", currentX);
			playerMap.put("currentY", currentY);
		}
		else if (mapType.equals("goto")) {
			playerMap.put("gotoX", gotoX);
			playerMap.put("gotoY", gotoY);
		}
		
		return playerMap;
	}
	
	
	/**
	 * The applyMap function is used to apply values passed in a hash map to this player. This assists
	 * in updating the server and clients with player details by utilizing a player frame encapsulated in 
	 * the hash map that can be passed between clients and the server.
	 *
	 * @param playerMap				The hash map with the player values to be used in this player are provided
	 *															in the passed player map.
	 */
	public void applyMap(Map<String, Object> playerMap) {
			if (playerMap.containsKey("name")) name = (String)playerMap.get("name");
			if (playerMap.containsKey("colorIndex")) colorIndex = ((Double)playerMap.get("colorIndex")).intValue();
			if (playerMap.containsKey("currentX")) currentX = ((Double)playerMap.get("currentX")).floatValue();
			if (playerMap.containsKey("currentY")) currentY = ((Double)playerMap.get("currentY")).floatValue();
			if (playerMap.containsKey("gotoX")) gotoX = ((Double)playerMap.get("gotoX")).intValue();
			if (playerMap.containsKey("gotoY")) gotoY = ((Double)playerMap.get("gotoY")).intValue();
			if (playerMap.containsKey("mousePressed")) mousePressed = (boolean)playerMap.get("mousePressed");
			if (playerMap.containsKey("isVisible")) isVisible = (boolean)playerMap.get("isVisible");
	}
	
	
	/**
	 * The calculateMove function is called by the game server to reposition this player's coordinates.
	 *
	 * Note that the current x and y coordinates are stored as floating point values to even though the
	 * pixel coordinates will be integers. This method is used to enable smooth multi-directional player
	 * movement.
	 *
	 * The boundaries of player movement in the play field are currently hard coded in this function. 
	 * Any changes to the size of the play field in the user interface will require coding changes here
	 * as well. This is obviously something that needs to be changed in the future.
	 */
	// calculate player move
	public void calculateMove() {
		if (mousePressed) {
			// calculate distance to goto coordinates
			int distance = (int)Math.sqrt((double)Math.pow((double)(gotoX - currentX), (double)2) + Math.pow((double)(gotoY - currentY), (double)2));
			
			// if distance is 1 pixel or more then move
			if (distance > 0) {
				currentX += moveIncrement * (gotoX - currentX) / distance;
				
				// border limits ****Need to come up with something better than hard coded values
				if (currentX < 0) currentX = 0;
				if (currentX > 400 - playerRadius) currentX = 400 - playerRadius;
				
				currentY += moveIncrement * (gotoY - currentY) / distance;
				
				// border limits ****Need to come up with something better than hard coded values
				if (currentY < 0) currentY = 0;
				if (currentY > 400 - playerRadius) currentY = 400 - playerRadius;
			}
		}
	}
	
	
	/**
	 * Player collisions are determined by an overlap of the player's sprite, in this case the 
	 * radius of the player's circle.
	 *
	 * @param playerX				The X coordinate of the other player to check against for a collision.
	 * @param playerY				The Y coordinate of the other player.
	 * @return									A boolean is returned denoting if a collision has taken place.
	 */
	// determine if collision with the given coordinates
	public boolean playerCollision(double playerX, double playerY) {
		// calculate distance to point
		int distance = (int)Math.sqrt((double)Math.pow((playerX - currentX), (double)2) + Math.pow((playerY - currentY), (double)2));
		
		if (distance < playerRadius) return true;
		else return false;
	}
	
	
	/**
	 * Draw the player's sprite on the provided graphic. The sprite will be a circle with the radius defined
	 * in the Player class.
	 *
	 * @parameter g				The Graphic object to draw on.
	 */
	// draw player on graphic
	public void drawPlayer(Graphics g) {
		if (isVisible) g.setColor(getColor());
		else g.setColor(new Color(0, 0, 0));
		
		//g.fillRect((int)next.currentX, (int)next.currentY, 5, 5);
		g.fillOval((int)currentX + playerRadius, (int)currentY + playerRadius, playerRadius, playerRadius);

	}

}