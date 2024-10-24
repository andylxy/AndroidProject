package run.yigou.gxzy.ui.tips.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class TipsArrowView extends View {
    public static final int DOWN = 1;
    public static final int UP = 0;
    private static final int DEFAULT_BORDER = 50;
    private static final int DEFAULT_COLOR = -1; // 白色

    private int border;
    private int direction;
    private Paint paint;
    private Path path;

    public void setDirection(int direction) {
        this.direction = direction;
        invalidate(); // 重新绘制以反映方向变化
    }

    public TipsArrowView(Context context) {
        this(context, null);
    }

    public TipsArrowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TipsArrowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.direction = UP;
        this.border = DEFAULT_BORDER;
        initPaint();
        initPath();
    }

    private void initPaint() {
        try {
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            this.paint.setColor(DEFAULT_COLOR);
            this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
            this.paint.setStrokeWidth(1.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPath() {
        this.path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        this.border = Math.max(Math.min(width, height), 1); // 确保 border 不为 0

        this.path.reset();
        if (this.direction == UP) {
            this.path.moveTo(0.0f, this.border);
            this.path.lineTo((float) this.border / 2, 0.0f);
            this.path.lineTo(this.border, this.border);
        } else {
            this.path.moveTo(0.0f, 0.0f);
            this.path.lineTo((float) this.border / 2, this.border);
            this.path.lineTo(this.border, 0.0f);
        }
        this.path.close();

        canvas.drawPath(this.path, this.paint);
    }
}
