package run.yigou.gxzy.ui.adapter;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import com.spreada.utils.chinese.ZHConverter;
import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.ResultCallback;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.Font;
import run.yigou.gxzy.common.Language;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.activity.BookReadActivity;
import run.yigou.gxzy.utils.SpannableStringHelper;
import run.yigou.gxzy.utils.StringHelper;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 21:36:51
 * 包名:  run.yigou.gxzy.ui.adapter
 * 类名:  BookReadContenAdapter
 * 版本:  1.0
 * 描述:
 */
public final class BookReadContenAdapter extends AppAdapter<Chapter> {

    private BookReadActivity mBookReadActivity;

    public BookReadContenAdapter(Context context) {
        super(context);
        mBookReadActivity = (BookReadActivity) context;
    }

//    @Override
//    public int getItemCount() {
//        return 1;
//    }

    private Typeface mTypeFace;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

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
            initFont();
            Chapter chapter = getItem(position);
            tvContent.setTypeface(mTypeFace);
            tvTitle.setTypeface(mTypeFace);
            tvErrorTips.setVisibility(View.GONE);
            tvTitle.setText("【" + getLanguageContext(chapter.getTitle()) + "】");
            if (SysManager.getSetting().isDayStyle()) {
                tvTitle.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                tvContent.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
            } else {
                tvTitle.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                tvContent.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
            }
            tvTitle.setTextSize(SysManager.getSetting().getReadWordSize() + 2);
            tvContent.setTextSize(SysManager.getSetting().getReadWordSize());
            if (mOnTouchListener != null) {
                getItemView().setOnTouchListener(mOnTouchListener);
            }
            getItemView().setOnClickListener(v -> {
                if (mOnClickItemListener != null) {
                    mOnClickItemListener.onClick(getItemView(), position);
                }
            });
            if (StringHelper.isEmpty(chapter.getContent())) {
                if (mOnChapterContentListener != null) {
                    mOnChapterContentListener.getChapter(position, new ResultCallback() {
                        @Override
                        public void onFinish(Object o, int code) {
                            if (code == 200)
                                notifyItemChanged((Integer) o);
                        }
                    });
                }
            } else {
//            RichText.from(getLanguageContext(chapter.getContent())).bind(mContext)
//                    .showBorder(false)
//                    .clickable(true)
//                    .size(ImageHolder.MATCH_PARENT, ImageHolder.WRAP_CONTENT)
//                    .into( viewHolder.tvContent);
                viewHolderSetTvContent(chapter);
            }
        }

        /**
         * 设置显示内容
         */
        public void viewHolderSetTvContent(Chapter chapter) {

            String content =getLanguageContext(chapter.getContent());

            if (mBookReadActivity.mBook.getSource() != null && mBookReadActivity.mBook.getSource().equals("Search")) {
                //Book .Desc字段在搜索上不使用,,显示时标记搜索关键字的颜色
                Spanned content2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), content,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                tvContent.setText(content2);
            } else{

                tvContent.setText(HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH));}
        }

    }


    private String getLanguageContext(String content) {
        if (SysManager.getSetting().getLanguage() == Language.traditional && SysManager.getSetting().getFont() == Font.默认字体) {
            return ZHConverter.convert(content, ZHConverter.TRADITIONAL);
        }
        return content;

    }

    private void initFont() {
        if (SysManager.getSetting().getFont() == Font.默认字体) {
            mTypeFace = null;
        } else {
            mTypeFace = Typeface.createFromAsset(getContext().getAssets(), SysManager.getSetting().getFont().path);
        }
    }

    private OnClickItemListener mOnClickItemListener;
    private View.OnTouchListener mOnTouchListener;

    private OnChapterContentListener mOnChapterContentListener;

    public void setmOnClickItemListener(OnClickItemListener mOnClickItemListener) {
        this.mOnClickItemListener = mOnClickItemListener;
    }

    public void setOnChapterContentListener(OnChapterContentListener mOnChapterContentListener) {
        this.mOnChapterContentListener = mOnChapterContentListener;
    }

    public void setmOnTouchListener(View.OnTouchListener mOnTouchListener) {
        this.mOnTouchListener = mOnTouchListener;
    }

    public interface OnClickItemListener {
        void onClick(View view, int positon);
    }

    public interface OnChapterContentListener {
        void getChapter(int postion, ResultCallback callback);
    }
}