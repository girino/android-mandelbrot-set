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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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
        
        for (int i = 0; i < operators.length; i++) {
        	menu.add(0, Menu.FIRST + i, 0, operators[i][1].toString());
        }

        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	if (item.getItemId() >= Menu.FIRST && item.getItemId() < Menu.FIRST + operators.length) {
    		view.stop();
    		FractalOperator oper = (FractalOperator) operators[item.getItemId() - Menu.FIRST][0];
    		view.setOper(oper);
    		view.start();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}