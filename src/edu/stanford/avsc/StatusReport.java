package edu.stanford.avsc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The Status Report Activity of the Controller
 * This Activity will be called from the Welcome Activity (AndroidVirtualSwitchController)
 * 
 * @author Te-Yuan Huang (huangty@stanford.edu)
 *
 */

public class StatusReport extends Activity implements Runnable {
	private StringBuffer mBuffer = new StringBuffer();
	private TextView tview_report;
	private ScrollView sview_report;
	private int bind_port;
	private Thread ofd_thread = null;
	/*
	 * To show the report on UI 
	 */
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			mBuffer.append(msg.getData()+"\n");
			doRedraw();
		}
	};
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        findViews();
        setListeners();
        getBundleValues();
        if(ofd_thread == null){
        	ofd_thread = new OpenflowSwitchControlChannel(bind_port, mHandler);
        	ofd_thread.start();
        }
        Message msg = mHandler.obtainMessage();
        Bundle data = new Bundle();
        data.putString("status", "started the server");
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
    private void findViews(){
    	tview_report = (TextView)findViewById(R.id.report);
    	sview_report = (ScrollView)findViewById(R.id.txt_scrollview);
    	
    }
    private void setListeners(){    
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
	public void run() {
		// TODO Auto-generated method stub
		
	}
    
}
