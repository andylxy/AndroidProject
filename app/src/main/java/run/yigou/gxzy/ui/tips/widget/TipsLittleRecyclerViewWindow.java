package run.yigou.gxzy.ui.tips.widget;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.http.EasyLog;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.adapter.RefactoredPopupAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;

/**
 * RecyclerView弹窗中间基类
 * 为带RecyclerView的弹窗提供通用功能（新架构）
 */
public abstract class TipsLittleRecyclerViewWindow extends TipsLittleWindow {

    protected RecyclerView recyclerView;
    protected RefactoredPopupAdapter adapter;
    protected List<GroupData> groupDataList;
    protected List<List<ItemData>> itemDataList;
    
    // 新架构数据缓存
    private Context cachedContext;
    private List<GroupData> cachedGroupDataList;
    private List<List<ItemData>> cachedItemDataList;

    @Override
    protected View createContentView(LayoutInflater inflater, ViewGroup container) {
        // 查找RecyclerView
        recyclerView = this.view.findViewById(R.id.include_tips_windows_sticky_list)
                .findViewById(R.id.sticky_rv_list);

        // 设置LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return recyclerView;
    }

    @Override
    protected void bindData() {
        EasyLog.print("=== TipsLittleRecyclerViewWindow.bindData() ===");

        // 检查是否有缓存的数据
        if (cachedGroupDataList == null || cachedItemDataList == null) {
            EasyLog.print("❌ 数据为null，setData未调用");
            return;
        }

        this.groupDataList = cachedGroupDataList;
        this.itemDataList = cachedItemDataList;

        EasyLog.print("数据准备就绪:");
        EasyLog.print("  groupDataList size: " + groupDataList.size());
        EasyLog.print("  itemDataList size: " + itemDataList.size());

        // 创建adapter（此时recyclerView已经初始化）
        if (adapter == null) {
            EasyLog.print("创建RefactoredPopupAdapter");
            adapter = new RefactoredPopupAdapter(cachedContext, groupDataList, itemDataList);
            EasyLog.print("RefactoredPopupAdapter创建完成, itemCount: " + adapter.getItemCount());
        } else {
            EasyLog.print("adapter已存在, 更新数据");
            adapter.updateData(groupDataList, itemDataList);
        }

        EasyLog.print("✅ adapter准备就绪, itemCount: " + adapter.getItemCount());
        EasyLog.print("✅ recyclerView状态: " + (recyclerView != null ? "已初始化" : "null"));
        
        recyclerView.setAdapter(adapter);
        EasyLog.print("RecyclerView.setAdapter() 完成");
    }

    /**
     * 设置数据源（新架构专用）
     * 由于此方法在onCreateView()之前调用，recyclerView还未初始化
     * 因此只缓存数据，实际的adapter创建延迟到bindData()中进行
     *
     * @param context 上下文
     * @param data Pair<GroupData列表, ItemData二维列表>
     */
    public void setData(Context context, Pair<List<GroupData>, List<List<ItemData>>> data) {
        EasyLog.print("=== TipsLittleRecyclerViewWindow.setData() ===");
        EasyLog.print("Context: " + context);
        EasyLog.print("GroupData size: " + (data.first != null ? data.first.size() : "null"));
        EasyLog.print("ItemData size: " + (data.second != null ? data.second.size() : "null"));

        if (data.first == null || data.second == null) {
            EasyLog.print("❌ 数据为null");
            return;
        }

        // 缓存数据，等待bindData()时处理
        this.cachedContext = context;
        this.cachedGroupDataList = data.first;
        this.cachedItemDataList = data.second;
        
        EasyLog.print("✅ 数据已缓存，等待bindData()时创建adapter");
        EasyLog.print("setData() 完成");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        adapter = null;
        groupDataList = null;
        itemDataList = null;
        cachedContext = null;
        cachedGroupDataList = null;
        cachedItemDataList = null;
    }
}
