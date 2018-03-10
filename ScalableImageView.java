package com.ice.picture.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by asd on 1/21/2017.
 */

public class ScalableImageView extends android.support.v7.widget.AppCompatImageView
        implements GestureDetector.OnGestureListener, View.OnTouchListener, GestureDetector.OnDoubleTapListener {

    private static final String TAG = "ScalableImageView";

    //离开屏幕之后的滑动动画
    private ValueAnimator mFlingAnimator;

    //按下屏幕的时候 保存图片的矩阵
    private Matrix currentMatrix = new Matrix();

    private GestureDetector mGestureDetector;

    //点击的时候的坐标
    private float mDownY, mDownX;

    //图片真实的高宽
    private float mHeight, mWidth;

    //图片放大到最大的时候的宽高（即双击之后的宽高）
    float mLargeHeight,mLargeWidth;

    //屏幕的宽高
    private float mScreenWidth,mScreenHeight;

    //图片放大的比例
    private float mScaleRate;

    //是否已经双击了 放大了
    public boolean mIsScaled = false;

    //手指按下的时候   图片各个方向周围的坐标
    private int mDownLeft, mDownTop, mDownBottom, mDownRight;

    //单击的回调函数
    private OnClickListener mOnClickListener;

    public ScalableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        //获取屏幕宽高
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        mScreenWidth = point.x;
        mScreenHeight = point.y;


        //       给图片设置触摸事件
        setOnTouchListener(this);
        //        得到手势对象
        mGestureDetector = new GestureDetector(context, this);
        //        给手势设置双击事件
        mGestureDetector.setOnDoubleTapListener(this);

    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        mHeight = bm.getHeight();
        mWidth = bm.getWidth();

        mScaleRate = mScreenWidth / mWidth;//缩放到刚好宽度和屏幕一样宽

        scaleImage();

    }

    //根据mScaleRate缩放图片
    public void scaleImage() {

        Matrix matrix = new Matrix();

        //放大或者缩小
        matrix.postScale(mScaleRate, mScaleRate);

        //将图片平移到中间
        //gap:缩放后 屏幕高度 与  显示的高度 的差
        float gap = mScreenHeight - mHeight * mScaleRate;
        if (gap>0) {
            //有黑框  那么将图片移动到中间
            matrix.postTranslate(0, gap / 2);
        }else{
            matrix.postTranslate(0,-gap/2);
        }
        //水平移动到中间
        matrix.postTranslate(-(mWidth*mScaleRate - mScreenWidth)/2,0);

        setImageMatrix(matrix);
    }



    //获取图片的top bottom left right属性
    public void getImageState() {
        //保存按下时候 图片各个边界的大小
        Matrix matrix2 = getImageMatrix();
        float[] values = new float[9];
        matrix2.getValues(values);
        ImageState mapState = new ImageState();
        mapState.setLeft(values[2]);
        mapState.setTop(values[5]);
        mapState.setRight(mapState.getLeft() + mWidth * mScaleRate);
        mapState.setBottom(mapState.getTop() + mHeight * mScaleRate);
        mDownLeft = (int) mapState.getLeft();
        mDownTop = (int) mapState.getTop();
        mDownBottom = (int) mapState.getBottom();
        Log.d(TAG, "getImageState: "+mDownBottom);
        mDownRight = (int) mapState.getRight();
    }


    /**********************手势对象回调的方法***************************/
    /*
    * 按下时的回调方法，这里必须把返回值改为true，其他不改可以
    * */
    @Override
    public boolean onDown(MotionEvent e) {
        currentMatrix.set(getImageMatrix());
        mDownX = e.getX();
        mDownY = e.getY();

        getImageState();

        //取消上次的图片动画
        if (mFlingAnimator != null && mFlingAnimator.isRunning()) mFlingAnimator.pause();

        return true;
    }

    /*
    * 按下后短时间内无位移时回调方法
    * */
    @Override
    public void onShowPress(MotionEvent e) {
    }


    /*
    * 单击时回调
    * */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }


    /*
    * 手指滑动时回调 e1是点击时候的 e2是现在的
    * */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float dx = e2.getX() - mDownX;
        float dy = e2.getY() - mDownY;

        //平移
        setTranslation(currentMatrix,dx, dy);

        return false;
    }



    //图片是否处于在左边界  或 右边界
    private boolean isOnLeftBorder , isOnRightBorder = false;

    /**
     * 平移
     * @param oldMatrix  image的矩阵
     * @param dx         相对于oldMatrix的矩阵的x偏移量
     * @param dy         相对于oldMatrix的矩阵的y偏移量
     */
    private void setTranslation(Matrix oldMatrix,float dx, float dy) {
        Matrix matrix = new Matrix(oldMatrix);
        float tx = dx, ty = dy;
        //如果是放大的状态  那么可以随意移动
        if (mIsScaled) {
            //达到了左边界  再向右拉就会出现左边的黑屏  所以不允许继续拉了
            //下面几个同理
            if (mDownLeft + dx >= 0) {
                tx = -mDownLeft;
                isOnLeftBorder = true;
            }

            if (mDownRight + dx <= mScreenWidth) {
                tx = -(mDownRight - mScreenWidth);
                isOnRightBorder = true;
            }

            if (mDownTop + dy >= 0) {
                ty = -mDownTop;
            }

            if (mDownBottom + dy < mScreenHeight) {
                ty = -(mDownBottom - mScreenHeight);
            }

            if (mScreenHeight > mLargeHeight) {
                //高度不够 不能够上下滑动
                ty = 0;
            }

            if (tx != 0) {
                //没有在边界
                isOnLeftBorder = isOnRightBorder = false;
            }
            matrix.postTranslate(tx, ty);
            setImageMatrix(matrix);
        } else {
            //如果图片太高了  不需要放大也能上下移动
            if (mHeight * mScaleRate > mScreenHeight) {
                if(dy+mDownTop>=0){
                    ty = -mDownTop;
                }
                if(dy + mDownBottom<mScreenHeight){
                    ty = mScreenHeight - mDownBottom;
                }
                matrix.postTranslate(0, ty);
                setImageMatrix(matrix);
            }
        }


    }

    /*
    * 长按时回调，和onSingleTapUp()是互斥的
    * */
    @Override
    public void onLongPress(MotionEvent e) {
    }

    /*
    * 手指滑动屏幕后快速离开，屏幕还处于惯性滑动的状态时回调
    * */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY) {

        //继续滑动的动画
        mFlingAnimator = new ValueAnimator().ofFloat(80);
        mFlingAnimator.setInterpolator(new DecelerateInterpolator());
        mFlingAnimator.setDuration(400);
        mFlingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float dx = 0,dy = 0;
            Matrix oldMatrix;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(oldMatrix == null){
                    getImageState();//从新获取当前图片的边框位置
                    oldMatrix = new Matrix(getImageMatrix());
                }

                float value = (float) animation.getAnimatedValue();
                dx += velocityX / (value + 100);
                dy += velocityY / (value + 100);

                setTranslation(oldMatrix,dx, dy);
            }
        });

        mFlingAnimator.start();
        return false;
    }


    /*
    * imageView.setOnTouchListener(this)的回调方法
    * */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        MotionEvent obtain = MotionEvent.obtain(event);
        obtain.setLocation(event.getRawX(), event.getRawY());

        return mGestureDetector.onTouchEvent(event);
    }

    /*
    * 确认是单击事件时回调
    * */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
//        Util.activityList.get(Util.activityList.size() - 1).finish();
        if(mOnClickListener!=null)mOnClickListener.onClick(this);
        return false;
    }

    /*
    * 双击事件时回调
    * */
    @Override
    public boolean onDoubleTap(MotionEvent e) {

        mLargeWidth = mWidth * mScaleRate * 2;
        mLargeHeight = mHeight * mScaleRate * 2;

        if (mIsScaled) {
            //恢复正常大小
            mScaleRate/=2;
            scaleImage();
            mIsScaled = false;
        } else {
            mScaleRate*=2;
            scaleImage();
            //放大两倍
//            Matrix matrix = new Matrix();
//            matrix.setScale(mScaleRate * 2, mScaleRate * 2);
//            将放大后的图片平移到中间
//            matrix.postTranslate(-(mLargeWidth - mScreenWidth) / 2, (mScreenHeight - mLargeHeight) / 2);
//            setImageMatrix(matrix);

            mIsScaled = true;
        }
        return true;
    }



    /*
    * 双击事件
    * */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    private int downX;
    public boolean needMotionEvent(MotionEvent ev){

        if(!isScaled()){
            //没放大图片  父容器拦截   让用户滑动viewPager
            return false;
        }

        //图片放大了
        if(ev.getAction() == MotionEvent.ACTION_DOWN){
            downX = (int) ev.getX();
        }else {

            int dx = (int) (downX - ev.getX());
            if (dx < 0 && isOnLeftBorder()) {
                //向右滑  且在左边界  应该滑动viewPager
                return false;
            }else if(dx>0 && isOnRightBorder()){
                //向左滑  且在右边界  应该滑动viewPager
                return false;
            }else{
                //放大了图片  但是没有在边界  所以不进行拦截  让用户可以滑动图片
                return true;
            }
        }
        return false;
    }


    public boolean isScaled(){
        return mIsScaled;
    }

    public void setOnSingleTapListener(OnClickListener clickListener){
        this.mOnClickListener = clickListener;
    }

    public boolean isOnLeftBorder() {
        return isOnLeftBorder;
    }

    public boolean isOnRightBorder() {
        return isOnRightBorder;
    }



    //保存图片的边界
    class ImageState {
        private float left;
        private float top;
        private float right;
        private float bottom;

        public float getLeft() {
            return left;
        }

        public void setLeft(float left) {
            this.left = left;
        }

        public float getTop() {
            return top;
        }

        public void setTop(float top) {
            this.top = top;
        }

        public float getRight() {
            return right;
        }

        public void setRight(float right) {
            this.right = right;
        }

        public float getBottom() {
            return bottom;
        }

        public void setBottom(float bottom) {
            this.bottom = bottom;
        }
    }

}

