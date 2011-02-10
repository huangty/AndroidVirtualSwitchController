package edu.stanford.avsc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;

import android.util.Log;


public class OpenflowSwitchControlChannel extends Thread{
	public void run(){		
		ServerSocket ctlServerSocket = null;
        try {
        	ctlServerSocket = new ServerSocket(6633);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("test", "Started the TCP server");
		while(true){
			try{
				byte[] buf = new byte[2000]; 
				Socket ofdSocket = ctlServerSocket.accept();
				InputStream inFromOfd = ofdSocket.getInputStream();
				OutputStream outToOfd = ofdSocket.getOutputStream();
				inFromOfd.read(buf);
				OFMatch ofm = new OFMatch();
				short inputPort = (short)ofdSocket.getLocalPort();
				ofm.loadFromPacket(buf, inputPort);
				Log.d("AVSC:Receive", ofm.toString());
				OFHello ofh = new OFHello();
				ByteBuffer bb = ByteBuffer.allocate(ofh.getLength());
				ofh.writeTo(bb);
				outToOfd.write(bb.array());
				Log.d("AVSC:Send", ofh.toString());
				//String clientSentence = inFromOfd.read
				//Log.d("test", clientSentence);
			}catch(IOException e){
				
			}
		}
	}
}