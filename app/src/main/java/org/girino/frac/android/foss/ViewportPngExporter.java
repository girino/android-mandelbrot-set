package org.girino.frac.android.foss;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Share or save viewport PNGs locally (issue #18). No network permissions. */
public final class ViewportPngExporter {
    private static final String PNG_MIME = "image/png";
    private static final String GALLERY_FOLDER = "Fractals";
    private static final String FILE_PREFIX = "mandelbrot_";

    private ViewportPngExporter() {
    }

    public static String defaultFileName() {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return FILE_PREFIX + stamp + ".png";
    }

    public static boolean share(
            Context context,
            Bitmap bitmap,
            CharSequence chooserTitle,
            CharSequence shareText) {
        if (bitmap == null) {
            return false;
        }
        try {
            File file = writeCacheShareFile(context, bitmap);
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(PNG_MIME);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if (shareText != null && shareText.length() > 0) {
                intent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, chooserTitle));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Saves to Pictures/Fractals via MediaStore (API 29+). */
    public static boolean saveToGallery(Context context, Bitmap bitmap) {
        if (bitmap == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, defaultFileName());
        values.put(MediaStore.Images.Media.MIME_TYPE, PNG_MIME);
        values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/" + GALLERY_FOLDER);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return false;
        }
        if (!writeBitmap(context, bitmap, uri)) {
            resolver.delete(uri, null, null);
            return false;
        }
        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return true;
    }

    public static boolean writeBitmap(Context context, Bitmap bitmap, Uri uri) {
        if (bitmap == null || uri == null) {
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                return false;
            }
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            return false;
        }
    }

    private static File writeCacheShareFile(Context context, Bitmap bitmap) throws IOException {
        File dir = new File(context.getCacheDir(), "share");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create share cache dir");
        }
        File file = new File(dir, defaultFileName());
        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("PNG compress failed");
            }
        }
        return file;
    }
}
