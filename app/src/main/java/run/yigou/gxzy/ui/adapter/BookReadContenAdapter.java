package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.spreada.utils.chinese.ZHConverter;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.ResultCallback;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.Font;
import run.yigou.gxzy.common.Language;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.http.api.BookDetailList;
import run.yigou.gxzy.http.entitymodel.ChapterList;
import run.yigou.gxzy.http.model.HttpData;
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
            Chapter chapter = getItem(position);
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
            // tvContent.setText(contentTest);

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
            Spanned content = HtmlCompat.fromHtml(getLanguageContext(chapter.getContent()), HtmlCompat.FROM_HTML_MODE_COMPACT);
//            if (mBook.getSource() != null && mBook.getSource().equals("search")) {
//                ///updateDate字段在搜索上不使用,,显示时标记搜索关键字的颜色
//                content=  SpannableStringHelper.getSpannableString(mBook.getUpdateDate(),content.toString(),/*mContext.getColor(R.color.colorPrimaryDark)*/0);
//            }
            tvContent.setText(content);
        }
    }

//    /**
//     * 加载章节内容
//     *
//     * @param chapter
//     * @param viewHolder
//     */
//    private void getChapterContent(final Chapter chapter) {
//
//      BookStoreApi.getChapterContent(chapter.getUrl(), new ResultCallback() {
//                @Override
//                public void onFinish(final Object o, int code) {
//                    chapter.setContent((String) o);
//                    mChapterService.saveOrUpdateChapter(chapter);
//                    if (viewHolder != null) {
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                viewHolderSetTvContent(viewHolder, chapter);
//                                //viewHolder.tvContent.setText(getLanguageContext((String) o));
////                                RichText.from(getLanguageContext((String) o)).bind(mContext)
////                                        .showBorder(false)
////                                        .size(ImageHolder.MATCH_PARENT, ImageHolder.WRAP_CONTENT)
////                                        .into( viewHolder.tvContent);
//
//                                viewHolder.tvErrorTips.setVisibility(View.GONE);
//                            }
//                        });
//                    }
//                }
//
//                @Override
//                public void onError(Exception e) {
//                    if (viewHolder != null) {
//                        mHandler.sendMessage(mHandler.obtainMessage(1, viewHolder));
//                    }
//                }
//
//            });
////                EasyHttp.get(this)
////                        .api(new BookDetailList().setId(mNavItem.getId()))
////                        .request(new HttpCallback<HttpData<List<BookDetailList.Bean>>>(this) {
////                            @Override
////                            public void onSucceed(HttpData<List<BookDetailList.Bean>> data) {
////                                if (data !=null && data.getData().size() > 0){
////                                    List<BookDetailList.Bean> detailList =  data.getData();
////                                    int i = 0;
////                                    try {
////                                        for (BookDetailList.Bean bean : detailList) {
////                                            for (ChapterList chapt : bean.getChapterList()) {
////                                                Chapter chapter = new Chapter();
////                                                chapter.setNumber(i++);
////                                                chapter.setTitle(chapt.getTitle());
////                                                chapter.setUrl( chapt.getId()+"");
////                                                chapters.add(chapter);
////                                            }
////                                        }
////                                    } catch (Exception e) {
////
////                                        e.printStackTrace();
////                                    }
////                                    updateAllOldChapterData(chapters);
////                                    mHandler.sendMessage(mHandler.obtainMessage(1));
////                                }
////
////                            }
////                        });
////            }
////
////        });
//
//    }

    private String getLanguageContext(String content) {
        if (SysManager.getSetting().getLanguage() == Language.traditional && SysManager.getSetting().getFont() == Font.默认字体) {
            return ZHConverter.convert(content, ZHConverter.TRADITIONAL);
        }
        return content;

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