package org.girino.frac.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MandelbrotView extends View {

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Paint mBitmapPaint;

	public MandelbrotView(Context context) {
		super(context);
		rescale(getWidth(), getHeight());
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

		// draws first mandelbrot
		mCanvas.drawARGB(0xff, 10, 10, 10);
	}

	private void rescale(int w, int h) {
		width = w > 0 ? w : 1;
		height = h > 0 ? h : 1;
		scale = 100.0 * 300.0 / (double) width;

		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
	}

	public boolean stoped = true;
	Thread t;

	public void start() {
		Log.d("MandelbrotView", "starting...");
		if (!stoped) {
			stop();
		}
		t = new Thread() {
			@Override
			public void run() {
				runMandelbrot();
			}
		};
		stoped = false;
		t.start();
		invalidate();
		Log.d("MandelbrotView", "started...");
	}

	public void stop() {
		Log.d("MandelbrotView", "stoping...");
		if (t.isAlive()) {
			stoped = true;
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Log.d("MandelbrotView", "stoped...");
	}

	int i0 = 0;
	int j0 = 0;

	private double hscale(int i) {
		return (i0 + i - (width/2)) / scale - 0.5;
	}

	private double vscale(int j) {
		return (j0 + j - (height / 2)) / scale;
	}

	public void runMandelbrot() {
		for (int d = 8; d > 0; d /= 2) {
			for (int j = 0; j < height; j += d) {
				for (int i = 0; i < width; i += d) {
					double x = 0f;
					double y = 0f;
					int v = 0;
					for (; v < 40 && (x * x + y * y) / 2f < 2f; v++) {
						double t = hscale(i) + x * x - y * y;
						y = vscale(j) + x * y * 2f;
						x = t;
					}
					int r = (v == 40) ? 0 : (int) (v * 0xFF / 40.0) * d;
					int g = (v == 40) ? 0 : (int) (255 * Math.log(v + 1) / Math
							.log(41))
							* d;
					// int g = (v == 40)?0:(int)(255 * Math.pow(v,0.5)/6.325) * d;
					int b = (v == 40) ? 0 : (int) (v * 0xFF / 40.0) * d;
					r = (r > 255) ? 255 : r;
					g = (g > 255) ? 255 : g;
					b = (b > 255) ? 255 : b;
					mBitmapPaint.setARGB(0xFF, r, g, b);
					mCanvas.drawPoint(i, j, mBitmapPaint);
					if (stoped) {
						Log.d("MandelbrotView", "forced stop...");
						return;
					}
				}
			}
		}
		stoped = true;
	}

	int width = 320;
	int height = 480;
	double scale = 100.0;

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d("MandelbrotView", "onSizeChanged begin...");
		Log.d("MandelbrotView", "Size: " + w + "x" + h);
		super.onSizeChanged(w, h, oldw, oldh);
		stop();
		rescale(w, h);
		start();
		Log.d("MandelbrotView", "onSizeChanged end...");
	}

	float xoffset = 0;
	float yoffset = 0;

	@Override
	protected void onDraw(Canvas canvas) {
		// avoids drawing too much
		canvas.drawBitmap(mBitmap, xoffset, yoffset, mBitmapPaint);
		// stops only after drawing a last time
		if (stoped) {
			return;
		}
		invalidate();
	}

	float x0;
	float y0;
	boolean hasMoved = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d("MandelbrotView", "down...");
			x0 = x;
			y0 = y;
			hasMoved = false;
			break;
		case MotionEvent.ACTION_MOVE:
			xoffset = x - x0;
			yoffset = y - y0;
			hasMoved = true;
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			Log.d("MandelbrotView", "up...");
			if (hasMoved) {
				hasMoved = false;
				i0 += (int) ((x0 - x));
				j0 += (int) ((y0 - y));
				// move image
				xoffset = x - x0;
				yoffset = y - y0;
				Bitmap bm = mBitmap;
				rescale(width, height);
				mCanvas.drawBitmap(bm, xoffset, yoffset, mBitmapPaint);
				xoffset = 0;
				yoffset = 0;
				start();
			}
			break;
		}
		return true;
	}

}
