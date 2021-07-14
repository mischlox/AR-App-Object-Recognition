package hs.aalen.arora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.Image;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Mask for the image preview
 * This code is based on https://www.programmersought.com/article/87413880631/
 *
 * @author Michael Schlosser
 */
public class FocusBoxImage extends ImageView {
    private final Context context;
    private int[] focusBoxLocation;
    public FocusBoxImage(Context context) {this(context,null);}
    public FocusBoxImage(Context context, AttributeSet attributeSet) {this(context,attributeSet,0);}
    public FocusBoxImage(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context,attributeSet, defStyleAttr);
        this.context = context;
        initView();
    }

    //Set the translucent background color
    private void initView(){setBackgroundColor(Color.parseColor("#7f000000"));}

    public void setFocusBoxLocation(int[] location){
        this.focusBoxLocation=location;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        //Draw a rectangular frame
        Paint paintrect=new Paint(Paint.ANTI_ALIAS_FLAG);
        PorterDuffXfermode porterDuffXfermode=new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
        paintrect.setXfermode(porterDuffXfermode);
        paintrect.setAntiAlias(true);
        canvas.drawRect(focusBoxLocation[0], focusBoxLocation[1], focusBoxLocation[2], focusBoxLocation[3], paintrect);
    }
}
