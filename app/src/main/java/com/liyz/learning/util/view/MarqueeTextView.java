package com.liyz.learning.util.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.core.view.GestureDetectorCompat;

import com.liyz.learning.util.R;

/**
 * <pre>
 *     author : xiaweizi
 *     class  : com.xiaweizi.marquee.MarqueeTextView
 *     e-mail : 1012126908@qq.com
 *     time   : 2017/12/26
 *     desc   : 自定义跑马灯
 * </pre>
 */

@SuppressLint("AppCompatCustomView")
public class MarqueeTextView extends TextView {

    /**
     * 默认滚动时间
     */
    private static final int ROLLING_INTERVAL_DEFAULT = 10000;
    /**
     * 默认滚动速度:单位（100像素/1000毫秒）
     */
    private static final int ROLLING_SPEED_DEFAULT = 100;
    /**
     * 第一次滚动默认延迟
     */
    private static final int FIRST_SCROLL_DELAY_DEFAULT = 1000;
    /**
     * 滚动模式-一直滚动
     */
    public static final int SCROLL_FOREVER = 100;
    /**
     * 滚动模式-只滚动一次
     */
    public static final int SCROLL_ONCE = 101;

    /**
     * 滚动器
     */
    private Scroller mScroller;
    /**
     * 滚动一次的时间
     */
    private int mRollingInterval;
    /**
     * 滚动速度
     */
    private int mRollingSpeed;
    /**
     * 滚动的初始 X 位置
     */
    private int mXPaused = 0;
    /**
     * 是否暂停
     */
    private boolean mPaused = true;
    /**
     * 是否第一次
     */
    private boolean mFirst = true;
    /**
     * 滚动模式
     */
    private int mScrollMode;
    /**
     * 初次滚动时间间隔
     */
    private int mFirstScrollDelay;
    /**
     * 传入显示的公告列表
     */
    private String[] listData;
    /**
     * 用来保存公告列表所在的位置 根据内容长度以x轴坐标计算
     */
    private int[] listDataPos;
    /**
     * 手势检测器
     */
    private GestureDetectorCompat mGestureDetector;
    private OnMarqueeItemClickListener onItemClickListener;
    private String showTextData;

    public MarqueeTextView(Context context) {
        this(context, null);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs, defStyle);
    }

    private void initView(Context context, AttributeSet attrs, int defStyle) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView);
        mRollingInterval = typedArray.getInt(R.styleable.MarqueeTextView_scroll_interval, ROLLING_INTERVAL_DEFAULT);
        mRollingSpeed = typedArray.getInt(R.styleable.MarqueeTextView_scroll_speed, ROLLING_SPEED_DEFAULT);
        mScrollMode = typedArray.getInt(R.styleable.MarqueeTextView_scroll_mode, SCROLL_FOREVER);
        mFirstScrollDelay = typedArray.getInt(R.styleable.MarqueeTextView_scroll_first_delay, FIRST_SCROLL_DELAY_DEFAULT);
        typedArray.recycle();
        setSingleLine();
        setEllipsize(null);
        // 手势监听工具
        mGestureDetector = new GestureDetectorCompat(context, gestureListener);
    }

    /**
     * 开始滚动
     */
    public void startScroll() {
        mXPaused = 0;
        mPaused = true;
        mFirst = true;
        if (onItemClickListener != null){
            String[] strings = onItemClickListener.initShowTextList();
            if (strings != null){
                setData(strings);
            }
        }
        resumeScroll();
    }

    /**
     * 继续滚动
     */
    public void resumeScroll() {
        if (!mPaused)
            return;
        // 设置水平滚动
        setHorizontallyScrolling(true);

        // 使用 LinearInterpolator 进行滚动
        if (mScroller == null) {
            mScroller = new Scroller(this.getContext(), new LinearInterpolator());
            setScroller(mScroller);
        }

        int scrollingLen = calculateScrollingLen(showTextData);
        final int distance = scrollingLen - mXPaused;
        // 滚动的时间间隔应该按照内容长短来
        // final int duration = (Double.valueOf(mRollingInterval * distance * 1.00000 / scrollingLen)).intValue();
        final int duration = (Double.valueOf(1000 * distance * 1.00000 / mRollingSpeed)).intValue();
        if (mFirst) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScroller.startScroll(mXPaused, 0, distance, 0, duration);
                    invalidate();
                    mPaused = false;
                }
            }, mFirstScrollDelay);
        } else {
            mScroller.startScroll(mXPaused, 0, distance, 0, duration);
            invalidate();
            mPaused = false;
        }
    }

    /**
     * 暂停滚动
     */
    public void pauseScroll() {
        if (null == mScroller)
            return;

        if (mPaused)
            return;

        mPaused = true;

        mXPaused = mScroller.getCurrX();

        mScroller.abortAnimation();
    }

    /**
     * 停止滚动，并回到初始位置
     */
    public void stopScroll() {
        if (null == mScroller) {
            return;
        }
        mPaused = true;
        mScroller.startScroll(0, 0, 0, 0, 0);
    }

    /**
     * 计算滚动的距离
     *
     * @return 滚动的距离
     */
    private int calculateScrollingLen(String strTxt) {
        if (TextUtils.isEmpty(strTxt)) {
            return 0;
        }
        TextPaint tp = getPaint();
        Rect rect = new Rect();
        tp.getTextBounds(strTxt, 0, strTxt.length(), rect);
        return rect.width();
    }

    /**
     * 添加显示的公告列表，并
     *
     * @param list
     */
    public void setData(String[] list) {
        this.listData = list;
        showTextData = getShowTextData();
        setText(showTextData);
    }

    /**
     * 拼接显示字符串，并计算出每个item所在的位置
     *
     * @return
     */
    private String getShowTextData() {
        if (listData != null && listData.length > 0) {
            listDataPos = new int[listData.length];
            StringBuilder showData = new StringBuilder();
            for (int i = 0; i < listData.length; i++) {
                showData.append(listData[i]);
                // 每一条后都添加空格占位符
                showData.append("\t\t\t\t");
                listDataPos[i] = calculateScrollingLen(showData.toString());
            }
            return showData.toString();
        } else {
            return getText().toString();
        }
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件交给手势处理
        mGestureDetector.onTouchEvent(event);
        return true;//继续执行后面的代码
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (null == mScroller) return;
        if (mScroller.isFinished() && (!mPaused)) {
            if (mScrollMode == SCROLL_ONCE) {
                stopScroll();
                return;
            }
            mPaused = true;
            mXPaused = -1 * getWidth();
            mFirst = false;
            this.resumeScroll();
        }
    }

    /**
     * 获取滚动一次的时间
     */
    public int getRndDuration() {
        return mRollingInterval;
    }

    /**
     * 设置滚动一次的时间
     */
    public void setRndDuration(int duration) {
        this.mRollingInterval = duration;
    }


    /**
     * 获取滚动速度
     * @return
     */
    public int getRollingSpeed() {
        return mRollingSpeed;
    }
    /**
     * 设置滚动速度
     * @return
     */
    public void setRollingSpeed(int mRollingSpeed) {
        this.mRollingSpeed = mRollingSpeed;
    }

    /**
     * 设置滚动模式
     */
    public void setScrollMode(int mode) {
        this.mScrollMode = mode;
    }

    /**
     * 获取滚动模式
     */
    public int getScrollMode() {
        return this.mScrollMode;
    }

    /**
     * 设置第一次滚动延迟
     */
    public void setScrollFirstDelay(int delay) {
        this.mFirstScrollDelay = delay;
    }

    /**
     * 获取第一次滚动延迟
     */
    public int getScrollFirstDelay() {
        return mFirstScrollDelay;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public void setOnItemClickListener(OnMarqueeItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    //GestureDetector.OnDoubleTapListener
    // 手势监听
    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // 点击事件 获取点击的位置
            if (onItemClickListener != null) {
                float clickX = mScroller.getCurrX() + e.getX();
                for (int i = 0; i < listDataPos.length; i++) {
                    if (clickX >= 0 && clickX <= listDataPos[i]) {
                        onItemClickListener.onClick(i);
                        break;
                    }
                }
            }
            return super.onSingleTapConfirmed(e);
        }
    };
    /**
     * 提供一个对外的初始化显示数据集合的方法
     *
     * @return 显示点数组
     */
    public interface OnMarqueeItemClickListener {
        void onClick(int position);
        String[] initShowTextList();
    }

}