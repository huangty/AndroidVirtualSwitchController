package edu.stanford.avsc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;

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
	ServerSocket ctlServerSocket = null;
		
	/** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;
    
    static final int MSG_REPORT_UPDATE = 4;
    
    static final int MSG_START_OPENFLOWD = 5;

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
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;
                case MSG_START_OPENFLOWD:
                	bind_port = msg.arg1;
                	sendReportToUI("Bind on port: " + bind_port);
                	Log.d("AVSC", "Send msg on bind: " + bind_port);          
                	
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

        // Tell the user we stopped.
        Toast.makeText(this, R.string.openflow_channel_stopped, Toast.LENGTH_SHORT).show();
    }
    private void sendReportToUI(String str){
    	Log.d("AVSC", "size of clients = " + mClients.size() );
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	Message msg = Message.obtain(null, MSG_REPORT_UPDATE);
            	Bundle data = new Bundle();
            	data.putString("MSG_REPORT_UPDATE", str);
            	msg.setData(data);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    
    private void startOpenflowD(){
    	OpenflowSwitchControlChannel oscc = new OpenflowSwitchControlChannel();
    	
    }
    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.openflow_channel_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());

        Intent intent = new Intent(this, StatusReport.class);
        Bundle bundle = new Bundle();
		bundle.putInt("BIND_PORT", bind_port);
		intent.putExtras(bundle);
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
    	Bundle bundle = intent.getExtras();
    	bind_port = bundle.getInt("BIND_PORT");    	
        return mMessenger.getBinder();
    }

	@Override
	public void run() {
		try {
        	ctlServerSocket = new ServerSocket(bind_port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("AVSC", "Started the Controller TCP server, listening on Port " + bind_port);
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
			}catch(IOException e){
				
			}
		}
	}

    
	/*public void run(){
        
	}*/

}