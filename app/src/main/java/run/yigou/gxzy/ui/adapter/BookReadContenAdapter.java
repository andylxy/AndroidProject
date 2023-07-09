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

    @Override
    public int getItemCount() {
        return 1;
    }


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
            String contentTest = "Git 变基模式如何理解，针对这个问题，这篇文章详细介绍了相对应的分析和解答，希望可以帮助更多想解决这个问题的小伙伴找到更简单易行的方法。\n" +
                    "\n" +
                    "今天主要和大伙儿唠唠 GIT 的变基模式(rebase)。\n" +
                    "\n" +
                    "GIT  本身对于一些初学者理解的不是这么好，对我个人来说，一开始一些基本概念在刚接触的时候，并不能通透的理解，只有当将这些概念放到实践形成自己的理解，才能知道这个概念原来是这个意思，以及这个命令是这个样子的。\n" +
                    "\n" +
                    "1、什么是变基(rebase)\n" +
                    "\n" +
                    "变基，我们可以理解为基低的意思。就像盖楼一样，一层层的向上盖，最下面是地基，我们把盖的每一层称为基。\n" +
                    "\n" +
                    "(为了更好的理解，就拿上述盖楼为例)\n" +
                    "\n" +
                    "假如，我们的楼层盖好了，共18层，需要多个装修工去每一层进行装修。我的装修团队一共有三个人，分别为A、B、C。\n" +
                    "\n" +
                    "A 负责第一层，B 负责第二层，C 负责第三层。按照正常的逻辑，三个人谁提前装修完自己的那一层，谁就要到第四层进行装修。\n" +
                    "\n" +
                    "但是有个问题就是，假如 B 第二层也装修完毕，但是 B 不知道其他两个人的最新进度，所以需要通过某种方式，把自己当前的进度更新到最新(B  应该知道下一步该装修第几层)，才能继续在其他两人的进度基础上继续装修。";
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
            if (mOnTouchListener != null) {
                getItemView().setOnTouchListener(mOnTouchListener);
            }
            getItemView().setOnClickListener(v -> {
                if (mOnClickItemListener != null) {

                    mOnClickItemListener.onClick( getItemView(), position);

                }
            });
        }
        /**
         * 设置显示内容
         */
//        public void viewHolderSetTvContent( Chapter chapter) {
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
    private OnClickItemListener mOnClickItemListener;
    private View.OnTouchListener mOnTouchListener;

    public void setmOnClickItemListener(OnClickItemListener mOnClickItemListener) {
        this.mOnClickItemListener = mOnClickItemListener;
    }

   public void setmOnTouchListener(View.OnTouchListener mOnTouchListener) {
        this.mOnTouchListener = mOnTouchListener;
    }

    public interface OnClickItemListener {
        void onClick(View view, int positon);
    }
}