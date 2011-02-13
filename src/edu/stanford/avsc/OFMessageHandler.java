package edu.stanford.avsc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;

import android.util.Log;

public class OFMessageHandler implements Runnable{

	private List msgQueue = new LinkedList();
	  
	public void processData(OpenflowSwitchControlChannel server, SocketChannel socket, byte[] data, int count) {
	    byte[] dataCopy = new byte[count];
	    System.arraycopy(data, 0, dataCopy, 0, count);
	    synchronized(msgQueue) {
	    	msgQueue.add(new OFMessageEvent(server, socket, dataCopy));
	    	msgQueue.notify();
	    }
	}
	
	public void run() {
		OFMessageEvent msgEvent;	    
	    while(true) {
	    	// Wait for data to become available
	    	synchronized(msgQueue) {
	    		while(msgQueue.isEmpty()) {
	    			try {
	    				msgQueue.wait();
	    			} catch (InterruptedException e) {
	    			}
	    		}
	    		msgEvent = (OFMessageEvent) msgQueue.remove(0);
	    	}	      
	    	Log.d("AVSC", "Received msg with size = "+msgEvent.data.length);
	    	OFMessage ofm = new OFMessage();
	    	Log.d("AVSC", "contnet of data = " + msgEvent.bb.toString());
	    	ofm.readFrom(msgEvent.bb);
	    	
	    	if(ofm.getType() == OFType.HELLO){
	    		Log.d("AVSC", "Received OFPT_HELLO");
	    		msgEvent.server.sendReportToUI("Received OFPT_HELLO");
	    		OFHello ofh = new OFHello();
				ByteBuffer bb = ByteBuffer.allocate(ofh.getLength());
				ofh.writeTo(bb);
				msgEvent.server.send(msgEvent.socket, bb.array());	
				OFFeaturesRequest offr = new OFFeaturesRequest();
				bb = ByteBuffer.allocate(offr.getLength());
				offr.writeTo(bb);
				msgEvent.server.send(msgEvent.socket, bb.array());
				msgEvent.server.sendReportToUI("Switch Connected");
	    	}else if(ofm.getType() == OFType.ECHO_REQUEST){
	    		Log.d("AVSC", "Received OFPT_ECHO_REQUEST");
	    		//msgEvent.server.sendReportToUI("Received OFPT_ECHO_REQUEST");
	    		OFEchoReply reply = new OFEchoReply();
				ByteBuffer bb = ByteBuffer.allocate(reply.getLength());
				reply.writeTo(bb);
				msgEvent.server.send(msgEvent.socket, bb.array());
				msgEvent.server.insertFixRule(msgEvent.socket);
	    	}else if(ofm.getType() == OFType.PACKET_IN){
	    		Log.d("AVSC", "Received PACKET_IN");
	    		msgEvent.server.sendReportToUI("Received PACKET_IN");
	    		OFPacketIn ofp_in = new OFPacketIn();
	    		ofp_in.readFrom(ByteBuffer.wrap(msgEvent.data));
	    		//msgEvent.server.sendReportToUI(ofp_in.toString());
	    	}else if(ofm.getType() == OFType.FEATURES_REPLY){
	    		Log.d("AVSC", "Received Switch Feature Reply");
	    		msgEvent.server.sendReportToUI("Received Switch Feature Reply");
	    		OFFeaturesReply offr = new OFFeaturesReply();	    		
	    		offr.readFrom(ByteBuffer.wrap(msgEvent.data));	    			    		
	    		msgEvent.server.sendReportToUI("Switch("+offr.getDatapathId()+"): port size = "+ offr.getPorts().size());
	    		msgEvent.server.switchData.put(offr.getDatapathId(), offr);
	    	}else{	    		
	    		msgEvent.server.sendReportToUI("Received OF Message type = " + ofm.getType().toString());
	    	}
	    }
	}
}
