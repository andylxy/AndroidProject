package run.yigou.gxzy.ui.tips.utils;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;
import run.yigou.gxzy.ui.tips.entity.ItemData;

/**
 * 弹窗数据适配器
 * 用于将旧架构的GroupEntity数据转换为新架构的GroupData+ItemData
 */
public class PopupDataAdapter {

    /**
     * 将GroupEntity列表转换为GroupData和ItemData列表
     *
     * @param groups 旧架构的GroupEntity列表
     * @return Pair<GroupData列表, ItemData二维列表>
     */
    public static Pair<List<GroupData>, List<List<ItemData>>> convert(ArrayList<GroupEntity> groups) {
        EasyLog.print("=== PopupDataAdapter.convert() 开始 ===");
        EasyLog.print("输入 groups size: " + (groups != null ? groups.size() : "null"));

        if (groups == null || groups.isEmpty()) {
            EasyLog.print("❌ groups为null或空，返回空列表");
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }

        List<GroupData> groupDataList = new ArrayList<>();
        List<List<ItemData>> itemDataList = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            GroupEntity groupEntity = groups.get(groupIndex);
            EasyLog.print("--- 转换 Group[" + groupIndex + "] ---");
            EasyLog.print("header: " + groupEntity.getHeader());

            // 创建GroupData
            GroupData groupData = new GroupData();
            groupData.setTitle(groupEntity.getHeader());
            groupData.setExpanded(true); // 弹窗默认展开
            groupDataList.add(groupData);

            // 转换Children
            ArrayList<ChildEntity> children = groupEntity.getChildren();
            List<ItemData> items = new ArrayList<>();

            if (children != null && !children.isEmpty()) {
                EasyLog.print("children size: " + children.size());

                for (int childIndex = 0; childIndex < children.size(); childIndex++) {
                    ChildEntity childEntity = children.get(childIndex);

                    // 创建ItemData
                    ItemData itemData = new ItemData();
                    
                    // 转换文本数据
                    SpannableStringBuilder text = childEntity.getAttributed_child_section_text();
                    if (text != null && text.length() > 0) {
                        itemData.setAttributedText(text);
                    }
                    
                    // 转换注释
                    SpannableStringBuilder note = childEntity.getAttributed_child_section_note();
                    if (note != null && note.length() > 0) {
                        itemData.setAttributedNote(note);
                    }
                    
                    // 转换视频
                    SpannableStringBuilder video = childEntity.getAttributed_child_section_video();
                    if (video != null && video.length() > 0) {
                        itemData.setAttributedVideo(video);
                    }
                    
                    // 转换图片
                    String image = childEntity.getChild_section_image();
                    if (image != null && !image.isEmpty()) {
                        itemData.setImageUrl(image);
                    }
                    
                    // 设置组位置
                    itemData.setGroupPosition(groupIndex);

                    items.add(itemData);

                    if (childIndex < 3) { // 只打印前3个
                        EasyLog.print("  child[" + childIndex + "]: " + 
                                (text != null ? text.toString().substring(0, Math.min(50, text.length())) : "null"));
                    }
                }
            } else {
                EasyLog.print("⚠️ children为null或空");
            }

            itemDataList.add(items);
            EasyLog.print("转换完成，items size: " + items.size());
        }

        EasyLog.print("=== PopupDataAdapter.convert() 完成 ===");
        EasyLog.print("输出 groupDataList size: " + groupDataList.size());
        EasyLog.print("输出 itemDataList size: " + itemDataList.size());

        return new Pair<>(groupDataList, itemDataList);
    }
}
