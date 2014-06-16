package sadsido.clearsky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


public class Service extends WallpaperService 
{
	// *************************************************************************************************

	@Override
	public Engine onCreateEngine() 
	{ return new MyEngine(); }

	// *************************************************************************************************

	private class MyEngine extends Engine 
	{
	    private final Handler handler = new Handler();
	    
	    private final Runnable runner = new Runnable()
	    { @Override public void run() { draw(); }};
	    
	    private Gradmap skymap;
	    private Gradmap landmap;
	    private Bitmap  landscape;
	    
	    private Rect srcRect;
	    private Rect dstRect;
	    
	    private int width;
	    private int height;
	    private int delta;

	    private boolean visible = true;   

		// *************************************************************************************************

	    public MyEngine() 
	    {
	    	
	    	skymap = new Gradmap(getResources(), R.drawable.skymap);
	    	landmap = new Gradmap(getResources(), R.drawable.landmap);
	    	landscape = BitmapFactory.decodeResource(getResources(), R.drawable.landscape);
	    	
	    	repostRunner(0);
	    }

		// *************************************************************************************************
	    
	    @Override
	    public void onCreate(SurfaceHolder surfaceHolder)
	    {
	    	super.onCreate(surfaceHolder);
	    	
	    	// start listening to manipulation
	    	// with dates and times and zones:
	    	
	    	IntentFilter filter = new IntentFilter();
			
	    	filter.addAction(Intent.ACTION_DATE_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
	        filter.addAction(Intent.ACTION_TIME_CHANGED);
	        
	        registerReceiver(onTimeChange, filter);
	    }

	    @Override
	    public void onDestroy()
	    {
	    	unregisterReceiver(onTimeChange);
	    	super.onDestroy();
	    }

	    @Override
	    public void onVisibilityChanged(boolean visible) 
	    {
	      super.onVisibilityChanged(visible);	      
	      this.visible = visible;
	      repostRunner(0);
	    }

	    @Override
	    public void onSurfaceDestroyed(SurfaceHolder holder)
	    {
	      super.onSurfaceDestroyed(holder);
	      this.onVisibilityChanged(false);
	    }

	    @Override
	    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	    {
	      this.width  = width;
	      this.height = height;
	      
	      precalcRects();
	      
	      super.onSurfaceChanged(holder, format, width, height);
	    }

		// *************************************************************************************************

	    @Override
	    public void onTouchEvent(MotionEvent event) 
	    {
	    	/***
	    	float x = event.getX();
    		float y = event.getY();
    		
    		delta = (int) y * 24 * 60 / height;
    		
  	      //  schedule next repaint:
  	      repostRunner(0);
    		
    		***/
	        super.onTouchEvent(event);   
	    }

		// *************************************************************************************************

	    private void draw()    
	    {
	      SurfaceHolder holder = getSurfaceHolder();
	      Canvas canvas = null;
	      
	      try 
	      {
	         canvas = holder.lockCanvas();
	         if (canvas != null)   
	         {
	        	 Time now = new Time();
	        	 now.setToNow();
	        	 
	        	 paintClearSky(canvas, now, delta);
	        	 paintLandscape(canvas, now, delta);
	         }
	      } 
	      finally 
	      {
	        if (canvas != null)
	          holder.unlockCanvasAndPost(canvas);
	      }
	      
	      //  schedule next repaint (20 minutes):
	      repostRunner(20 * 60 * 1000);
	    }

	    // *************************************************************************************************

	    private void repostRunner(long msDelay)
	    {
	    	handler.removeCallbacks(runner);	    	
	    	if (visible) { handler.postDelayed(runner, msDelay); }
	    }
	    
	    private void paintClearSky(Canvas canvas, Time time, int offset)
	    {
			int [] skysample = skymap.getSample(time, offset);
			 
			LinearGradient shader = new LinearGradient(0, 0, width, height, skysample, null, TileMode.CLAMP);
			Paint paint = new Paint();
			  
			paint.setShader(shader);				 
		 	canvas.drawPaint(paint);
	    }
	
	    private void paintLandscape(Canvas canvas, Time time, int offset)
	    {
	       	int [] lndsample = landmap.getSample(time, offset);
	       		       	
	       	ColorFilter filter = new LightingColorFilter(lndsample[0], 1);
	       	Paint paint = new Paint();
  
	       	paint.setColorFilter(filter);	       	
	       	canvas.drawBitmap(landscape, srcRect, dstRect, paint);
	    }
	    	    
	    private void precalcRects()
	    {
	    	if (width >= height)
	    	{
	    		// landscape mode, fit entire picture:
	    		int dstHeight = width * landscape.getHeight() / landscape.getWidth();	    		
	    		srcRect = new Rect(0, 0, landscape.getWidth(), landscape.getHeight());
	    		dstRect = new Rect(0, height - dstHeight, width, height);
	    	}
	    	else
	    	{
	    		// portrait mode, crop center part:
	    		int dstHeight = height * landscape.getHeight() / landscape.getWidth();
	    		int srcWidth  = landscape.getWidth() * width / height;
	    		int srcBorder = (landscape.getWidth() - srcWidth) / 2;
	    		
	    		srcRect = new Rect(srcBorder, 0, landscape.getWidth() - srcBorder, landscape.getHeight());
	    		dstRect = new Rect(0, height - dstHeight, width, height);
	    	}
	    }
	    
	    // *************************************************************************************************
	    
	    private final BroadcastReceiver onTimeChange = new BroadcastReceiver() 
	    {
	        @Override
	        public void onReceive(Context context, Intent intent) 
	        {
	            final String action = intent.getAction();

	            if (action.equals(Intent.ACTION_TIME_CHANGED) ||
	            	action.equals(Intent.ACTION_DATE_CHANGED) ||
	                action.equals(Intent.ACTION_TIMEZONE_CHANGED))
	            {
	                repostRunner(0);
	            }
	        }
	    };
	    
	    // *************************************************************************************************
	}
}
