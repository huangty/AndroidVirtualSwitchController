package edu.stanford.avsc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMatch;

import edu.stanford.avsc.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private Button.OnClickListener startOpenflowSwitchControlChannel = new Button.OnClickListener(){
    	public void onClick(View v){
    		int bindingPort = -1;
    		try{
    			bindingPort = Integer.parseInt(field_port.getText().toString());
    		}catch(Exception e){
    			Toast.makeText(AndroidVirtualSwitchController.this, 
    					R.string.portRangeReminder, Toast.LENGTH_SHORT).show();
    		}
    		
    		if(bindingPort < 1024 || bindingPort > 65535){
    			Toast.makeText(AndroidVirtualSwitchController.this, 
    					R.string.portRangeReminder, Toast.LENGTH_SHORT).show();
    		}else{
    			Intent intent = new Intent();
    			intent.setClass(AndroidVirtualSwitchController.this, StatusReport.class);
    			startActivity(intent);
    		}
    		
    		/*else{
    			new OpenflowSwitchControlChannel(bindingPort).start();
    		}*/
    		
    	}
    };
}