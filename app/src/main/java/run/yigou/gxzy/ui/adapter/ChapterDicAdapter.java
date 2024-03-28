package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.http.entitymodel.TitelInfo;
import run.yigou.gxzy.utils.StringHelper;

/**
 * 作者:  zhs
 * 时间:  2023-07-08 10:38:13
 * 包名:  run.yigou.gxzy.ui.adapter
 * 类名:  ChapterTitleAdapter
 * 版本:  1.0
 * 描述:
 */
public final class ChapterDicAdapter extends AppAdapter<TitelInfo> {

    private  static String showViewType = null;
    public ChapterDicAdapter(Context context) {
        super(context);
        showViewType = "0";
    }
    public ChapterDicAdapter(Context context,String viewType) {
        super(context);
        showViewType = viewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private TextView mTvChapterTitle;
        private LinearLayout mLlChapter;
        private TextView mTvTitleChapter;
        private TextView mTvTitleSection;
        private TextView mTvChapterVideoUrl;
        private TextView mTvChapterContent;



        private ViewHolder() {
            super(R.layout.book_chapter_title_item);
            mTvChapterTitle = findViewById(R.id.tv_chapter_title);
            mLlChapter = findViewById(R.id.ll_chapter);
            mTvTitleChapter = findViewById(R.id.tv_title_chapter);
            mTvTitleSection = findViewById(R.id.tv_title_section);
            mTvChapterVideoUrl = findViewById(R.id.tv_chapter_video_url);
            mTvChapterContent = findViewById(R.id.tv_chapter_content);
        }

        @Override
        public void onBindView(int position) {
            // mTvChapterTitle.setText();
            TitelInfo titelInfo = null;
            if (getData() == null) return;
            else titelInfo = getData().get(position);
            if (Objects.equals(showViewType, "0")|| StringHelper.isEmpty(showViewType)) {
                if (getData() != null) {
                    mTvChapterTitle.setText(titelInfo.getTitle());
                }
                mLlChapter.setVisibility(View.GONE);
                mTvChapterTitle.setVisibility(View.VISIBLE);
            } else {
                mLlChapter.setVisibility(View.VISIBLE);
                mTvChapterTitle.setVisibility(View.GONE);
                mTvTitleChapter .setText("条辨1");
                mTvTitleChapter .setTextColor(Color.parseColor("#F1E5E6"));
                mTvTitleChapter.setBackground(new ColorDrawable(Color.parseColor("#464648")));
                mTvTitleSection .setText("太阳症");
                mTvTitleSection .setTextColor(Color.parseColor("#F1E5E6"));
                mTvTitleSection.setBackground(new ColorDrawable(Color.parseColor("#830227")));
//                if (StringHelper.isEmpty(chapter.getMSectionVideoUrl()))
//                    mTvChapterVideoUrl.setText("视频地址");
                mTvChapterContent .setText("条辨内容......");
            }


        }
    }
}