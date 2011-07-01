/*
 * synergy -- mouse and keyboard sharing utility
 * Copyright (C) 2010 Shaun Patterson
 * Copyright (C) 2010 The Synergy Project
 * Copyright (C) 2009 The Synergy+ Project
 * Copyright (C) 2002 Chris Schoeneman
 * 
 * This package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * found in the file COPYING that should have accompanied this file.
 * 
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.synergy.io.msgs;

import java.io.DataInputStream;
import java.io.IOException;

public class EnterMessage extends Message {
	public static final MessageType MESSAGE_TYPE = MessageType.CENTER;
	
	private short x;
	private short y;
	private int sequenceNumber;
	private short mask;
	
	public EnterMessage (MessageHeader header, DataInputStream din) throws IOException {
		super (header);
		
		x = din.readShort ();
		y = din.readShort ();
		sequenceNumber = din.readInt ();
		mask = din.readShort ();
	}
	
	public short getX () {
		return x;
	}

	public short getY () {
		return y;
	}

	public int getSequenceNumber () {
		return sequenceNumber;
	}

	public short getMask () {
		return mask;
	}

	public String toString () {
		return "EnterMessage:(" + x + "," + y + "):" + sequenceNumber + ":" + mask;  
	}
	
	
}
