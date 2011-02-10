package edu.stanford.avsc;

import android.app.Activity;
import android.os.Bundle;

/**
 * The Status Report Activity of the Controller
 * This Activity will be called from the Welcome Activity (AndroidVirtualSwitchController)
 * 
 * @author Te-Yuan Huang (huangty@stanford.edu)
 *
 */

public class StatusReport extends Activity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        findViews();
        setListeners();
    }
    private void findViews(){
    	
    }
    private void setListeners(){
    
    }
}
