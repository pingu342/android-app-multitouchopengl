package jp.saka.multitouchopengl;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.view.MotionEvent;
import android.view.GestureDetector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class RectSize {
	public int width;
	public int height;
	public RectSize() {
		width = height = 0;
	}
	public RectSize(int w, int h) {
		width  = w;
		height = h;
	}
	public float getAspectRatio() {
		return (float)width/(float)height;
	}
	public Coord2D getCenter() {
		int x = width/2;
		int y = height/2;
		return new Coord2D(x, y);
	}
	public static RectSize makeRectSizeRescaled(RectSize size, float scalefactor) {
		float w = (float)size.width * scalefactor;
		float h = (float)size.height * scalefactor;
		RectSize rescaled = new RectSize((int)w, (int)h);
		return rescaled;
	}
	public String toString() {
		return "{width:" + width + ",height:" + height + "}";
	}
}

final class Coord2D {
	public int x;
	public int y;
	public Coord2D() {
		x = y = 0;
	}
	public Coord2D(int _x, int _y) {
		x  = _x;
		y = _y;
	}
	public Coord2D makeCopy() {
		Coord2D coord2d = new Coord2D();
		coord2d.x = x;
		coord2d.y = y;
		return coord2d;
	}
	public String toString() {
		return "{x:" + x + ",y:" + y + "}";
	}
}

final class Rect {
	public RectSize size;
	public Coord2D base;  //左上頂点の座標を指す
	public Rect(int x, int y, int w, int h) {
		base = new Coord2D(x, y);
		size = new RectSize(w, h);
	}
	public Rect(Coord2D _base, RectSize _size) {
		base = _base;
		size = _size;
	}
	public Coord2D makeCoord2DRectBaseAlingedCenter(RectSize _size) {
		Coord2D _base = new Coord2D();
		_base.x = (size.width - _size.width) / 2;
		_base.y = (size.height - _size.height) / 2;
		return _base;
	}
	public Rect makeRectAlignCenter(RectSize _size, float scalefactor) {
		_size = RectSize.makeRectSizeRescaled(_size, scalefactor);
		Coord2D _base = makeCoord2DRectBaseAlingedCenter(_size);
		return new Rect(_base, _size);
	}
	public float getScaleFactorToDrawAspectFit(RectSize _size) {
		float scalefactor = 1.0f;
		if (size.width > size.height) {
			scalefactor = (float)size.height/(float)_size.height;
		} else {
			scalefactor = (float)size.width/(float)_size.width;
		}
		return scalefactor;
	}
	public RectSize makeSizeAspectFit(RectSize _size) {
		float scalefactor = getScaleFactorToDrawAspectFit(_size);
		return RectSize.makeRectSizeRescaled(_size, scalefactor);
	}
	public Rect makeRectBaseAlignCenterSizeAspectFit(RectSize _size) {
		_size = makeSizeAspectFit(_size);
		Coord2D _base = makeCoord2DRectBaseAlingedCenter(_size);
		return new Rect(_base, _size);
	}
	public String toString() {
		return "{base:" + base + ",size:" + size + "}";
	}
}

final class TouchPoint {
	public float x;
	public float y;
	public TouchPoint() {
		x = y = 0.0f;
	}
	public TouchPoint(float _x, float _y) {
		x = _x;
		y = _y;
	}
	public String toString() {
		return "{x:" + x + ",y:" + y + "}";
	}
}

final class TouchDistance {
	public float dx;
	public float dy;
	public TouchDistance(TouchPoint p0, TouchPoint p1) {
		dx = p1.x - p0.x;
		dy = p1.y - p0.y;
	}
	public String toString() {
		return "{dx:" + dx + ",dy:" + dy + "}";
	}
}

final class Touch {
	public TouchPoint touchPoint = new TouchPoint();
	public double multiTouchDistance = 0.0f;
	public boolean touchDown;
	public Touch() {
		touchDown = false;
	}
	public TouchDistance makeTouchDistance(TouchPoint p) {
		return new TouchDistance(touchPoint, p);
	}
	public String toString() {
		return "{touchDown:" + touchDown + "touchPoint:" + touchPoint + "}";
	}
}

class GLView extends GLSurfaceView implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

	private GestureDetector mGestureDetector;

	public GLView(Context context) {
		super(context);
		mRenderer = new Renderer();
		setRenderer(mRenderer);
		mGestureDetector = new GestureDetector(context, this);
	}

	public GLView(Context context, AttributeSet att) {
		super(context, att);
		mRenderer = new Renderer();
		setRenderer(mRenderer);
		mGestureDetector = new GestureDetector(context, this);
	}

	private Renderer mRenderer;

	class Renderer implements GLSurfaceView.Renderer
	{
		private Rect mViewRect;
		private RectSize mTextureSize;
		private Rect mDrawTextureRect;
		private static final float MAX_SCALE_FACTOR=3.0f;
		private float mScaleFactorToDrawWholeTextureAspectFit=1.0f;
		private float mCurrentScaleFactor=1.0f;

		private boolean isDrawTextureRectTouched(TouchPoint touch) {
			Coord2D base = mDrawTextureRect.base;
			RectSize size = mDrawTextureRect.size;
			int x = (int)touch.x;
			int y = (int)touch.y;
			if ((base.x <= x && x <= (base.x + size.width)) &&
				(base.y <= y && y <= (base.y + size.height))) {
				return true;
			} else {
				return false;
			}
		}

		private Coord2D makeCoord2DDrawTextureRectTouched(TouchPoint touch) {
			if (!isDrawTextureRectTouched(touch))
				return null;
			Coord2D base = mDrawTextureRect.base;
			int x = (int)touch.x;
			int y = (int)touch.y;
			Coord2D p = new Coord2D((x - base.x), (y - base.y));
			return p;
		}

		private void rescaleDrawTextureRectAspectFit() {
			RectSize s = mViewRect.makeSizeAspectFit(mTextureSize);
			Coord2D p = mViewRect.makeCoord2DRectBaseAlingedCenter(s);
			mDrawTextureRect.size = s;
			mDrawTextureRect.base = p;
		}

		private void rescaleDrawTextureRectMaxSize() {
			RectSize s = RectSize.makeRectSizeRescaled(mTextureSize, MAX_SCALE_FACTOR);
			Coord2D p = mViewRect.makeCoord2DRectBaseAlingedCenter(s);
			mDrawTextureRect.size = s;
			mDrawTextureRect.base = p;
		}

		private void moveDrawTextureRect(TouchDistance td) {

			synchronized (this) {
				if (mDrawTextureRect == null) {
					return;
				}
				int x = mDrawTextureRect.base.x;
				int y = mDrawTextureRect.base.y;
				int cannotDrawWidth = mDrawTextureRect.size.width - mViewRect.size.width;
				int cannotDrawHeight = mDrawTextureRect.size.height - mViewRect.size.height;
				int tmp;

				tmp = x + (int)td.dx;
				if (x <= 0) {
					if (tmp > 0)
						x = 0;
					else if (tmp < (-1 * cannotDrawWidth))
						x = (-1 * cannotDrawWidth);
					else
						x = tmp;
				}

				if (y <= 0) {
					tmp = y + (int)td.dy;
					if (tmp > 0)
						y = 0;
					else if (tmp < (-1 * cannotDrawHeight))
						y = (-1 * cannotDrawHeight);
					else
						y = tmp;
				}

				mDrawTextureRect.base.x = x;
				mDrawTextureRect.base.y = y;
			}
		}

		private Coord2D makeCoord2DBaseRescaled(float scalefactor, Coord2D point) {
			Coord2D rescaledBase = mDrawTextureRect.base.makeCopy();
			rescaledBase.x += (point.x - (point.x * scalefactor));
			rescaledBase.y += (point.y - (point.y * scalefactor));
			return rescaledBase;
		}

		private void rescaleDrawTextureRect(float scalefactor, Coord2D point) {
			RectSize currentSize = mDrawTextureRect.size;
			RectSize rescaledSize = RectSize.makeRectSizeRescaled(currentSize, scalefactor);
			Coord2D rescaledBase = null;
			float newscalefactor = (float)rescaledSize.width / (float)mTextureSize.width;
			if (newscalefactor <= mScaleFactorToDrawWholeTextureAspectFit) {
				rescaledSize = mViewRect.makeSizeAspectFit(mTextureSize);
				rescaledBase = mViewRect.makeCoord2DRectBaseAlingedCenter(rescaledSize);
				newscalefactor = mScaleFactorToDrawWholeTextureAspectFit;
			} else if (MAX_SCALE_FACTOR < newscalefactor) {
				rescaledSize = RectSize.makeRectSizeRescaled(mTextureSize, MAX_SCALE_FACTOR);
				rescaledBase = makeCoord2DBaseRescaled(((float)rescaledSize.width / (float)currentSize.width), point);
				newscalefactor = MAX_SCALE_FACTOR;
			} else {
				rescaledBase = makeCoord2DBaseRescaled(((float)rescaledSize.width / (float)currentSize.width), point);
			}
			mDrawTextureRect.size = rescaledSize;
			mDrawTextureRect.base = rescaledBase;

			Coord2D rectBaseAlignedCenter = mViewRect.makeCoord2DRectBaseAlingedCenter(mDrawTextureRect.size);
			int cannotDrawWidth = mDrawTextureRect.size.width - mViewRect.size.width;
			int cannotDrawHeight = mDrawTextureRect.size.height - mViewRect.size.height;
			if (cannotDrawWidth <= 0) {
				if (newscalefactor <= mScaleFactorToDrawWholeTextureAspectFit) {
					mDrawTextureRect.base.x = rectBaseAlignedCenter.x;
				}
			} else {
				if (mCurrentScaleFactor > newscalefactor) {
					if (mDrawTextureRect.base.x > 0) {
						mDrawTextureRect.base.x = 0;
					} else if (mDrawTextureRect.base.x < (-1 * cannotDrawWidth)) {
						mDrawTextureRect.base.x = (-1 * cannotDrawWidth);
					}
				}
			}
			if (cannotDrawHeight <= 0) {
				if (newscalefactor <= mScaleFactorToDrawWholeTextureAspectFit) {
					mDrawTextureRect.base.y = rectBaseAlignedCenter.y;
				}
			} else {
				if (mCurrentScaleFactor > newscalefactor) {
					if (mDrawTextureRect.base.y > 0) {
						mDrawTextureRect.base.y = 0;
					} else if (mDrawTextureRect.base.y < (-1 * cannotDrawHeight)) {
						mDrawTextureRect.base.y = (-1 * cannotDrawHeight);
					}
				}
			}

			mCurrentScaleFactor = newscalefactor;
			Log.d("sakalog", "DrawRect=" + mDrawTextureRect + " CurrentScaleFactor=" + mCurrentScaleFactor);
		}

		private void scaleUpDrawRect(Coord2D point) {
			Log.d("sakalog", "scaleUp");
			rescaleDrawTextureRect(1.04f, point);
			Log.d("sakalog", "mDrawTextureRect=" + mDrawTextureRect);
		}

		private void scaleDownDrawRect(Coord2D point) {
			Log.d("sakalog", "scaleDown");
			rescaleDrawTextureRect(0.96f, point);
			Log.d("sakalog", "mDrawTextureRect=" + mDrawTextureRect);
		}

		@Override
		public void onSurfaceCreated(GL10 gl10, EGLConfig eglconfig) {
			Log.d("sakalog", "GLSurfaceView.Renderer#onSurfaceCreated thread=" + Thread.currentThread().getName());
		}

		@Override
		public void onSurfaceChanged(GL10 gl10, int w, int h) {
			Log.d("sakalog", "GLSurfaceView.Renderer#onSurfaceChanged thread=" + Thread.currentThread().getName() + " w=" + w + " h=" + h);

			synchronized (this) {
				mViewRect = new Rect(0, 0, w, h);
				Log.d("sakalog", "ViewRect=" + mViewRect);

				Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.doyagao);
				mTextureSize = new RectSize(bmp.getWidth(), bmp.getHeight());
				Log.d("sakalog", "TextureSize=" + mTextureSize);

				mScaleFactorToDrawWholeTextureAspectFit = mViewRect.getScaleFactorToDrawAspectFit(mTextureSize);

				mCurrentScaleFactor = mScaleFactorToDrawWholeTextureAspectFit;

				mDrawTextureRect = mViewRect.makeRectBaseAlignCenterSizeAspectFit(mTextureSize);
				Log.d("sakalog", "DrawRect=" + mDrawTextureRect + " CurrentScaleFactor=" + mCurrentScaleFactor);

				//利用する領域を指定（原点は左下）
				gl10.glViewport(0, 0, w, h);

				gl10.glEnable(GL10.GL_TEXTURE_2D);
				int[] buffers = new int[1];
				gl10.glGenTextures(1, buffers, 0);
				int texture = buffers[0];
				gl10.glBindTexture(GL10.GL_TEXTURE_2D, texture);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
				gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
				gl10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
				bmp.recycle();
			}
		}

		@Override
		public void onDrawFrame(GL10 gl10) {
			//			Log.d("sakalog", "onDrawFrame");

			synchronized (this) {
				//GLViewの全領域を黒（不透明）塗り潰す
				//glViewportで指定した領域だけを塗り潰すのではないみたい
				gl10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
				gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

				setTextureArea(gl10, 0, 0, mTextureSize.width, mTextureSize.height);

				drawQuad(gl10, mDrawTextureRect.base.x, mDrawTextureRect.base.y, mDrawTextureRect.size.width, mDrawTextureRect.size.height);
			}
		}

		private float getCoordX(int x) {
			float fx = (float)x;
			float fw = (float)mViewRect.size.width;
			return (fx / fw) * 2.0f - 1.0f;
		}

		private float getCoordY(int y) {
			float fy = (float)y;
			float fh = (float)mViewRect.size.height;
			return (fy / fh) * 2.0f - 1.0f;
		}

		private float getCoordU(int x) {
			float fx = (float)x;
			float fw = (float)mTextureSize.width;
			return (fx / fw);
		}

		private float getCoordV(int y) {
			float fy = (float)y;
			float fh = (float)mTextureSize.height;
			return (fy / fh);
		}

		private void setTextureArea(GL10 gl10, int x, int y, int w, int h) {
			float left = getCoordU(x);
			float top = getCoordV(y);
			float right = left + getCoordU(w);
			float bottom = top + getCoordV(h);

			float uv[] = {
				left, top,		//左上
				left, bottom,	//左下
				right, top,		//右上
				right, bottom	//右下
			};

			ByteBuffer bb = ByteBuffer.allocateDirect(uv.length*4);
			bb.order(ByteOrder.nativeOrder());
			FloatBuffer fb = bb.asFloatBuffer();
			fb.put(uv);
			fb.position(0);

			gl10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fb);
		}

		private void drawQuad(GL10 gl10, int x, int y, int w, int h) {
			float left = getCoordX(x);
			float top = getCoordY(y);
			float right = left + getCoordX(w) + 1.0f;
			float bottom = top + getCoordY(h) + 1.0f;
			top = -top;
			bottom = -bottom;
			float positions[] = {
				left, top, 0.0f,		//左上
				left, bottom, 0.0f,		//左下
				right, top, 0.0f,		//右上
				right, bottom, 0.0f		//右下
			};
			ByteBuffer bb = ByteBuffer.allocateDirect(positions.length*4);
			bb.order(ByteOrder.nativeOrder());
			FloatBuffer fb = bb.asFloatBuffer();
			fb.put(positions);
			fb.position(0);
			gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl10.glVertexPointer(3, GL10.GL_FLOAT, 0, fb);
			gl10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, positions.length/3);
		}
	}

	private Touch mTouch = new Touch();
	private boolean mSkipOffsetDrawRect = false;

	@Override
	public boolean onTouchEvent(MotionEvent e) {

		mGestureDetector.onTouchEvent(e);

		if (e.getPointerCount() == 1) {

			TouchPoint touchPoint = new TouchPoint(e.getX(), e.getY());
			Log.d("sakalog", "touchPoint=" + touchPoint + " thread=" + Thread.currentThread().getName());

			Coord2D touchDrawTextureRectCoord = mRenderer.makeCoord2DDrawTextureRectTouched(touchPoint);

			if (touchDrawTextureRectCoord != null) {
				Log.d("sakalog", "touchDrawTextureRectCoord=" + touchDrawTextureRectCoord + " thread=" + Thread.currentThread().getName());
				switch (e.getAction()) {
					case MotionEvent.ACTION_MOVE:
						TouchDistance td = mTouch.makeTouchDistance(touchPoint);
						Log.d("sakalog", "distance=" + td + " thread=" + Thread.currentThread().getName());
						if (!mSkipOffsetDrawRect) 
							mRenderer.moveDrawTextureRect(td);
						mSkipOffsetDrawRect = false;
						break;

					case MotionEvent.ACTION_DOWN:
						mTouch.touchDown = true;
						break;

					case MotionEvent.ACTION_UP:
						mTouch.touchDown = false;
						break;
				}
			}

			mTouch.touchPoint = touchPoint;

		} else if (e.getPointerCount() == 2) {

			int pointId1 = e.getPointerId(0);
			int pointId2 = e.getPointerId(1);
			int pointIndex1 = e.findPointerIndex(pointId1);
			int pointIndex2 = e.findPointerIndex(pointId2);

			double distance = Math.pow((e.getX(pointIndex2) - e.getX(pointIndex1)), 2.0f) +
				Math.pow((e.getY(pointIndex2) - e.getY(pointIndex1)), 2.0f);

			TouchPoint touchPoint = new TouchPoint(
					(e.getX(pointIndex2) + e.getX(pointIndex1)) / 2.0f,
					(e.getY(pointIndex2) + e.getY(pointIndex1)) / 2.0f);
			Log.d("sakalog", "touchPoint=" + touchPoint + " thread=" + Thread.currentThread().getName());

			Coord2D textureCoordTouched = mRenderer.makeCoord2DDrawTextureRectTouched(touchPoint);

			if (textureCoordTouched != null) {
				Log.d("sakalog", "textureCoordTouched=" + textureCoordTouched + " thread=" + Thread.currentThread().getName());
				switch (e.getAction()) {
					case MotionEvent.ACTION_MOVE:
//					Log.d("sakalog", "distance=" + distance + " thread=" + Thread.currentThread().getName());
						if (mTouch.multiTouchDistance > distance)
							mRenderer.scaleDownDrawRect(textureCoordTouched);
						else
							mRenderer.scaleUpDrawRect(textureCoordTouched);
						mSkipOffsetDrawRect = true;
						break;
				}
			}

			mTouch.multiTouchDistance = distance;
		}

		return true;
	}

	//ダウン
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	//長押し
	@Override
	public void onLongPress(MotionEvent e) {
	}

	//フリック
	@Override
	public boolean onFling(MotionEvent e0,MotionEvent e1,
			float velocityX,float velocityY) {
		return false;
	}

	//スクロール
	@Override
	public boolean onScroll(MotionEvent e0,MotionEvent e1,
			float distanceX,float distanceY) {
		return false;
	}

	//プレス
	@Override
	public void onShowPress(MotionEvent e) {

	}

	//シングルタップ
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	//ダブルタップ
	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Log.d("sakalog", "onDoubleTap");
		//mRenderer.rescaleDrawTextureRectAspectFit();
		return true;
	}

	//ダブルタップイベント
	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	//シングルタップ
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

}
