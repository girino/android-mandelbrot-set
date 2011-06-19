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
import android.view.ScaleGestureDetector;
import android.view.View;

public class MandelbrotView extends View {

	private static final int INVALID_POINTER_ID = -1;

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Paint mBitmapPaint;
	private ScaleGestureDetector mScaleDetector;
	private ScaleListener mScaleListener;

	
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
		
		// scale gesture
		mScaleListener = new ScaleListener();
		mScaleDetector = new ScaleGestureDetector(context, mScaleListener);
	}
	
	float cumulatedScale = 1f;
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			stop();
	        Log.d("MandelbrotView", "onScaleBegin");
			cumulatedScale = 1f;
			return true;
		}
		
	    @Override
	    public boolean onScale(ScaleGestureDetector detector) {
	    	cumulatedScale *= detector.getScaleFactor();
	        invalidate();
	        return true;
	    }
	    
	    @Override
	    public void onScaleEnd(ScaleGestureDetector detector) {
	        Log.d("MandelbrotView", "onScaleEnd " + cumulatedScale);
	    	scale *= cumulatedScale;
	    	
			Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bm);
		    canvas.save();
		    // delta for scaling
		    float dx = (1 - cumulatedScale) * width / 2;
		    float dy = (1 - cumulatedScale) * height / 2;
	    	canvas.translate(mPosX + dx, mPosY + dy);
		    canvas.scale(cumulatedScale, cumulatedScale);
		    canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
		    canvas.restore();
			mBitmap = bm;
			mCanvas = canvas; 
	    	
			cumulatedScale = 1f;
	    	start();
	    }
	    
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

	@Override
	protected void onDraw(Canvas canvas) {
		// avoids drawing too much
        //Log.d("MandelbrotView", "Redrawing...");
	    canvas.save();
	    // delta for scaling
	    float dx = (1 - cumulatedScale) * width / 2;
	    float dy = (1 - cumulatedScale) * height / 2;
    	canvas.translate(mPosX + dx, mPosY + dy);
	    canvas.scale(cumulatedScale, cumulatedScale);
	    canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
	    canvas.restore();
		// stops only after drawing a last time
		if (stoped) {
			return;
		}
		invalidate();
	}

    private float mPosX;
    private float mPosY;
    
    private float mLastTouchX;
    private float mLastTouchY;


    private int mActivePointerId = INVALID_POINTER_ID;

    @Override
	public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
	    switch (action & MotionEvent.ACTION_MASK) {
	    case MotionEvent.ACTION_DOWN: {
	        final float x = ev.getX();
	        final float y = ev.getY();
	        
	        mLastTouchX = x;
	        mLastTouchY = y;

	        // Save the ID of this pointer
	        mActivePointerId = ev.getPointerId(0);
	        break;
	    }
	        
	    case MotionEvent.ACTION_MOVE: {
	        // Find the index of the active pointer and fetch its position
	        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
	        final float x = ev.getX(pointerIndex);
	        final float y = ev.getY(pointerIndex);
	        
	        if (!mScaleDetector.isInProgress()) {
	        	final float dx = x - mLastTouchX;
	        	final float dy = y - mLastTouchY;
	        
	        	mPosX += dx;
	        	mPosY += dy;
		    
	        	invalidate();
	        }
	        
	        mLastTouchX = x;
	        mLastTouchY = y;
	        
	        Log.d("MandelbrotView", "moved " + mPosX + "x" + mPosY);
	        break;
	    }
	        
	    case MotionEvent.ACTION_UP:	        
	    case MotionEvent.ACTION_CANCEL: {
	        mActivePointerId = INVALID_POINTER_ID;
			x0 -= mPosX / scale;
			y0 -= mPosY / scale;
			// move bitmap
			Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bm);
			canvas.drawBitmap(mBitmap, mPosX, mPosY, mBitmapPaint);
			mBitmap = bm;
			mCanvas = canvas; 
	        mPosX = 0;
	        mPosY = 0;
	        start();
	        break;
	    }
	    
	    case MotionEvent.ACTION_POINTER_UP: {
	        // Extract the index of the pointer that left the touch sensor
	        final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) 
	                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	        final int pointerId = ev.getPointerId(pointerIndex);
	        if (pointerId == mActivePointerId) {
	            // This was our active pointer going up. Choose a new
	            // active pointer and adjust accordingly.
	            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
	            mLastTouchX = ev.getX(newPointerIndex);
	            mLastTouchY = ev.getY(newPointerIndex);
	            mActivePointerId = ev.getPointerId(newPointerIndex);
	        }
	        break;
	    }
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
