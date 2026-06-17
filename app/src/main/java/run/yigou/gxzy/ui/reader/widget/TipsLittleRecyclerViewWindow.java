package run.yigou.gxzy.ui.reader.widget;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.tips.widget.TipsLittleWindow;
import run.yigou.gxzy.ui.reader.adapter.RefactoredPopupAdapter;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;

/**
 * RecyclerView弹窗中间基类
 * 为带RecyclerView的弹窗提供通用功能（新架构）
 */
public abstract class TipsLittleRecyclerViewWindow extends TipsLittleWindow {

    protected RecyclerView recyclerView;
    protected RefactoredPopupAdapter adapter;

    // 数据缓存（setData 在 onCreateView 之前调用，需延迟到 bindData 时使用）
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
        // 检查是否有缓存的数据
        if (cachedGroupDataList == null || cachedItemDataList == null) {
            return;
        }

        // 创建adapter（此时recyclerView已经初始化）
        if (adapter == null) {
            adapter = new RefactoredPopupAdapter(cachedContext, cachedGroupDataList, cachedItemDataList);
        } else {
            adapter.updateData(cachedGroupDataList, cachedItemDataList);
        }

        recyclerView.setAdapter(adapter);
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
        if (data.first == null || data.second == null) {
            return;
        }

        // 缓存数据，等待bindData()时处理
        this.cachedContext = context;
        this.cachedGroupDataList = data.first;
        this.cachedItemDataList = data.second;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        adapter = null;
        cachedContext = null;
        cachedGroupDataList = null;
        cachedItemDataList = null;
    }
}
