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


/**
 * A GameField is a canvas to display game play and interact with the user.
 *
 * @author			Bryan Nielsen
 */
class GameField extends Canvas {
	GameEngine parent;
	boolean resetField = true;
	
	/**
	 * The GameField constructure sets up the canvas play field and instantiates the listeners
	 * for the user interface.
	 *
	 * @param so		A reference to the parent game engine is required to enable the game field to
	 *									interact with the players in the game.
	 */
	public GameField(GameEngine so) {
		super();
		setSize(400, 400);
		setBackground(new Color(0, 0, 0));
		parent = so;
		addMouseListener(new displayMouseListener(this));
		addMouseMotionListener(
		new displayMouseMotionListener(this));
	}
	
	
	/**
	 * The clearField function sets the flag needed by the canvas paint function to clear the play field.
	 * It then calls repaint to kick off the paint process.
	 */
	public void clearField() {
		resetField = true;
		repaint();
	}
	
	
	public void update(Graphics g) {
		paint(g);
	}
	
	
	public void paint(Graphics g) {
		Dimension s = getSize();
		
		// if field reset flag is set then clear the field
		if (resetField) {
			g.setColor(new Color(0, 0, 0));
			g.fillRect(0, 0, s.width, s.height);
			resetField = false;
		}
		
		// iterate through player list from the game engine
		Iterator<Player> playerIterator = parent.playerList.iterator();
		while (playerIterator.hasNext()) {
			Player next = playerIterator.next();
			if (next != null) next.drawPlayer(g);
		}
	}
}


class displayMouseMotionListener implements MouseMotionListener {
	GameField parent;
	
	public displayMouseMotionListener(GameField so) {
		parent = so;
	}
	
	public void mouseDragged(MouseEvent e) {
		// if mouse has moved significantly from previous location then change coordinates
		if (Math.abs(parent.parent.me.gotoX - e.getX()) > 2 || Math.abs(parent.parent.me.gotoY - e.getY()) > 2) {
			parent.parent.me.gotoX = e.getX();
			parent.parent.me.gotoY = e.getY();
			
			if (e.isControlDown()) parent.parent.me.isVisible = false;
			else parent.parent.me.isVisible = true;
		}
	}
	
	
	public void mouseMoved(MouseEvent e) {}
}


class displayMouseListener implements MouseListener {
	GameField parent;
	
	public displayMouseListener(GameField so) {
		parent = so;
	}
	
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	
	
	public void mousePressed(MouseEvent e) {
		parent.parent.me.mousePressed = true;
		
		parent.parent.me.gotoX = e.getX();
		parent.parent.me.gotoY = e.getY();
		
		if (e.isControlDown()) parent.parent.me.isVisible = false;
		else parent.parent.me.isVisible = true;
	}
	
	
	public void mouseReleased(MouseEvent e) {
		parent.parent.me.mousePressed = false;
	}
}
