package org.girino.frac.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MandelbrotActivity extends Activity {

	MandelbrotView view;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// restart in default position
		view = new MandelbrotView(this);
		setContentView(view);
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
		view = null;
	}
}