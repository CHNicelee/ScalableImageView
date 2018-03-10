package com.ice.picture.ui.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ice.picture.R;
import com.ice.picture.adapter.MyPagerAdapter;

/**
 * Created by asd on 1/21/2017.
 */

public class MyViewPager extends ViewPager {
    private static final String TAG = "MyViewPager";

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        View view = ((MyPagerAdapter)getAdapter()).getCurrentView();
        ScalableImageView imageView = ((ScalableImageView) view.findViewById(R.id.imageView));

        //如果imageView需要滑动事件  那么不进行拦截
        if(imageView.needMotionEvent(ev))
            return false;

        //否则不改变逻辑
        return super.onInterceptTouchEvent(ev);

    }

}
