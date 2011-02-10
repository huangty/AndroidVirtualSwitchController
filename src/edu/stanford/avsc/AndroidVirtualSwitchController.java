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


public class AndroidVirtualSwitchController extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        new OpenflowSwitchControlChannel().start();
    }
}