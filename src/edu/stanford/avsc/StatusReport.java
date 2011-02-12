package edu.stanford.avsc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Status Report Activity of the Controller
 * This Activity will be called from the Welcome Activity (AndroidVirtualSwitchController)
 * 
 * @author Te-Yuan Huang (huangty@stanford.edu)
 *
 */

public class StatusReport extends Activity {
	private StringBuffer mBuffer = new StringBuffer();
	private TextView tview_report;
	private ScrollView sview_report;
	private int bind_port;
	//private Thread ofd_thread = null;
	private final static int REPORT_RECEIVED = 1;
	/*
	 * To show the report on UI 
	 */
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case REPORT_RECEIVED:
					mBuffer.append(msg.getData()+"\n");
	                break;
	            default:
	                super.handleMessage(msg);
			}			
			doRedraw();
		}
	};
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	/** Some text view we are using to show state information. */
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case OpenflowSwitchControlChannel.MSG_SET_VALUE:
	            	//tview_report.setText("Received from service: " + msg.arg1);
	            	mBuffer.append("Received from service: "+ msg.arg1+"\n");
	                break;	
	            case OpenflowSwitchControlChannel.MSG_REPORT_UPDATE:
	            	//tview_report.setText("Received from service: " + msg.arg1);
	            	mBuffer.append(msg.getData().getString("MSG_REPORT_UPDATE")+"\n");
	                Log.d("AVSC", "Got reprot from openflowd");
	            	break;	
	            default:
	                super.handleMessage(msg);
	        }
	        doRedraw();
	    }
	}
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());


	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
	        tview_report.setText("Attached.");
	        Log.d("AVSC", "Service Attached");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null,
	            		OpenflowSwitchControlChannel.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

	            
	            msg = Message.obtain(null,
	            		OpenflowSwitchControlChannel.MSG_START_OPENFLOWD, bind_port, 0);	            
	            mService.send(msg);
	            
	            // Give it some value as an example.
	            /*msg = Message.obtain(null,
	            		OpenflowSwitchControlChannel.MSG_SET_VALUE, this.hashCode(), 0);
	            mService.send(msg);*/
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(StatusReport.this, R.string.openflow_channel_started,
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        tview_report.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(StatusReport.this, R.string.openflow_channel_stopped,
	                Toast.LENGTH_SHORT).show();
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
		Intent intent = new Intent(StatusReport.this, OpenflowSwitchControlChannel.class);
		Bundle bundle = new Bundle();
		bundle.putInt("BIND_PORT", bind_port);
		intent.putExtras(bundle);
	    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	    tview_report.setText("Binding.");
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                		OpenflowSwitchControlChannel.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        tview_report.setText("Unbinding.");
	    }
	}

	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        findViews();
        getBundleValues();
        doBindService();        
    }
    private void findViews(){
    	tview_report = (TextView)findViewById(R.id.report);
    	sview_report = (ScrollView)findViewById(R.id.txt_scrollview);
    	
    }
    private void doRedraw(){
    	tview_report.setText(mBuffer.toString());
    	sview_report.scrollTo(0, tview_report.getHeight());
    }
    private void getBundleValues(){
    	Bundle bundle = this.getIntent().getExtras();
    	bind_port = bundle.getInt("BIND_PORT");    	
    }
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	}

    
}
