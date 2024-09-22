package run.yigou.gxzy.ui.tips.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class ArrowView extends View {
    public static int DOWN = 1;
    public static int UP;
    private int border;
    private int direction;
    private Paint paint;

    public void setDirection(int i) {
        this.direction = i;
    }

    public ArrowView(Context context) {
        super(context);
        this.direction = UP;
        this.paint = new Paint();
        this.border = 50;
        initPaint();
    }

    public ArrowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.direction = UP;
        this.paint = new Paint();
        this.border = 50;
        initPaint();
    }

    public ArrowView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.direction = UP;
        this.paint = new Paint();
        this.border = 50;
        initPaint();
    }

    protected void initPaint() {
        this.paint.setAntiAlias(true);
        this.paint.setColor(-1);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.paint.setStrokeWidth(1.0f);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.border = Math.min(getWidth(), getHeight());
        Path path = new Path();
        if (this.direction == UP) {
            path.moveTo(0.0f, this.border);
            path.lineTo(this.border / 2, 0.0f);
            path.lineTo(this.border, this.border);
            path.close();
        } else {
            path.moveTo(0.0f, 0.0f);
            path.lineTo(this.border / 2, this.border);
            path.lineTo(this.border, 0.0f);
            path.close();
        }
        canvas.drawPath(path, this.paint);
        invalidate();
    }
}
