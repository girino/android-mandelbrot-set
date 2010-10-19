package org.girino.frac.android;

import org.girino.frac.operators.BurningShipOperator;
import org.girino.frac.operators.CubeMandelbrotOperator;
import org.girino.frac.operators.FourthMandelbrotOperator;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.MandelbarOperator;
import org.girino.frac.operators.NovaOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.operators.ShipBarOperator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MandelbrotActivity extends Activity {

	MandelbrotView view;
	public static final int SELECT_OPERATOR = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		view = new MandelbrotView(this);
		setContentView(view);
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

	private Object[][] operators = {
		{new OptimizedMandelbrotOperator(), "Mandelbrot Set"},
		{new BurningShipOperator(), "Burning Ship"},
		{new NovaOperator(), "Nova"},
		{new MandelbarOperator(), "Mandelbar"},
		{new CubeMandelbrotOperator(), "Cube Mandelbrot"},
		{new FourthMandelbrotOperator(), "Fourth Mandelbrto"},
		{new ShipBarOperator(), "Shipbar"},
	};
	
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