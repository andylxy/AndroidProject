package run.yigou.gxzy.ui.dividerItemdecoration;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class CustomDividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint;
    private final int height;

    public CustomDividerItemDecoration() {

        this.paint = new Paint();
        this.paint.setColor(Color.parseColor("#F4F4F4")); //  默认自定义分隔线颜色
        this.height = 2; // 默认自定义分隔线高度
    }

    public CustomDividerItemDecoration(int color, int height) {
        this.paint = new Paint();
        this.paint.setColor(color);
        this.height = height;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        for (int i = 0; i < parent.getChildCount() - 1; i++) {
            View child = parent.getChildAt(i);
            int top = child.getBottom();
            int bottom = top + height;

            c.drawRect(left, top, right, bottom, paint);
        }
    }
}
