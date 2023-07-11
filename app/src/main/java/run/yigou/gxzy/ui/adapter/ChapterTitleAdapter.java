package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.http.api.BookInfoNav;

/**
 * 作者:  zhs
 * 时间:  2023-07-08 10:38:13
 * 包名:  run.yigou.gxzy.ui.adapter
 * 类名:  ChapterTitleAdapter
 * 版本:  1.0
 * 描述:
 */
public final class ChapterTitleAdapter extends AppAdapter<Chapter> {
    private int curChapterPosition = -1;
    private Setting setting;

    public void setCurChapterPosition(int curChapterPosition) {
        this.curChapterPosition = curChapterPosition;
    }

    public ChapterTitleAdapter(Context context) {
        super(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        setting = SysManager.getSetting();
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private TextView mTvChapterTitle;

        private ViewHolder() {
            super(R.layout.book_chapter_title_item);
            mTvChapterTitle = findViewById(R.id.tv_chapter_title);
        }

        @Override
        public void onBindView(int position) {
            // mTvChapterTitle.setText();
            Chapter chapter = null;
            if (getData() != null) {
                chapter = getData().get(position);
            }
            if (chapter != null) {
                mTvChapterTitle.setText("【" + chapter.getTitle() + "】");
                if (setting.isDayStyle()) {
                    mTvChapterTitle.setTextColor(getContext().getResources().getColor(setting.getReadWordColor()));
                } else {
                    mTvChapterTitle.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
                }
                //表示当前章节正在阅读
                if (position == curChapterPosition) {
                    mTvChapterTitle.setTextColor(getContext().getResources().getColor(R.color.sys_dialog_setting_word_red));
                }
            }

        }
    }
}