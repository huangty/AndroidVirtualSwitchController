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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class AndroidVirtualSwitchController extends Activity {
	private Button button_startctl;
	private EditText field_port;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
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
    		int bindingPort = Integer.parseInt(field_port.getText().toString());
    		new OpenflowSwitchControlChannel(bindingPort).start();
    	}
    };
}