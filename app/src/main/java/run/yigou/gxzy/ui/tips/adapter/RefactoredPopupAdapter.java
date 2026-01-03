package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import run.yigou.gxzy.other.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 重构后的弹窗适配器
 * 使用新架构的GroupData + ItemData数据结构
 * 简化版适配器，专门用于弹窗场景
 */
public class RefactoredPopupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHILD = 1;

    private final Context context;
    private List<GroupData> groupDataList;
    private List<List<ItemData>> itemDataList;

    // 位置映射缓存
    private final List<PositionMapping> positionMappings = new ArrayList<>();

    /**
     * 位置映射类
     * 用于将RecyclerView的position映射到GroupData+ItemData
     */
    private static class PositionMapping {
        final int groupPosition;
        final int childPosition; // -1表示Header
        final int viewType;

        PositionMapping(int groupPosition, int childPosition, int viewType) {
            this.groupPosition = groupPosition;
            this.childPosition = childPosition;
            this.viewType = viewType;
        }
    }

    public RefactoredPopupAdapter(@NonNull Context context,
                                    @NonNull List<GroupData> groupDataList,
                                    @NonNull List<List<ItemData>> itemDataList) {
        this.context = context;
        this.groupDataList = new ArrayList<>(groupDataList);
        this.itemDataList = new ArrayList<>(itemDataList);
        buildPositionMappings();
    }

    /**
     * 构建位置映射
     * Header和Child交替排列
     */
    private void buildPositionMappings() {
        positionMappings.clear();

        for (int groupIndex = 0; groupIndex < groupDataList.size(); groupIndex++) {
            // 添加Header
            positionMappings.add(new PositionMapping(groupIndex, -1, VIEW_TYPE_HEADER));

            // 添加Children
            List<ItemData> items = itemDataList.get(groupIndex);
            for (int childIndex = 0; childIndex < items.size(); childIndex++) {
                positionMappings.add(new PositionMapping(groupIndex, childIndex, VIEW_TYPE_CHILD));
            }
        }

        EasyLog.print("=== RefactoredPopupAdapter.buildPositionMappings() ===");
        EasyLog.print("总映射数量: " + positionMappings.size());
        EasyLog.print("Groups: " + groupDataList.size());
    }

    /**
     * 更新数据
     */
    public void updateData(@NonNull List<GroupData> groupDataList,
                            @NonNull List<List<ItemData>> itemDataList) {
        this.groupDataList = new ArrayList<>(groupDataList);
        this.itemDataList = new ArrayList<>(itemDataList);
        buildPositionMappings();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return positionMappings.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.adapter_expandable_header, parent, false);
            return new PopupHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.adapter_child, parent, false);
            return new PopupChildViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PositionMapping mapping = positionMappings.get(position);

        if (holder instanceof PopupHeaderViewHolder) {
            GroupData groupData = groupDataList.get(mapping.groupPosition);
            ((PopupHeaderViewHolder) holder).bind(groupData);
        } else if (holder instanceof PopupChildViewHolder) {
            ItemData itemData = itemDataList.get(mapping.groupPosition).get(mapping.childPosition);
            ((PopupChildViewHolder) holder).bind(itemData);
        }
    }

    @Override
    public int getItemCount() {
        return positionMappings.size();
    }

    /**
     * Child ViewHolder
     */
    private static class PopupChildViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSectionText;
        private final TextView tvSectionNote;
        private final TextView tvSectionVideo;

        public PopupChildViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvSectionText = itemView.findViewById(R.id.tv_sectiontext);
            this.tvSectionNote = itemView.findViewById(R.id.tv_sectionnote);
            this.tvSectionVideo = itemView.findViewById(R.id.tv_sectionvideo);
        }

        public void bind(@NonNull ItemData itemData) {
            EasyLog.print("=== PopupChildViewHolder.bind() ===");
            EasyLog.print("ItemData groupPosition: " + itemData.getGroupPosition());
            
            // 绑定文本
            SpannableStringBuilder text = itemData.getAttributedText();
            if (text != null && text.length() > 0) {
                EasyLog.print("✅ 绑定文本, 长度: " + text.length() + 
                        ", 内容: " + text.toString().substring(0, Math.min(50, text.length())));
                tvSectionText.setText(text);
                tvSectionText.setMovementMethod(LocalLinkMovementMethod.getInstance());
                tvSectionText.setVisibility(View.VISIBLE);
            } else {
                EasyLog.print("⚠️ 文本为空，隐藏tvSectionText");
                tvSectionText.setVisibility(View.GONE);
            }

            // 绑定注释
            SpannableStringBuilder note = itemData.getAttributedNote();
            if (note != null && note.length() > 0) {
                EasyLog.print("✅ 绑定注释, 长度: " + note.length());
                tvSectionNote.setText(note);
                tvSectionNote.setMovementMethod(LocalLinkMovementMethod.getInstance());
                tvSectionNote.setVisibility(View.VISIBLE);
            } else {
                tvSectionNote.setVisibility(View.GONE);
            }

            // 绑定视频
            SpannableStringBuilder video = itemData.getAttributedVideo();
            if (video != null && video.length() > 0) {
                EasyLog.print("✅ 绑定视频, 长度: " + video.length());
                tvSectionVideo.setText(video);
                tvSectionVideo.setMovementMethod(LocalLinkMovementMethod.getInstance());
                tvSectionVideo.setVisibility(View.VISIBLE);
            } else {
                tvSectionVideo.setVisibility(View.GONE);
            }
            
            EasyLog.print("bind() 完成");
        }
    }
}
