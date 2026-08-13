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
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class OperatorsListActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings));
		getListView().setTextFilterEnabled(true);
	}
	
	String[] mStrings = {
			"Mandelbrot Set",
			"Burning Ship",
			"Nova Set",
			"Mandelbar",
			"Cube Mandelbrot",
			"Mandelbrot to the fourth power",
			"Shipbar",
	};
	
	private static final FractalOperator[] operators = {
		new OptimizedMandelbrotOperator(),
		new BurningShipOperator(),
		new NovaOperator(),
		new MandelbarOperator(),
		new CubeMandelbrotOperator(),
		new FourthMandelbrotOperator(),
		new ShipBarOperator(),
	};
	
	static public FractalOperator getOperator(int i) {
		return operators[i];
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		
		setResult(Activity.RESULT_FIRST_USER + position);
		this.finish();
	}
	
}
