package org.girino.frac.android;

import org.girino.frac.operators.Complex;
import org.girino.frac.operators.FractalOperator;
import org.girino.frac.operators.OptimizedMandelbrotOperator;
import org.girino.frac.palettes.HSBPalette;
import org.girino.frac.palettes.PaletteProvider;

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
	
	private FractalOperator oper = new OptimizedMandelbrotOperator();
	private PaletteProvider palette = new HSBPalette();
	private boolean smooth = false;

	public FractalOperator getOper() {
		return oper;
	}

	public void setOper(FractalOperator oper) {
		this.oper = oper;
	}
	
	public void setPalette(PaletteProvider provider) {
		this.palette = provider;
	}

	public MandelbrotView(Context context) {
		super(context);
		rescale(getWidth(), getHeight());
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

		// draws first mandelbrot
		mCanvas.drawARGB(0xff, 10, 10, 10);
	}

	private void rescale(int w, int h) {
		w = w > 0 ? w : 1;
		width = w;
		scale *= ((double)w)/(double)width;
		height = h > 0 ? h : 1;

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

	double x0 = 0.0;
	double y0 = 0.0;

	private double hscale(int i) {
		return (i - (width/2)) / scale + x0;
	}

	private double vscale(int j) {
		return (j - (height / 2)) / scale + y0;
	}

	public void runMandelbrot() {
		Complex C = new Complex();
		for (int d = 8; d > 0; d /= 2) {
			long begin = System.currentTimeMillis();
			for (int j = 0; j < height; j += d) {
				for (int i = 0; i < width; i += d) {
					C.set(hscale(i), vscale(j));
					double v = oper.apply(C, 40, smooth);

					mBitmapPaint.setColor(palette.getColor(v));
					if (d == 1)	mCanvas.drawPoint(i, j, mBitmapPaint);
					else mCanvas.drawRect(i, j, i+d, j+d, mBitmapPaint);
					if (stoped) {
						Log.d("MandelbrotView", "forced stop...");
						return;
					}
				}
			}
			long end = System.currentTimeMillis();
			long elapsed = end-begin;
			Log.d("MandelbrotView", "Elapsed time: " + elapsed);
		}
		stoped = true;
	}

	int width = 320;
	int height = 480;
	double scale = 100.0 * 300.0 / (double) width;

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

	float ex0;
	float ey0;
	boolean hasMoved = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d("MandelbrotView", "down...");
			ex0 = x;
			ey0 = y;
			hasMoved = false;
			break;
		case MotionEvent.ACTION_MOVE:
			xoffset = x - ex0;
			yoffset = y - ey0;
			hasMoved = true;
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			Log.d("MandelbrotView", "up...");
			if (hasMoved) {
				hasMoved = false;
				x0 += (ex0 - x) / scale;
				y0 += (ey0 - y) / scale;
				// move image
				xoffset = x - ex0;
				yoffset = y - ey0;
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

	public void zoom() {
		stop();
		scale *= 1.5;
		start();
	}

	public void smooth() {
		stop();
		smooth = !smooth;
		start();
	}

	public void reset() {
		stop();
		x0 = 0;
		y0 = 0;
		scale = 100.0 * 300.0 / (double) width;
		start();
	}

}
