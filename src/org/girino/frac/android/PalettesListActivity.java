package org.girino.frac.android;

import org.girino.frac.palettes.DefaultPaletteBlue;
import org.girino.frac.palettes.DefaultPaletteGreen;
import org.girino.frac.palettes.DefaultPaletteRed;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;
import org.girino.frac.palettes.SmoothFixedPalette;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PalettesListActivity extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings));
		getListView().setTextFilterEnabled(true);
	}
	
	String[] mStrings = {
			"Green",
			"Blue",
			"Red",
			"Rainbow 1",
			"Rainbow 2",
	};
	
	private static final PaletteProvider[] palettes = {
		new DefaultPaletteGreen(),
		new DefaultPaletteBlue(),
		new DefaultPaletteRed(),
		new HSBPalette(),
		new SmoothFixedPalette(),
	};
	
	static public PaletteProvider getPalette(int i) {
		return palettes[i];
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		
		setResult(Activity.RESULT_FIRST_USER + position);
		this.finish();
	}
	
}
