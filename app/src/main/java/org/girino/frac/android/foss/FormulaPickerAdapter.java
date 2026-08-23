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

/** Formula bottom-sheet rows with a mini fractal preview (issue #30). */
final class FormulaPickerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ListView listView;
    private final Bitmap[] previews;

    FormulaPickerAdapter(Context context, ListView listView) {
        this.inflater = LayoutInflater.from(context);
        this.listView = listView;
        int count = FormulaCatalog.size();
        previews = new Bitmap[count];
        for (int i = 0; i < count; i++) {
            previews[i] = FormulaPreview.createThumbnail(FormulaCatalog.create(i));
        }
    }

    @Override
    public int getCount() {
        return FormulaCatalog.size();
    }

    @Override
    public String getItem(int position) {
        return FormulaCatalog.labels()[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.formula_picker_item, parent, false);
        }
        CheckedTextView label = row.findViewById(R.id.formula_label);
        ImageView preview = row.findViewById(R.id.formula_preview);
        label.setText(getItem(position));
        label.setChecked(listView.isItemChecked(position));
        preview.setImageBitmap(previews[position]);
        return row;
    }
}
