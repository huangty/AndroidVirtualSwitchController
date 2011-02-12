package edu.stanford.avsc;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * The Welcome Activity of the Controller
 *
 * @author Te-Yuan Huang (huangty@stanford.edu)
 *
 */

public class AndroidVirtualSwitchController extends Activity {
	private Button button_startctl;
	private EditText field_port;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        findViews();
        setListeners();
    }
    private void findViews(){
    	button_startctl = (Button)findViewById(R.id.start);
    	field_port = (EditText)findViewById(R.id.port);
    }
    private void setListeners(){
    	button_startctl.setOnClickListener(startOpenflowSwitchControlChannel);
    }
    /**
     * Check if the port number is within [1024 - 65535]. 
     * If yes, then start the controller; otherwise, staying in this activity.
     */
    private Button.OnClickListener startOpenflowSwitchControlChannel = new Button.OnClickListener(){
    	public void onClick(View v){
    		int bind_port = -1;
    		try{
    			bind_port = Integer.parseInt(field_port.getText().toString());
    		}catch(Exception e){
    			Toast.makeText(AndroidVirtualSwitchController.this, 
    					R.string.portRangeReminder, Toast.LENGTH_SHORT).show();
    		}
    		
    		if(bind_port < 1024 || bind_port > 65535){
    			Toast.makeText(AndroidVirtualSwitchController.this, 
    					R.string.portRangeReminder, Toast.LENGTH_SHORT).show();
    		}else{
    			/** 
    			 * Port number is okay, start the controller
    			 * */
    			Intent intent = new Intent();
    			intent.setClass(AndroidVirtualSwitchController.this, StatusReport.class);
    			Bundle bundle = new Bundle();
    			bundle.putInt("BIND_PORT", bind_port);
    			intent.putExtras(bundle);
    			startActivity(intent);
    		}    		
    	}
    };
}