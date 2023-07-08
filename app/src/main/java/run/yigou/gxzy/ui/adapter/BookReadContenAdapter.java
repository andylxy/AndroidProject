package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import com.spreada.utils.chinese.ZHConverter;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.Font;
import run.yigou.gxzy.common.Language;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.utils.SpannableStringHelper;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 21:36:51
 * 包名:  run.yigou.gxzy.ui.adapter
 * 类名:  BookReadContenAdapter
 * 版本:  1.0
 * 描述:
 */
public final class BookReadContenAdapter extends AppAdapter<String> {
    private Setting mSetting;
    public BookReadContenAdapter(Context context) {
        super(context);
    }

//    @Override
//    public int getItemCount() {
//        return 1;
//    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mSetting = SysManager.getSetting();
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvErrorTips;

        private ViewHolder() {
            super(R.layout.book_chapter_content_item);

            tvTitle = findViewById(R.id.tv_title);
            tvContent = findViewById(R.id.tv_content);
            tvErrorTips = findViewById(R.id.tv_loading_error_tips);
        }

        @Override
        public void onBindView(int position) {
            String contentTest = "测试字符串";
            tvErrorTips.setVisibility(View.GONE);
            tvTitle.setText("【" + getLanguageContext(contentTest) + "】");
            if (mSetting.isDayStyle()) {
                tvTitle.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
                tvContent.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
            } else {
                tvTitle.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                tvContent.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
            }

            tvTitle.setTextSize(mSetting.getReadWordSize() + 2);
            tvContent.setTextSize(mSetting.getReadWordSize());
            tvContent.setText(contentTest);
            //viewHolderSetTvContent(chapter);

        }
        /**
         * 设置显示内容
         */
//        private void viewHolderSetTvContent( Chapter chapter) {
//            Spanned content = HtmlCompat.fromHtml(getLanguageContext(chapter.getContent()), HtmlCompat.FROM_HTML_MODE_COMPACT);
//            if (mBook.getSource() != null && mBook.getSource().equals("search")) {
//                ///updateDate字段在搜索上不使用,,显示时标记搜索关键字的颜色
//                content=  SpannableStringHelper.getSpannableString(mBook.getUpdateDate(),content.toString(),/*mContext.getColor(R.color.colorPrimaryDark)*/0);
//            }
//            tvContent.setText(content);
//        }
    }


    private String getLanguageContext(String content) {
        if (mSetting.getLanguage() == Language.traditional && mSetting.getFont() == Font.默认字体) {
            return ZHConverter.convert(content, ZHConverter.TRADITIONAL);
        }
        return content;

    }


}