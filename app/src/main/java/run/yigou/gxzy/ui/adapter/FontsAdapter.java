package run.yigou.gxzy.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.common.Font;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/11/05
 *    desc   : 可进行拷贝的副本
 */
public final class FontsAdapter extends AppAdapter<Font> {

    public FontsAdapter(Context context) {
        super(context);
    }
    private Map<Font, Typeface> mTypefaceMap;
    private Setting setting;
//    @Override
//    public int getItemCount() {
//        return 10;
//    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mTypefaceMap = new HashMap<>();
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        TextView tvExample;
        TextView tvFontName;
        Button btnFontUse;
        private ViewHolder() {
            super(R.layout.font_item);
            setting =    SysManager.getSetting();
            tvFontName =findViewById(R.id.tv_font_name);
            btnFontUse =findViewById(R.id.btn_font_use);
            tvExample = findViewById(R.id.tv_font_example);
        }

        @Override
        public void onBindView(int position) {
            final Font font = getItem(position);
            Typeface typeFace = null;
            if (font != Font.默认字体) {
                if (!mTypefaceMap.containsKey(font)){
                    typeFace = Typeface.createFromAsset(getContext().getAssets(), font.path);
                    mTypefaceMap.put(font,typeFace);
                }else {
                    typeFace = mTypefaceMap.get(font);
                }
            }
            tvExample.setTypeface(typeFace);
            tvFontName.setText(font.toString());

            if ( setting.getFont() == font){
                btnFontUse.setBackgroundResource(R.drawable.font_using_btn_bg);
                btnFontUse.setTextColor(getContext().getResources().getColor(R.color.sys_font_using_btn));
                btnFontUse.setText(getContext().getString(R.string.font_using));
                btnFontUse.setOnClickListener(null);
            }else {
                btnFontUse.setBackgroundResource(R.drawable.font_use_btn_bg);
                btnFontUse.setTextColor(getContext().getResources().getColor(R.color.sys_font_use_btn));
                btnFontUse.setText(getContext().getString(R.string.font_use));
               btnFontUse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setting.setFont(font);
                        SysManager.saveSetting(setting);
                        setting= SysManager.getSetting();
                        notifyDataSetChanged();
                        Intent intent = new Intent();
                        intent.putExtra(APPCONST.FONT,font);
                        ((Activity)getContext()).setResult(Activity.RESULT_OK,intent);
                    }
                });
            }
        }
    }
}