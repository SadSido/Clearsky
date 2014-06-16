package sadsido.clearsky;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.format.Time;


public class Gradmap 
{
	// *************************************************************************************************

	int [][] keys; 
	
	int minInDay;
	int minInSample;
	
	// *************************************************************************************************

	public Gradmap(Resources res, int resID)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();		
		options.inScaled  = false;
		
		Bitmap bitmap = BitmapFactory.decodeResource(res, resID, options);
		
		int bmw = bitmap.getWidth();
		int bmh = bitmap.getHeight();
		
		keys = new int[bmw][bmh];
		
		// I bet there is a better way to do it:
		
		for ( int x = 0; x < bmw; ++ x )
		for ( int y = 0; y < bmh; ++ y )
		{
			keys[x][y] = bitmap.getPixel(x, y);
		}
		
		// remember the number of minutes in sample:
	
		minInDay = (24 * 60);
		minInSample = minInDay / bmw;
	}

	// *************************************************************************************************

	public int sampleSize()
	{ return keys[0].length; }
	
	public int sampleCount()
	{ return keys.length; } 
	
	// *************************************************************************************************

	public int[] getSample( Time time, int minOffset )
	{
		// calc linear coefficients for interpolation:
		
		final int sinceMidnight = (time.hour * 60 + time.minute + minOffset) % minInDay;		
		
		final int baseSample  = sinceMidnight / minInSample;
		final int nextSample  = (baseSample + 1) % keys.length;
		
		final float nextCoeff = (sinceMidnight - baseSample * minInSample) / (float)(minInSample);
		final float baseCoeff = 1.0f - nextCoeff;
		
		// interpolate between samples:
		
		int [] sample = new int[sampleSize()];
		
		for ( int keyNo = 0; keyNo < sample.length; ++ keyNo )
		{ 
			final int baseColor = keys[baseSample][keyNo];
			final int nextColor = keys[nextSample][keyNo];
			
			sample[keyNo] = interpolate(baseColor, nextColor, baseCoeff, nextCoeff); 
		}
		
		return sample;
	}
	
	// *************************************************************************************************

	private static int clampChannel(float channel)
	{
		return Math.min(255, Math.max((int)channel, 0));
	}
	
	private static int interpolate(int color1, int color2, float coeff1, float coeff2)
	{
		final float red = coeff1 * Color.red(color1)   + coeff2 * Color.red(color2);
		final float grn = coeff1 * Color.green(color1) + coeff2 * Color.green(color2);
		final float blu = coeff1 * Color.blue(color1)  + coeff2 * Color.blue(color2);

		return Color.rgb(clampChannel(red), clampChannel(grn), clampChannel(blu));
		
	}
	
	// *************************************************************************************************
}
