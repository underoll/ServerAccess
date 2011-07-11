/*
 * Copyright (C) 2005-2011 NAUMEN. All rights reserved.
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 2 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 */
package ru.naumen.servacc;

import java.net.InetAddress;
import java.net.ServerSocket;

public class SocketReserver {
	public static ServerSocket createListener(String host, int port) throws Exception {
		return new ServerSocket(port, 0, InetAddress.getByName(host));
	}

	private static int PORTBASE = 12000;
	private static int PORTMAX = 13000;
	private static int port = PORTBASE;

	public static ServerSocket createListener(String host) throws Exception {
		int portstart = port;
		while (true)
			try {
				return createListener(host, ++port);
			} catch (Exception e) {
				if (port > PORTMAX)
					port = PORTBASE;
				if (port == portstart)
					throw e;
			}
	}

	public static int getFreePort() throws Exception {
		ServerSocket sock = SocketReserver.createListener("0.0.0.0");
		int port = sock.getLocalPort();
		sock.close();
		return port;
	}
}
