package run.yigou.gxzy.ui.tips.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.entity.GroupData;

/**
 * 弹窗Header ViewHolder
 * 用于弹窗中的组头部视图
 */
public class PopupHeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView tvHeader;

    public PopupHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        this.tvHeader = itemView.findViewById(R.id.tv_expandable_header);
    }

    /**
     * 绑定数据
     *
     * @param groupData 组数据
     */
    public void bind(@NonNull GroupData groupData) {
        if (groupData.getTitle() != null) {
            tvHeader.setText(groupData.getTitle());
        }
    }

    /**
     * 获取TextView控件
     *
     * @return TextView
     */
    public TextView getTextView() {
        return tvHeader;
    }
}
