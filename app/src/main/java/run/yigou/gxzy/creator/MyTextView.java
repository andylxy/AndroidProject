package run.yigou.gxzy.creator;


import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;


/**
 *  作者:  zhs
 *  时间:  2023-07-08 16:42:12
 *  包名:  run.yigou.gxzy.creator
 *  类名:  MyTextView
 *  版本:  1.0
 *  描述:
 *
*/

public class MyTextView extends /*AppCompatEditText*/ AppCompatTextView {

   /* private OnTouchListener mOnTouchListener;
    private long timeDown;
    private long timeUp;*/


    public MyTextView(Context context) {
        super(context);

    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }


   /* @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (timeUp - timeDown > 0 && timeUp - timeDown < 1000){
            return false;
        }else {
            return super.dispatchTouchEvent(event);
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            timeDown = System.currentTimeMillis();
        }else if (event.getAction() == MotionEvent.ACTION_UP){
            timeUp = System.currentTimeMillis();
        }
        return super.onTouchEvent(event);
    }*/




}

