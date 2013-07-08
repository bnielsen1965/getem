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


/**
 * GameMessage encapsulates all the properties and functions required for a game message
 * to be passed between the clients and the server.
 *
 * @author			Bryan Nielsen
 */
class GameMessage {
	MessageTypes messageType;
	Map<String, Object> payload;
	
	// enumerate the message types
	public enum MessageTypes {
		JOIN, JOIN_REJECT, JOIN_ACCEPT, DISCONNECT, PLAYER_FRAME, PLAYERS_FRAME, PING, PING_RESPONSE, UNKNOWN;
	}
	
	
	/**
	 * The constructor initializes a new game message of the type specified in a String.
	 *
	 * @param msgType			The name of the message type to create.
	 */
	public GameMessage(String msgType) {
		messageType = MessageTypes.valueOf(msgType);
		payload = new HashMap<>();
	}
	
	
	/**
	 * The constructor initializes a new game message of the type specified in the enumerated value.
	 *
	 * @param msgType			The name of the message type to create.
	 */
	public GameMessage(MessageTypes msgType) {
		messageType = msgType;
		payload = new HashMap<>();
	}
	
	
	/**
	 * Return the enumerated type of this message.
	 *
	 * @return				The enumerated message type.
	 */
	public MessageTypes getMessageType() {
		return messageType;
	}
	
}