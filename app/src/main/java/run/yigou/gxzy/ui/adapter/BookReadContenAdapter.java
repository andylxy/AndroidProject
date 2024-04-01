package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    private Typeface mTypeFace;
    private  String showViewType = null;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvErrorTips;
        private TextView mTvFangJi;
        private TextView mTvFangJiZhujie;
        private TextView mTvVideoTitle;
        private TextView mTvVideoContent;
        private TextView mTvTitleContent;

        private LinearLayout mLlShowItemOne;
        private LinearLayout mLlShowItemTwo;
        private TextView mTvSectionContentTitle;
        private TextView mTvSectionTitleNo;
        private TextView mTvSectionContent;
        private TextView mTvSectionContentNote;
        private View mVwSectionDividerLine;


        private ViewHolder() {
            super(R.layout.book_chapter_content_item);

                tvTitle = findViewById(R.id.tv_title);
                tvContent = findViewById(R.id.tv_content);
                tvErrorTips = findViewById(R.id.tv_loading_error_tips);
                mTvFangJi = findViewById(R.id.tv_FangJi);
                mTvFangJiZhujie = findViewById(R.id.tv_FangJiZhujie);
                mTvVideoTitle = findViewById(R.id.tv_video_title);
                mTvVideoContent = findViewById(R.id.tv_video_content);
                mTvTitleContent = findViewById(R.id.tv_title_content);

                mTvSectionContentTitle = findViewById(R.id.tv_section_content_title);
                mTvSectionTitleNo = findViewById(R.id.tv_section_title_no);
                mTvSectionContent = findViewById(R.id.tv_section_content);
                mTvSectionContentNote = findViewById(R.id.tv_section_content_note);
                mVwSectionDividerLine = findViewById(R.id.vw_section_divider_line);

            mLlShowItemOne = findViewById(R.id.ll_show_item_one);
            mLlShowItemTwo = findViewById(R.id.ll_show_item_two);
        }

        @Override
        public void onBindView(int position) {
            Chapter chapter = getItem(position);
            showViewType = chapter.getParentId() ==null?"0":chapter.getParentId() ;

            //初始化字体
            initFont();
            initActivity();
            if (mOnTouchListener != null) {
                getItemView().setOnTouchListener(mOnTouchListener);
            }
            getItemView().setOnClickListener(v -> {
                if (mOnClickItemListener != null) {
                    mOnClickItemListener.onClick(getItemView(), position);
                }
            });
            if (StringHelper.isEmpty(chapter.getMSection())) {
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
         * 窗口显示初始化
         */
        private void initActivity() {
            if (showViewType.equals("0")) {
                tvContent.setTypeface(mTypeFace);
                tvTitle.setTypeface(mTypeFace);
                mTvFangJi.setTypeface(mTypeFace);
                mTvFangJiZhujie.setTypeface(mTypeFace);
                // mTvVideoTitle.setTypeface(mTypeFace);
                mTvVideoContent.setTypeface(mTypeFace);
                mTvTitleContent.setTypeface(mTypeFace);
                tvErrorTips.setVisibility(View.GONE);
                mLlShowItemOne.setVisibility(View.VISIBLE);
                mLlShowItemTwo.setVisibility(View.GONE);
                if (SysManager.getSetting().isDayStyle()) {
                    tvTitle.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    tvContent.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    mTvFangJi.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    mTvFangJiZhujie.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    //  mTvVideoTitle.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    mTvVideoContent.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                    //  mTvTitleContent.setTextColor(getContext().getResources().getColor(SysManager.getSetting().getReadWordColor()));
                } else {
                    tvTitle.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    tvContent.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    mTvFangJi.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    mTvFangJiZhujie.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    //  mTvVideoTitle.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    mTvVideoContent.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                    //  mTvTitleContent.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                }
                tvTitle.setTextSize(SysManager.getSetting().getReadWordSize() + 2);
                tvContent.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvFangJi.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvFangJiZhujie.setTextSize(SysManager.getSetting().getReadWordSize());
                //mTvVideoTitle.setTextSize(SysManager.getSetting().getReadWordSize() );
                mTvVideoContent.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvTitleContent.setTextSize(SysManager.getSetting().getReadWordSize());
            } else {
                mLlShowItemOne.setVisibility(View.GONE);
                mLlShowItemTwo.setVisibility(View.VISIBLE);
                mTvSectionContentTitle.setTypeface(mTypeFace);
                mTvSectionTitleNo.setTypeface(mTypeFace);
                mTvSectionContent.setTypeface(mTypeFace);
                mTvSectionContentNote.setTypeface(mTypeFace);
                mTvSectionContentTitle.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvSectionTitleNo.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvSectionContent.setTextSize(SysManager.getSetting().getReadWordSize());
                mTvSectionContentNote.setTextSize(SysManager.getSetting().getReadWordSize());
                //注解默认隐藏
                mTvSectionContentNote.setVisibility(View.GONE);
                mVwSectionDividerLine.setVisibility(View.GONE);
                //原文补点击显示注解
                mTvSectionContent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mTvSectionContentNote.getVisibility() == View.VISIBLE) {
                            mTvSectionContentNote.setVisibility(View.GONE);
                            mVwSectionDividerLine.setVisibility(View.GONE);
                        } else {
                            mTvSectionContentNote.setVisibility(View.VISIBLE);
                            mVwSectionDividerLine.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }

        /**
         * 设置显示内容
         */
        public void viewHolderSetTvContent(Chapter chapter) {




            if (showViewType.equals("0")) {

                String mSection = getLanguageContext(chapter.getMSection());
                String mSectionNote = getLanguageContext(chapter.getMSectionNote());
                String mSectionVideoMemo = getLanguageContext(chapter.getMSectionVideoMemo());
                String mFangJi = getLanguageContext(chapter.getMFangJi());
                String mFangJiZhujie = getLanguageContext(chapter.getMFangJiZhujie());
                tvTitle.setText("【" + getLanguageContext(chapter.getTitle()) + "】");

                if (!StringHelper.isEmpty(mSection)) mSection = "[原文]\n" + mSection;
                if (!StringHelper.isEmpty(mSectionNote))
                    mSectionNote = "[原文注解]\n" + mSectionNote;
                if (!StringHelper.isEmpty(mFangJi)) mFangJi = "[方剂原文]\n" + mFangJi;
                if (!StringHelper.isEmpty(mFangJiZhujie))
                    mFangJiZhujie = "[方剂注解]\n" + mFangJiZhujie;
                if (!StringHelper.isEmpty(mSectionVideoMemo))
                    mSectionVideoMemo = "[视频实录]\n" + mSectionVideoMemo;

                Spanned mSection2;
                Spanned mSectionNote2;
                Spanned mSectionVideoMemo2;
                Spanned mFangJi2;
                Spanned mFangJiZhujie2;

                if (mBookReadActivity.mBook.getSource() != null && mBookReadActivity.mBook.getSource().equals("Search")) {
                    //Book .Desc字段在搜索上不使用,,显示时标记搜索关键字的颜色
                    mSection2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mSection,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    mSectionNote2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mSectionNote,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    mSectionVideoMemo2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mSectionVideoMemo,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    mFangJi2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mFangJi,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    mFangJiZhujie2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mFangJiZhujie,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    tvTitle.setVisibility(View.GONE);
                } else {
                    tvTitle.setVisibility(View.VISIBLE);
                    mSection2 = SpannableStringHelper.getSpannableString(mSection);
                    mSectionNote2 = SpannableStringHelper.getSpannableString(mSectionNote);
                    mSectionVideoMemo2 = SpannableStringHelper.getSpannableString(mSectionVideoMemo);
                    mFangJi2 = SpannableStringHelper.getSpannableString(mFangJi);
                    mFangJiZhujie2 = SpannableStringHelper.getSpannableString(mFangJiZhujie);
                }
                //原文
                if (StringHelper.isEmpty(mSection)) {
                    mTvTitleContent.setVisibility(View.GONE);

                } else {
                    mTvTitleContent.setVisibility(View.VISIBLE);
                    mTvTitleContent.setText(mSection2);
                }
                //原文注解
                if (StringHelper.isEmpty(mSectionNote)) {
                    tvContent.setVisibility(View.GONE);
                } else {
                    tvContent.setVisibility(View.VISIBLE);
                    tvContent.setText(mSectionNote2);
                }
                //方剂
                if (StringHelper.isEmpty(mFangJi)) {
                    mTvFangJi.setVisibility(View.GONE);
                } else {
                    mTvFangJi.setVisibility(View.VISIBLE);
                    mTvFangJi.setText(mFangJi2);
                }
                //方剂注解
                if (StringHelper.isEmpty(mFangJiZhujie)) {
                    mTvFangJiZhujie.setVisibility(View.GONE);
                } else {
                    mTvFangJiZhujie.setVisibility(View.VISIBLE);
                    mTvFangJiZhujie.setText(mFangJiZhujie2);
                }
                // 视频实录
                if (StringHelper.isEmpty(mSectionVideoMemo)) {
                    mTvVideoContent.setVisibility(View.GONE);
                    mTvVideoTitle.setVisibility(View.GONE);
                } else {
                    mTvVideoContent.setVisibility(View.VISIBLE);
                    mTvVideoTitle.setVisibility(View.VISIBLE);
                    mTvVideoTitle.setText("视频文字实录");
                    mTvVideoContent.setText(mSectionVideoMemo2);
                }

            } else {
                String mSection = getLanguageContext(chapter.getMSection());
                String mSectionNote = getLanguageContext(chapter.getMSectionNote());
                if (!StringHelper.isEmpty(mSection)) mSection = "[原文]\n" + mSection;
                if (!StringHelper.isEmpty(mSectionNote))
                    mSectionNote = "[原文注解]\n" + mSectionNote;
                Spanned mSection2;
                Spanned mSectionNote2;
                if (mBookReadActivity.mBook.getSource() != null && mBookReadActivity.mBook.getSource().equals("Search")) {
                    //Book .Desc字段在搜索上不使用,,显示时标记搜索关键字的颜色
                    mSection2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mSection,/*mContext.getColor(R.color.colorPrimaryDark)*/null);
                    mSectionNote2 = SpannableStringHelper.getSpannableString(mBookReadActivity.mBook.getDesc(), mSectionNote,/*mContext.getColor(R.color.colorPrimaryDark)*/null);

                } else {

                    mSection2 = SpannableStringHelper.getSpannableString(mSection);
                    mSectionNote2 = SpannableStringHelper.getSpannableString(mSectionNote);
                }

                mTvSectionContentTitle .setText(chapter.getTitle());

                if (chapter.getMNo() ==null) mTvSectionTitleNo .setVisibility(View.GONE);
                else {
                    mTvSectionTitleNo .setVisibility(View.VISIBLE);
                    mTvSectionTitleNo .setText(chapter.getMNo());}
                //原文
                if (StringHelper.isEmpty(mSection)) {
                    mTvSectionContent.setVisibility(View.GONE);

                } else {
                    mTvSectionContent.setVisibility(View.VISIBLE);
                    mTvSectionContent.setText(mSection2);
                    mTvSectionContentNote.setText(mSectionNote2);
                }
//                //原文注解
//                if (StringHelper.isEmpty(mSection)) {
//                    mTvSectionContentNote.setVisibility(View.GONE);
//                } else {
//                    mTvSectionContentNote.setVisibility(View.VISIBLE);
//                    mTvSectionContentNote.setText(mSection2);
//                }
            }
        }


    }


    private String getLanguageContext(String content) {
        if (StringHelper.isEmpty(content)) return null;
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