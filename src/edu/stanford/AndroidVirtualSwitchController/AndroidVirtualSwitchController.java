package edu.stanford.AndroidVirtualSwitchController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class AndroidVirtualSwitchController extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        new Thread(){
			public void run(){
				ServerSocket welcomeSocket = null;
		        try {
					welcomeSocket = new ServerSocket(1234);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d("test", "Started the TCP server");
				while(true){
					try{
						Socket connectionSocket = welcomeSocket.accept();
						BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
						String clientSentence = inFromClient.readLine();
						Log.d("test", clientSentence);
					}catch(IOException e){
						
					}
				}
			}
		}.start();
    }
}