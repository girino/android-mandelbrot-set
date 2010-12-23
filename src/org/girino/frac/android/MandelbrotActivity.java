package org.girino.frac.android;

import org.girino.frac.operators.FractalOperator;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;

public class MandelbrotActivity extends Activity {

	FrameLayout layout;
	MandelbrotView view;
	AdView adView;
	public static final int SELECT_OPERATOR = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		layout = new FrameLayout(this);
		view = new MandelbrotView(this);
		adView = new AdView(this);
		AdManager.setTestDevices(new String[] { 
				AdManager.TEST_EMULATOR,
				"04037B710901A015" // motorola droid
		});
		adView.setRequestInterval(20);
		adView.setBackgroundColor(0x000000);
		adView.setPrimaryTextColor(0xFFFFFF);
		adView.setSecondaryTextColor(0xCCCCCC);
		adView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		//adView.setBackgroundResource(R.color.white);
		

		//layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(view);
		layout.addView(adView);
		
		setContentView(layout);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		view.start();
		Log.i("MandelbrotActivity", "started...");
	}

	@Override
	protected void onPause() {
		super.onPause();
		view.stop();
		Log.i("MandelbrotActivity", "stoped...");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
    	menu.add(0, Menu.FIRST, 0, "Formula");
    	menu.add(0, Menu.FIRST+1, 0, "Zoom");
    	menu.add(0, Menu.FIRST+2, 0, "Reset");
    	menu.add(0, Menu.FIRST+3, 0, "Exit");

        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	switch (item.getItemId() - Menu.FIRST) {
    	case 0:
    		view.stop();
    		startActivityForResult(new Intent(this, OperatorsListActivity.class), SELECT_OPERATOR);
            return true;
    	case 1: 
    		view.zoom();
    		return true;
    	case 2:
    		view.reset();
    		return true;
    	case 3:
    		finish();
    		return true;
        }
    	
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	if (requestCode == SELECT_OPERATOR && resultCode >= Activity.RESULT_FIRST_USER) {
    		FractalOperator oper = OperatorsListActivity.getOperator(resultCode - Activity.RESULT_FIRST_USER);
    		Log.d("MandelbrotActivity", oper.toString());
    		view.setOper(oper);
    		view.reset();
    	}
    }
}