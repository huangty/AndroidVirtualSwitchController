package edu.stanford.avsc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * The Thread Create and Maintain Connections to Openflowd
 *
 * @author Te-Yuan Huang (huangty@stanford.edu)
 *
 */

public class OpenflowSwitchControlChannel extends Service implements Runnable{
	int bind_port = 6633;
    ServerSocketChannel ctlServer = null; 
	Selector selector = null; 
	OFMessageHandler ofm_handler = new OFMessageHandler();

	// A list of PendingChange instances
	private List pendingChanges = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map pendingData = new HashMap();
	public Map switchData = new HashMap<String, OFFeaturesReply>();
	
	/** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    
    /** Holds last value set by a client. */
    int mValue = 0;

    static final int MSG_REGISTER_CLIENT = 1;

    static final int MSG_UNREGISTER_CLIENT = 2;

    static final int MSG_SET_VALUE = 3;
    
    static final int MSG_REPORT_UPDATE = 4;
    
    static final int MSG_START_OPENFLOWD = 5;

    final int BUFFER_SIZE = 8192;
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;                    
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i=mClients.size()-1; i>=0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            mClients.remove(i);
                        }
                    }
                    break;
                case MSG_START_OPENFLOWD:
                	bind_port = msg.arg1;
                	sendReportToUI("Bind on port: " + bind_port);
                	Log.d("AVSC", "Send msg on bind: " + bind_port);
                	startOpenflowD();
                	
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.openflow_channel_started);
        //close server socket before leaving the service
        try{
	        if(ctlServer != null && ctlServer.isOpen()){
				ctlServer.socket().close();				
				ctlServer.close();
	        }
        }catch(IOException e){        	
        }
        // Tell the user we stopped.
        Toast.makeText(this, R.string.openflow_channel_stopped, Toast.LENGTH_SHORT).show();
        
    }
    public void sendReportToUI(String str){
    	//Log.d("AVSC", "size of clients = " + mClients.size() );
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	Message msg = Message.obtain(null, MSG_REPORT_UPDATE);
            	Bundle data = new Bundle();
            	data.putString("MSG_REPORT_UPDATE", str+"\n -------------------------------");
            	msg.setData(data);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }
    
    public void startOpenflowD(){    	
    	Log.d("AVSC", "Started the Controller TCP server, listening on Port " + bind_port);        
        

    	new Thread(this).start();    	
    }
    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        CharSequence text = getText(R.string.openflow_channel_started);

        // Set the icon, scrolling text and timestamp for notification
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, StatusReport.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.openflow_channel_started),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.openflow_channel_started, notification);
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

	@Override
	public void run() {
		try {			
        	ctlServer = ServerSocketChannel.open();
    		ctlServer.configureBlocking(false);
    		ctlServer.socket().bind(new InetSocketAddress(bind_port));
    		selector = Selector.open();
	        SelectionKey sk = ctlServer.register(selector, SelectionKey.OP_ACCEPT);	        
	        new Thread(ofm_handler).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
			
		}
		
    	Log.d("AVSC","starting openflowd on another thread");
    	ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    	try{
    		while (!Thread.interrupted()) {
    			
    			synchronized (this.pendingChanges) {
					Iterator changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						NIOChangeRequest change = (NIOChangeRequest) changes.next();
						switch (change.type) {
						case NIOChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
						}
					}
					this.pendingChanges.clear();
				}
    			/*if(selector == null){
    				
    			}*/
    			int num = selector.select();
    			Set selectedKeys = selector.selectedKeys();
    			Iterator it = selectedKeys.iterator();
    			while(it.hasNext()){
    				SelectionKey key = (SelectionKey) it.next();
    				it.remove();
    				if( ! key.isValid() ){
    					continue;
    				}else if( key.isAcceptable() ){
    					//handle new connection
    					ServerSocketChannel scc = (ServerSocketChannel) key.channel();
    					SocketChannel sc = scc.accept();
    					sc.configureBlocking(false);
    					SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
    					sendReportToUI("Accpet New Connection");
    					Log.d("AVSC", "accept new connection");
    				}else if(key.isReadable()){
    					//handle message from switch/remote host
    					read(key, readBuffer);   					    					
    				}else if(key.isWritable()){
    					write(key);
    				}
    			}    			
    		}
    	}catch (IOException e) {
			e.printStackTrace();
		}
	}	  
	private void read(SelectionKey key, ByteBuffer readBuffer) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		readBuffer.clear();
		int numRead = -1;
		try {
			numRead = sc.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			sc.close();
			return;
		}
		if (numRead == -1) {
			key.channel().close();
			key.cancel();
			return;
		}
		// Hand the data off to OFMessage Handler
		this.ofm_handler.processData(this, sc, readBuffer.array(), numRead);
	}
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
	public void send(SocketChannel socket, byte[] data) {
		synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			this.pendingChanges.add(new NIOChangeRequest(socket, NIOChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List queue = (List) this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList();
					this.pendingData.put(socket, queue);
				}
				
				queue.add(ByteBuffer.wrap(data));
				Log.d("AVSC", "wrap data = " + data);
			}
		}
		// Finally, wake up our selecting thread so it can make the required changes
		this.selector.wakeup();
	}
	
	public void insertFixRule(SocketChannel socket){
		sendReportToUI("Insert Fix Rule");
		/*OFFlowMod ofmod = new OFFlowMod();
		OFMatch match = new OFMatch();
		ofmod.setMatch(match);
		match.setInputPort((short)1);		
		OFActionOutput ofa = new OFActionOutput((short)0, (short)32767);
		List<OFAction> ofal = new LinkedList<OFAction>();
		ofmod.setActions(ofal);
		ofal.add(ofa);
		ByteBuffer bb = ByteBuffer.allocate(8192);
		ofmod.writeTo(bb);
		Log.d("AVSC", "ofmod = "+ofmod.toString());
		send(socket, bb.array());*/
		
		/*ofmod = new OFFlowMod();
		match = new OFMatch();	
		match.setInputPort((short)0);
		ofmod.setMatch(match);
		ofa = new OFActionOutput((short)1, (short)32767);
		ofal = new LinkedList<OFAction>();
		ofal.add(ofa);
		ofmod.setActions(ofal);
		bb = ByteBuffer.allocate(8192);
		ofmod.writeTo(bb);*/
		
		/*Iterator it = switchData.values().iterator();
		while(it.hasNext()){
			OFFeaturesReply offr = (OFFeaturesReply) it.next();
			int portSize = offr.getPorts().size();
			
		}*/
	}

}