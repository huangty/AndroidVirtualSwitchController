package edu.stanford.avsc;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class OFMessageEvent {
	public OpenflowSwitchControlChannel server;
	public SocketChannel socket;
	public byte[] data;
	public ByteBuffer bb;
	
	OFMessageEvent(OpenflowSwitchControlChannel server, SocketChannel socket, byte[] data) {
		this.server = server;
		this.socket = socket;
		this.data = data;
		bb = ByteBuffer.allocate(data.length);
		bb.put(data);
		bb.flip();
	}
}
