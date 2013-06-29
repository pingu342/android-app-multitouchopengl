package jp.saka.multitouchopengl;

import android.app.Activity;
import android.view.Window;
import android.widget.TextView;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MultiTouchOpenglActivity extends Activity
{
	private GLSurfaceView mGLView;
	private TextView mTextView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d("sakalog", "MultiTouchOpenglActivity#onCreate");
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		mGLView = (GLSurfaceView)findViewById(R.id.GLView);
		mTextView = (TextView)findViewById(R.id.TextView);
		mTextView.setText("Hello World.");
	}

	@Override
	public void onPause()
	{
		Log.d("sakalog", "MultiTouchOpenglActivity#onPause");
		
		super.onPause();
		mGLView.onPause();
	}

	@Override
	public void onResume()
	{
		Log.d("sakalog", "MultiTouchOpenglActivity#onResume");
		
		super.onResume();
		mGLView.onResume();
	}

}
