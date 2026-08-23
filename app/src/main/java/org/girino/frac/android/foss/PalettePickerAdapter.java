package org.girino.frac.android.foss;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

/** Palette bottom-sheet rows with a color swatch preview (issue #16). */
final class PalettePickerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ListView listView;
    private final Bitmap[] swatches;

    PalettePickerAdapter(Context context, ListView listView) {
        this.inflater = LayoutInflater.from(context);
        this.listView = listView;
        int count = PaletteCatalog.size();
        swatches = new Bitmap[count];
        for (int i = 0; i < count; i++) {
            swatches[i] = PaletteSwatch.createStrip(PaletteCatalog.get(i));
        }
    }

    @Override
    public int getCount() {
        return PaletteCatalog.size();
    }

    @Override
    public String getItem(int position) {
        return PaletteCatalog.labels()[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.palette_picker_item, parent, false);
        }
        CheckedTextView label = row.findViewById(R.id.palette_label);
        ImageView swatch = row.findViewById(R.id.palette_swatch);
        label.setText(getItem(position));
        label.setChecked(listView.isItemChecked(position));
        swatch.setImageBitmap(swatches[position]);
        return row;
    }
}
