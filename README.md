# ScalableImageView
可移动、可缩放的ImageView，可简单解决滑动冲突

# 效果图
[点此查看效果图](https://upload-images.jianshu.io/upload_images/4774781-363d2d15a578b195.gif)

目前只支持双击放大，不支持双指缩放。

# 使用
将ScalableImageView复制到项目中，然后引用到xml中：
xml:
```
    <com.ice.picture.ui.widget.ScalableImageView
        android:scaleType="matrix"
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

如果要与滑动控件嵌套，要重写滑动控件的方法，才能解决滑动冲突：
```
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
```
