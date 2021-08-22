package net.christianbeier.droidvnc_ng;

/*
 * DroidVNC-NG InputService that binds to the Android a11y API and posts input events sent by the native backend to Android.
 *
 * Its original version was copied from https://github.com/anyvnc/anyvnc/blob/master/apps/ui/android/src/com/anyvnc/AnyVncAccessibilityService.java at
 * f32015d9d29d2d022217f52a99f676ace90cc29e.
 *
 * Original author is Tobias Junghans <tobydox@veyon.io>
 *
 * Licensed under GPL-2.0 as per https://github.com/anyvnc/anyvnc/blob/master/COPYING.
 *
 * Swipe fixes and gesture handling by Christian Beier <info@christianbeier.net>.
 *
 */

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.io.File;
import java.io.OutputStream;

public class InputService {

	private static final String TAG = "InputService";

	private static boolean mIsButtonOneDown;
	private static Point swipeStart=new Point();
	private static Point swipeEnd=new Point();
	private static long mLastGestureStartTime;

	private static boolean mIsKeyCtrlDown;
	private static boolean mIsKeyAltDown;
	private static boolean mIsKeyShiftDown;
	private static boolean mIsKeyDelDown;
	private static boolean mIsKeyEscDown;
	private static Process sh;
	private static OutputStream os;
	private static Context context;

	public static void startSu(Context context) {
		InputService.context = context;
		try {
			sh = Runtime.getRuntime().exec("su",null,new File("."));
			os = sh.getOutputStream();
		} catch (Exception e) {
			Log.e(TAG, "Error initializing root shell", e);
		}
	}

	public static void stopSu() {
		try {
			if(os != null) {
				os.close();
				sh = null;
				os = null;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error initializing root shell", e);
		}
	}

	public static boolean isEnabled()
	{
		return true;
	}

	private static void writeCommand(String command) throws Exception {
		Log.v(TAG, "EXECUTING COMMAND: " + command);
		//sh = Runtime.getRuntime().exec("su -c \"" + command + "\"",null,new File("/"));
		//sh = Runtime.getRuntime().exec(command,null,new File("."));
		//os = sh.getOutputStream();
		os.write((command + "\n").getBytes("ASCII"));
		//os.close();
	}


	public static void onPointerEvent(int buttonMask, int x, int y, long client) {
		Log.d(TAG, "onPointerEvent: buttonMask " + buttonMask + " x " + x + " y " + y + " by client " + client);

		try {
			/*
			    left mouse button
			 */

			// down, was up
			if ((buttonMask & (1 << 0)) != 0 && !mIsButtonOneDown) {
				startGesture(x, y);
				mIsButtonOneDown = true;
			}

			// down, was down
			//if ((buttonMask & (1 << 0)) != 0 && mIsButtonOneDown) {
			//	continueGesture(x, y);
			//}

			// up, was down
			if ((buttonMask & (1 << 0)) == 0 && mIsButtonOneDown) {
				endGesture(x, y);
				mIsButtonOneDown = false;
			}


			// right mouse button
			if ((buttonMask & (1 << 2)) != 0) {
				longPress(x, y);
			}

			// scroll up
			if ((buttonMask & (1 << 3)) != 0) {

				DisplayMetrics displayMetrics = new DisplayMetrics();
				WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
				wm.getDefaultDisplay().getRealMetrics(displayMetrics);

				scroll(x, y, -displayMetrics.heightPixels / 2);
			}

			// scroll down
			if ((buttonMask & (1 << 4)) != 0) {

				DisplayMetrics displayMetrics = new DisplayMetrics();
				WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
				wm.getDefaultDisplay().getRealMetrics(displayMetrics);

				scroll(x, y, displayMetrics.heightPixels / 2);
			}
		} catch (Exception e) {
			// instance probably null
			Log.e(TAG, "onPointerEvent: failed: " + e.toString());
		}
	}

	public static void onKeyEvent(int down, long keysym, long client) {
		Log.d(TAG, "onKeyEvent: keysym " + keysym + " down " + down + " by client " + client);



		/*
			Special key handling.
		 */
		try {
			/*
				Save states of some keys for combo handling.
			 */
			if(keysym == 0xFFE3)
				mIsKeyCtrlDown = down != 0;
			else if(keysym == 0xFFE9 || keysym == 0xFF7E) // MacOS clients send Alt as 0xFF7E
				mIsKeyAltDown = down != 0;
			else if(keysym == 0xFFE1)
				mIsKeyShiftDown = down != 0;
			else if(keysym == 0xFFFF)
				mIsKeyDelDown = down != 0;
			else if(keysym == 0xFF1B)
				mIsKeyEscDown = down != 0;


			if(keysym == 0xFF09 && down != 0) {
				Log.i(TAG, "onKeyEvent: got Tab");
				writeCommand("input keyevent \"KEYCODE_TAB\"");
			} else if (keysym == 0xFF0D && down != 0) {
				Log.i(TAG, "onKeyEvent: got Enter");
				writeCommand("input keyevent \"KEYCODE_ENTER\"");
			} else if (keysym == 0xFF50 && down != 0) {
				Log.i(TAG, "onKeyEvent: got Home/Pos1");
				writeCommand("input keyevent \"KEYCODE_HOME\"");
			} else if(keysym == 0xFF51 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got a DPAD LEFT");
				writeCommand("input keyevent KEYCODE_DPAD_LEFT");
			} else if(keysym == 0xFF52 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got a DPAD UP");
				writeCommand("input keyevent KEYCODE_DPAD_UP");
			} else if(keysym == 0xFF53 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got a DPAD RIGHT");
				writeCommand("input keyevent KEYCODE_DPAD_RIGHT");
			} else if(keysym == 0xFF54 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got a DPAD DOWN");
				writeCommand("input keyevent KEYCODE_DPAD_DOWN");
			} else if(keysym == 0xFF55 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got PgUp");
				writeCommand("input keyevent KEYCODE_APP_SWITCH");
			} else if(keysym == 0xFF56 && down != 0)  {
				Log.i(TAG, "onKeyEvent: got PgDown");
				writeCommand("service call statusbar 1");
//			} else if(keysym == 0xFF57 && down != 0)  {
//				Log.i(TAG, "onKeyEvent: got End");
//				writeCommand("input keyevent \"KEYCODE_POWER\"");
			} else if(mIsKeyCtrlDown && mIsKeyAltDown && mIsKeyDelDown) {
				Log.i(TAG, "onKeyEvent: got Ctrl-Alt-Del");
				/*Handler mainHandler = new Handler(getMainLooper());
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						MainService.togglePortraitInLandscapeWorkaround();
					}
				});*/
				return;
			} else if(mIsKeyCtrlDown && mIsKeyShiftDown && mIsKeyEscDown) {
				Log.i(TAG, "onKeyEvent: got Ctrl-Shift-Esc");
				//performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
			} else if(down != 0)
				writeCommand("input keyevent " + keysym);

		} catch (Exception e) {
			// instance probably null
			Log.e(TAG, "onKeyEvent: failed: " + e.toString());
		}
	}


	private static void startGesture(int x, int y) {
		swipeStart.set( x, y );
		mLastGestureStartTime = System.currentTimeMillis();
	}

//	private static void continueGesture(int x, int y) {
//		mPath.lineTo( x, y );
//	}

	private static void endGesture(int x, int y) throws Exception {
		swipeEnd.set( x, y );

		long time = System.currentTimeMillis() - mLastGestureStartTime;
		if(swipeEnd.equals(swipeStart) && time < 250)
			writeCommand("/system/bin/input tap " + swipeStart.x + " " + swipeStart.y);
		else
			writeCommand("/system/bin/input swipe " + swipeStart.x + " " + swipeStart.y + " " + swipeEnd.x + " " + swipeEnd.y + " " + time);
//		GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription( mPath, 0, System.currentTimeMillis() - mLastGestureStartTime);
//		GestureDescription.Builder builder = new GestureDescription.Builder();
//		builder.addStroke(stroke);
//		dispatchGesture(builder.build(), null, null);
	}


	private static void longPress( int x, int y )
	{
			//dispatchGesture( createClick( x, y, ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout()), null, null );
	}

	private static void scroll( int x, int y, int scrollAmount )
	{
			/*
			   Ignore if another gesture is still ongoing. Especially true for scroll events:
			   These mouse button 4,5 events come per each virtual scroll wheel click, an incoming
			   event would cancel the preceding one, only actually scrolling when the user stopped
			   scrolling.
			 */
//			if(!mGestureCallback.mCompleted)
//				return;
//
//			mGestureCallback.mCompleted = false;
//			dispatchGesture(createSwipe(x, y, x, y - scrollAmount, ViewConfiguration.getScrollDefaultDelay()), mGestureCallback, null);
	}

	/*private static GestureDescription createClick( int x, int y, int duration )
	{
		Path clickPath = new Path();
		clickPath.moveTo( x, y );
		GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription( clickPath, 0, duration );
		GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
		clickBuilder.addStroke( clickStroke );
		return clickBuilder.build();
	}*/

}
