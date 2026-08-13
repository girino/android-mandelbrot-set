package org.girino.frac.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.girino.frac.operators.FractalOperator;
import org.girino.frac.palettes.PaletteProvider;

public class MandelbrotActivity extends Activity {
    private static final int SELECT_OPERATOR = 0;
    private static final int SELECT_PALETTE = 1;

    private MandelbrotView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new MandelbrotView(this);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.start();
    }

    @Override
    protected void onPause() {
        view.stop();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Menu.FIRST, 0, R.string.menu_formula);
        menu.add(0, Menu.FIRST + 1, 1, R.string.menu_palette);
        menu.add(0, Menu.FIRST + 2, 2, R.string.menu_smooth_palette);
        menu.add(0, Menu.FIRST + 3, 3, R.string.menu_zoom);
        menu.add(0, Menu.FIRST + 4, 4, R.string.menu_reset);
        menu.add(0, Menu.FIRST + 5, 5, R.string.menu_exit);
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
                view.stop();
                startActivityForResult(new Intent(this, PalettesListActivity.class), SELECT_PALETTE);
                return true;
            case 2:
                view.smooth();
                return true;
            case 3:
                view.zoom();
                return true;
            case 4:
                view.reset();
                return true;
            case 5:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_OPERATOR && resultCode >= Activity.RESULT_FIRST_USER) {
            FractalOperator operator = OperatorsListActivity.getOperator(
                    resultCode - Activity.RESULT_FIRST_USER);
            view.setOper(operator);
            view.reset();
        } else if (requestCode == SELECT_PALETTE && resultCode >= Activity.RESULT_FIRST_USER) {
            PaletteProvider provider = PalettesListActivity.getPalette(
                    resultCode - Activity.RESULT_FIRST_USER);
            view.setPalette(provider);
            view.start();
        }
    }
}
