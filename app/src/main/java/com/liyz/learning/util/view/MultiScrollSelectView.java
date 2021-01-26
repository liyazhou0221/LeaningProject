package com.liyz.learning.util.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;

import com.liyz.learning.util.R;
import com.liyz.learning.util.entity.MultiScrollSelectOption;
import com.liyz.learning.util.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多个滑动选择 控件
 */
public class MultiScrollSelectView extends View {
    private String TAG = MultiScrollSelectView.class.getSimpleName();

    private boolean showLog = false;

    private final Context mContext;
    private int mCountOfOption = 0;
    private int mCountOfShow = 1;
    private int mTextSize = 14;
    private int mSelectTextSize = 15;
    private int mTextColor = Color.BLACK;
    private int mSelectTextColor = Color.BLACK;
    private int mTextPadding = 10;
    private Paint mTextPaint;
    private Paint mSelectTextPaint;
    private int mSelectBackgroundColor;
    private Paint mSelectBackgroundPaint;
    private int mSelectBackgroundColorAlpha = 100;
    private int mPerOptionWidth = 0;
    private int mTextHeight;
    private int mPerOptionHeight = 60;
    private List<MultiScrollSelectOption> mDataList;
    private Map<String, Integer> mSelectResultMap;

    private boolean mIsFirstDraw = false;
    private ArrayList<PointF> mOriginPoints;

    private int mSelectPosition = 1;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private int mMinimumFlingVelocity;
    private int mScaledTouchSlop;
    private int mScrollPosition = -1;
    private int mScrollDuration = 250;
    private SelectChangeListener changeListener;
    // 显示分割线
    private boolean showDividerLine;
    private int dividingLineColor = Color.BLACK;
    private Paint mDividerLinePaint;
    private int dpHeight;
    private int dividingLineWidth = 1;
    private int optionIntervalWidth = 0;
    private int observerPosition = -1;
    private Map<Integer,ObserverPositionListener> observerListMap ;
    private Map<Integer, Integer> observedPositionMap ;
    private ObserverPositionListener changeChainListener;
    private boolean defaultValue;
    private boolean showSelectBackground;
    // 选中字体加粗
    private boolean isSelectTextTypeBold;

    public MultiScrollSelectView(Context context) {
        this(context,null);
    }

    public MultiScrollSelectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MultiScrollSelectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initAttr(attrs);
        initScroll();
        initPaint();
        initSize();
        initData();
    }

    private void initScroll() {
        mGestureDetector = new GestureDetectorCompat(mContext, mGestureListener);
        mScroller = new OverScroller(mContext, new FastOutLinearInInterpolator());

        mMinimumFlingVelocity = ViewConfiguration.get(mContext).getScaledMinimumFlingVelocity();
        mScaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
    }

    private void initData() {
        if (mCountOfShow <= 0){
            mSelectPosition = 1;
            mCountOfShow = 1;
        }else{
            // 从零开始 ：  Math.floor 向下取值
            mSelectPosition = (int) Math.floor(mCountOfShow / 2);
        }
        mOriginPoints = new ArrayList<>();
        mOriginPoints.add(new PointF(0,0));

        mSelectResultMap = new HashMap<>();
        mSelectResultMap.put("option" + 0,mSelectPosition);
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray a = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.MultiScrollSelectView, 0, 0);
        try {
            mCountOfOption = a.getInteger(R.styleable.MultiScrollSelectView_countOfOption, mCountOfOption);
            mCountOfShow = a.getInteger(R.styleable.MultiScrollSelectView_countOfShow, mCountOfShow);
            mSelectBackgroundColorAlpha = a.getInteger(R.styleable.MultiScrollSelectView_selectBackgroundColorAlpha, mSelectBackgroundColorAlpha);

            mTextSize = a.getDimensionPixelSize(R.styleable.MultiScrollSelectView_unSelectTextSize, getTextSize(mTextSize));
            mSelectTextSize = a.getDimensionPixelSize(R.styleable.MultiScrollSelectView_selectTextSize, getTextSize(mSelectTextSize));
            mTextPadding = a.getDimensionPixelSize(R.styleable.MultiScrollSelectView_textPadding, getHeight(mTextPadding));

            mTextColor = a.getColor(R.styleable.MultiScrollSelectView_unSelectTextColor, mTextColor);
            mSelectTextColor = a.getColor(R.styleable.MultiScrollSelectView_selectTextColor, mSelectTextColor);
            mSelectBackgroundColor = a.getColor(R.styleable.MultiScrollSelectView_selectBackgroundColor, Color.WHITE);

            showDividerLine = a.getBoolean(R.styleable.MultiScrollSelectView_showDividerLine, false);
            showSelectBackground = a.getBoolean(R.styleable.MultiScrollSelectView_showSelectBackground, false);
            isSelectTextTypeBold = a.getBoolean(R.styleable.MultiScrollSelectView_selectTextTypeBold, false);
            dividingLineColor = a.getColor(R.styleable.MultiScrollSelectView_dividingLineColor, Color.GRAY);
            dividingLineWidth = a.getDimensionPixelSize(R.styleable.MultiScrollSelectView_dividingLineWidth, getHeight(dividingLineWidth));

            optionIntervalWidth = a.getDimensionPixelSize(R.styleable.MultiScrollSelectView_optionIntervalWidth, getHeight(optionIntervalWidth));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }
    }

    private void initPaint() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mTextSize);

        mSelectTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectTextPaint.setColor(mSelectTextColor);
        if (isSelectTextTypeBold){
            mSelectTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        mSelectTextPaint.setTextSize(mSelectTextSize);
        mSelectTextPaint.setTextAlign(Paint.Align.CENTER);

        mSelectBackgroundPaint = new Paint();
        mSelectBackgroundPaint.setColor(mSelectBackgroundColor);
        mSelectBackgroundPaint.setAlpha(mSelectBackgroundColorAlpha);

        mDividerLinePaint = new Paint();
        mDividerLinePaint.setColor(dividingLineColor);
        mDividerLinePaint.setAlpha(mSelectBackgroundColorAlpha);
        mDividerLinePaint.setStrokeWidth(dividingLineWidth);
    }

    private void initSize() {
        Rect rect = new Rect();
        mTextPaint.getTextBounds("周一", 0, "周一".length(), rect);
        mTextHeight =  rect.height();
        // 注意：这里获取不到宽度，需要在onDraw方法中获取
        // mPerOptionWidth = getWidth() / mCountOfOption ;
        mPerOptionHeight = mTextPadding * 2 + mTextHeight;
        dpHeight = getHeight(1);
    }

    public void setData(List<List<String>> lists){
        if (lists == null){
            Logger.e(TAG,"setData lists == null");
            return;
        }
        List<MultiScrollSelectOption> mDataList = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            MultiScrollSelectOption option = new MultiScrollSelectOption(lists.get(i),"");
            mDataList.add(option);
        }
        setOptionData(mDataList);
    }

    public void setOptionData(List<MultiScrollSelectOption> mDataList) {
        logger("setOptionData 设置选择列表数据");
        if (mDataList == null){
            Logger.e(TAG,"setOptionData lists == null");
            return;
        }
        defaultValue = false;
        this.mDataList = mDataList;
        initOptionRelationContent();
        // 重绘
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        logger("onFocusChanged gainFocus = " + gainFocus);
        if (gainFocus){
            initOptionRelationContent();
        }
    }

    /**
     * 初始化选项关联的内容
     */
    private void initOptionRelationContent() {
        if (mDataList == null){
            return;
        }
        logger("initOptionRelationContent 初始化选项关联的内容");
        mIsFirstDraw = true;
        mOriginPoints.clear();
        mSelectResultMap.clear();
        // 第一次获取不到值，需要在onDraw方法中重新调用
        if (mDataList.size() != 0 && getMeasuredWidth() != 0){
            mPerOptionWidth = getMeasuredWidth() / mDataList.size();
        }

        // logger("initOptionRelationContent mPerOptionWidth = " + mPerOptionWidth);
        if (mPerOptionWidth == 0){
            return;
        }
        for (int i = 0; i < mDataList.size(); i++) {
            // 设置的有默认选中位置
            if (mDataList.get(i).getSelectPosition() >=0){
                mOriginPoints.add(new PointF(i * mPerOptionWidth,mPerOptionHeight * (mSelectPosition - mDataList.get(i).getSelectPosition())));
                mSelectResultMap.put("option" + i,mDataList.get(i).getSelectPosition());
            }
            // 最后一项时，第一个内容设置为选中
            else if (i == mDataList.size() -1){
                mOriginPoints.add(new PointF(i * mPerOptionWidth,mSelectPosition * mPerOptionHeight));
                mSelectResultMap.put("option" + i,0);
            }else{
                mOriginPoints.add(new PointF(i * mPerOptionWidth,0));
                mSelectResultMap.put("option" + i,mSelectPosition);
            }
        }
    }

    /**
     * 设置选择项
     * @param selectResultMap
     */
    public void setSelectResultMap(Map<String, Integer> selectResultMap){
        if (mSelectResultMap != null){
            this.mSelectResultMap = selectResultMap;
            mOriginPoints.clear();
            // 重置选择项则需要重置y坐标值
            for (int i = 0; i < mDataList.size(); i++) {
                int select = mSelectResultMap.get("option" + i);
                mDataList.get(i).setPosition(select);
                if (mDataList.get(i).getSelectPosition() >= 0){
                    mOriginPoints.add(new PointF(i * mPerOptionWidth,mPerOptionHeight * (mSelectPosition - mDataList.get(i).getSelectPosition())));
                }
            }
            invalidate();
        }
    }
    public Map<String, Integer> getSelectResultMap(){
        if (mSelectResultMap != null){
            return  mSelectResultMap;
        }else{
            mSelectResultMap = new HashMap<>();
        }
        return mSelectResultMap;
    }


    public interface SelectChangeListener{
        void onSelectChange(Map<String, Integer> resultMap);
    }
    // 处理需要级联的选项
    public interface ObserverPositionListener{
        MultiScrollSelectOption resetOption(MultiScrollSelectOption option, Map<String, Integer> mSelectResultMap);
    }

    /**
     * 设置选择监听
     * @param changeListener
     */
    public void setSelectChangeListener(SelectChangeListener changeListener){
        this.changeListener = changeListener;
    }

    public void setObserverPositionListener(int observerPosition,int observedPosition,ObserverPositionListener changeChainListener){
        if (observerListMap == null){
            observerListMap = new HashMap<>();
            observedPositionMap = new HashMap<>();
        }
        observedPositionMap.put(observerPosition,observedPosition);
        observerListMap.put(observerPosition,changeChainListener);
    }

    private List<MultiScrollSelectOption> getListData(){
        if (mDataList == null){
            mDataList = new ArrayList<>();
            List<String> options = new ArrayList<>();
            options.add("--");
            options.add("--");
            options.add("--");
            MultiScrollSelectOption option = new MultiScrollSelectOption(options,"");
            mDataList.add(option);
            // 默认值
            logger("mDataList == null 设置为默认值-- -- -- ");
            defaultValue = true;
        }
        return mDataList;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOption(canvas);
    }

    private void drawOption(Canvas canvas) {
        logger("drawOption");
        List<MultiScrollSelectOption> listData = getListData();
        // 需要在这里获取到宽度
        if (mPerOptionWidth == 0){
            initOptionRelationContent();
            // logger("drawOption mPerOptionWidth = " + mPerOptionWidth);
        }
        // 规划区域 先画出选中的背景区域
        if (showDividerLine){
            canvas.drawLine(getHeight(10),
                    mSelectPosition * mPerOptionHeight + dividingLineWidth,
                    getWidth() - getHeight(10),
                    mSelectPosition * mPerOptionHeight + dividingLineWidth,mDividerLinePaint);
            canvas.drawLine(getHeight(10),
                    mSelectPosition * mPerOptionHeight + mPerOptionHeight - dividingLineWidth,
                    getWidth() - getHeight(10),
                    mSelectPosition * mPerOptionHeight + mPerOptionHeight - dividingLineWidth,mDividerLinePaint);
        }else if (showSelectBackground){
            canvas.drawRect(0,mSelectPosition * mPerOptionHeight ,getWidth(),mSelectPosition * mPerOptionHeight + mPerOptionHeight,mSelectBackgroundPaint);
        }

        int size = listData.size();
        for (int i = 0; i < size; i++) {
            MultiScrollSelectOption option = listData.get(i);
            int optionLength = option.getSize();
            PointF pointF = mOriginPoints.get(i);

            logger("drawOption 第几列--共几行数据--初始点y坐标值 = " + i +"--" + optionLength +" x-y = "+ pointF.x + "--" + pointF.y);
            // x轴是不改变的，这里只考虑 y 轴滑动的最大 和 最小值
            // 初始点的Y坐标不能下移：最大值为 向下移动到选中的行。如：选中行为1（第二行），距离顶部的距离 pointF.y = 行高 * 1
            if (pointF.y > mPerOptionHeight * mSelectPosition){
                pointF.y = mPerOptionHeight * mSelectPosition;
                //logger("超出最大值，重置最大值 i = " + i + "  pointF.y = " + pointF.y);
            }
            // y坐标有最小值：列的最后一项移动到选中行。如：选中行为1（第二行），距离顶部的距离pointF.y = -（显示的总行数） * 行高 - view高度
            int min = -(mPerOptionHeight * (optionLength + mCountOfShow - mSelectPosition - 1) - getHeight());
            if (pointF.y < min){
                pointF.y = min;
                //logger("超出最小值，重置最小值 i = " + i + "  pointF.y = " + pointF.y);
            }
            int select = mSelectResultMap.get("option" + i);
            /*// 暂不优化，滚动的时候由于没有画完，会出现空白
              // 这里需要优化 不画出所有的内容，只画出比显示的上下各多出一个内容
            int startIndex = select - mSelectPosition - 1 ;
            if (startIndex < option.getStart()) startIndex = option.getStart();
            int endIndex = select + mSelectPosition + 1;
            if (endIndex >= optionLength) endIndex = optionLength;*/
            for (int j = 0; j < optionLength; j++) {
                canvas.drawText(option.getShowText(j) ,
                        pointF.x + mPerOptionWidth / 2 ,
                        pointF.y + mPerOptionHeight * j + (mPerOptionHeight + mTextHeight) / 2,
                        select == j  ? mSelectTextPaint : mTextPaint);
            }
        }
        // 设置完选择项后 第一次绘制，返回选择的内容
        if (mIsFirstDraw && !defaultValue){
            mIsFirstDraw = false;
            changeListener.onSelectChange(mSelectResultMap);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int with = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        switch (MeasureSpec.getMode(heightMeasureSpec)){
            case MeasureSpec.EXACTLY:
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                height = getPaddingBottom() + getPaddingEnd() + mCountOfShow * mPerOptionHeight;
                break;
        }
        setMeasuredDimension(with,height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        logger("onTouchEvent event.getAction() = " + event.getAction());
        boolean val = mGestureDetector.onTouchEvent(event);
        // 子控件消费滑动事件
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        // 手势抬起，暂停滑动
        if (event.getAction() == MotionEvent.ACTION_UP && mCurrentFlingDirection == Direction.NONE) {
            logger("onTouchEvent 手势抬起，暂停滑动 mCurrentScrollDirection = " + mCurrentScrollDirection);
            if (mCurrentScrollDirection == Direction.VERTICAL) {
                logger("onTouchEvent 滑动到最近的日期");
                goToNearestOrigin();
            }
            mCurrentScrollDirection = Direction.NONE;
        }
        return val;
    }

    /**
     * 滚动到最近的点，这里有个触发条件（Fling被触发时）--mCurrentFlingDirection != Direction.NONE
     */
    private void goToNearestOrigin(){
        PointF p = mOriginPoints.get(mScrollPosition);
        double changeNum = p.y / mPerOptionHeight;

        if (mCurrentFlingDirection != Direction.NONE) {
            // snap to nearest day 四舍五入
            changeNum = Math.round(changeNum);
        } else if (mCurrentScrollDirection == Direction.LEFT) {
            // snap to last day 向下取整
            changeNum = Math.floor(changeNum);
        } else if (mCurrentScrollDirection == Direction.RIGHT) {
            // snap to next day 向上取整
             changeNum = Math.ceil(changeNum);
        } else {
            // snap to nearest day
            changeNum = Math.round(changeNum);
        }

        int nearestOrigin = (int) (p.y - changeNum * mPerOptionHeight);

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller.forceFinished(true);
            // Snap to date. int startX, int startY, int dx, int dy, int duration
            mScroller.startScroll((int) p.x, (int) p.y, 0, - nearestOrigin, (int) (Math.abs(nearestOrigin) / mPerOptionHeight * mScrollDuration));
            ViewCompat.postInvalidateOnAnimation(MultiScrollSelectView.this);
        }else{
            // 这里需要重绘
            invalidate();
        }
        // Reset scrolling and fling direction.
        mCurrentScrollDirection = mCurrentFlingDirection = Direction.NONE;

        logger("goToNearestOrigin 最近的行 = " + (- changeNum + mSelectPosition) + " 所在的列  = " + mScrollPosition);
        // 保存选中项
        int position = (int) (-changeNum + mSelectPosition);
        mSelectResultMap.put("option" + mScrollPosition,position);
        // 更新选中位置
        mDataList.get(mScrollPosition).setPosition(position);
        // 将变化内容返回
        if (!defaultValue){
            changeListener.onSelectChange(mSelectResultMap);
        }
        // 重置需要改变的列表
        resetChangeOption();
    }

    /**
     * 重置需要改变的列表
     */
    private void resetChangeOption() {
        if (observerListMap != null && observerListMap.get(mScrollPosition) != null){
            logger("resetChangeOption 重置option数据 position = " + mScrollPosition);
            observerListMap.get(mScrollPosition).resetOption(mDataList.get(observedPositionMap.get(mScrollPosition)),mSelectResultMap);
            // 重置相关属性
            initOptionRelationContent();
            // 数据更新后重绘
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        logger("computeScroll");
        if (mScroller.isFinished()) {
            logger("computeScroll mScroller.isFinished() mCurrentFlingDirection = " + mCurrentFlingDirection);
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                logger("computeScroll mScroller.isFinished() goToNearestOrigin 滑动到最近的日期");
                goToNearestOrigin();
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                logger("computeScroll forceFinishScroll 滑动到最近的日期");
                goToNearestOrigin();
            } else if (mScroller.computeScrollOffset()) {
                //logger("computeScroll mScroller.computeScrollOffset() “滚动” 未结束！");
                logger("更新坐标值 mScroller.getCurrY() = " + mScroller.getCurrY());
                // 计算currX，currY,并检测是否已完成“滚动”
                mOriginPoints.get(mScrollPosition).y = mScroller.getCurrY();
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private boolean forceFinishScroll() {
        logger("forceFinishScroll");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // current velocity only available since api 14
            return mScroller.getCurrVelocity() <= mMinimumFlingVelocity;
        } else {
            return false;
        }
    }

    private enum Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    Direction mCurrentScrollDirection = Direction.NONE;
    Direction mCurrentFlingDirection = Direction.NONE;

    /**
     * 手势监听
     */
    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener(){

        @Override
        public boolean onDown(MotionEvent e) {
            logger("mGestureListener onDown");
            // 需要检查是触发的那一列，并修改y方向的值
            for (int i = 0; i < mOriginPoints.size(); i++) {
                if (e.getX() <= mPerOptionWidth * (i + 1)){
                    mScrollPosition = i;
                    break;
                }
            }

            if (mScrollPosition < 0){
                mScrollPosition = 0;
            }
            logger("mGestureListener onDown 选中的滑动列为 mScrollPosition = " + mScrollPosition);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            logger("mGestureListener onScroll");
            boolean isHorizontal = Math.abs(distanceX) > Math.abs(distanceY);
            // 计算滚动方向
            switch (mCurrentScrollDirection) {
                case NONE: {
                    // Allow scrolling only in one direction.
                    if (isHorizontal) {
                        if (distanceX > 0) {
                            mCurrentScrollDirection = Direction.LEFT;
                        } else {
                            mCurrentScrollDirection = Direction.RIGHT;
                        }
                    } else {
                        mCurrentScrollDirection = Direction.VERTICAL;
                    }
                    break;
                }
                case LEFT: {
                    // Change direction if there was enough change.
                    if (isHorizontal && (distanceX < -mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.RIGHT;
                    }
                    break;
                }
                case RIGHT: {
                    // Change direction if there was enough change.
                    if (isHorizontal && (distanceX > mScaledTouchSlop)) {
                        mCurrentScrollDirection = Direction.LEFT;
                    }
                    break;
                }
            }

            // 计算滚动结束后的初始点位置
            switch (mCurrentScrollDirection) {
                case LEFT:
                case RIGHT://左右滑动
                    break;
                case VERTICAL:// 上下滑动
                    mOriginPoints.get(mScrollPosition).y -= distanceY;
                    ViewCompat.postInvalidateOnAnimation(MultiScrollSelectView.this);
                    break;
            }
            logger("mGestureListener onScroll 滚动方向为 = " + mCurrentScrollDirection);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            logger("mGestureListener onFling");

            mScroller.forceFinished(true);

            mCurrentFlingDirection = mCurrentScrollDirection;
            switch (mCurrentFlingDirection) {
                case LEFT:
                case RIGHT:
                    break;
                case VERTICAL:
                    // 根据初始点的位置判断触发的列，并修改Y方向的值
                    // int startX, int startY, int velocityX, int velocityY,int minX, int maxX, int minY, int maxY
                    PointF p = mOriginPoints.get(mScrollPosition);
                    mScroller.fling(
                            (int)  p.x, (int)  p.y,
                            0, (int) velocityY,
                            Integer.MIN_VALUE, Integer.MAX_VALUE,
                            -(mPerOptionHeight * (mDataList.get(mScrollPosition).getSize() + mCountOfShow - mSelectPosition - 1) - getHeight()), mSelectPosition * mPerOptionHeight
                    );
                    break;
            }

            ViewCompat.postInvalidateOnAnimation(MultiScrollSelectView.this);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            logger("mGestureListener onSingleTapConfirmed");
            return super.onSingleTapConfirmed(e);
        }
    };

    private int getTextSize(int defaultSize){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, defaultSize, mContext.getResources().getDisplayMetrics());
    }
    private int getHeight(int defaultSize){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, defaultSize, mContext.getResources().getDisplayMetrics());
    }

    /**
     * 打印日志 
     */
    private void logger(String content){
        if (showLog){
            Logger.e(TAG,content);
        }
    }
}
