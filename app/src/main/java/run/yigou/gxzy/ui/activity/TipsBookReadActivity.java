/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadActivity.java
 * 包名: run.yigou.gxzy.ui.activity.TipsBookReadActivity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月08日 10:50:43
 * 上次修改时间: 2024年09月08日 10:50:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.Helper;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonData;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupModel;


public class TipsBookReadActivity extends AppActivity {
    private RecyclerView rvList;
    private ClearEditText clearEditText;
    private ExpandableAdapter adapter;
    private TextView numTips;
    private int totalNum;
    private String searchText = null;
    private Button tipsBtnSearch;

    /**
     * @return
     */
    @Override
    protected int getLayoutId() {
        return R.layout.tips_book_read_activity_group_list;
    }

    /**
     *
     */
    @Override
    protected void initView() {
        rvList = findViewById(R.id.tips_book_read_activity_group_list);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        clearEditText = findViewById(R.id.searchEditText);
        tipsBtnSearch = findViewById(R.id.tips_btn_search);
        tipsBtnSearch.setOnClickListener(this);
        numTips = findViewById(R.id.numTips);
        clearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d("clearEditText", "onTextChanged: " + s);
                if (charSequenceIsEmpty(s)) {
                    reListAdapter(true, false);
                    numTips.setText("");
                    Log.d("clearEditText", "onTextChanged: 1");

                } else {
                    setSearchText(s.toString());
                    Log.d("clearEditText", "onTextChanged: 2");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        adapter = new ExpandableAdapter(this);
        // adapter.setmGroups(GroupModel.getExpandableGroups(SingletonData.getInstance().getContent(), false));
        reListAdapter(true, false);
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(ExpandableActivity.this, "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                ExpandableAdapter expandableAdapter = (ExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
            }
        });
        adapter.setOnChildClickListener(new GroupedRecyclerViewAdapter.OnChildClickListener() {
            @Override
            public void onChildClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                     int groupPosition, int childPosition) {
//                Toast.makeText(getContext(), "子项：groupPosition = " + groupPosition
//                                + ", childPosition = " + childPosition,
//                        Toast.LENGTH_LONG).show();
                //String text=  ((TextView) holder.get(R.id.tv_child2)).getText().toString();
                //Toast.makeText(getContext(), text,Toast.LENGTH_LONG).show();

            }
        });
        rvList.setAdapter(adapter);
    }

    /**
     *
     */
    @Override
    protected void initData() {

    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tips_btn_search) {

            if (this.searchText == null) {
                reListAdapter(true,false);
            } else setSearchText(this.searchText);
        }
    }

//    /**
//     * @param init false 初始化显示 ,true 搜索结果 显示
//     */
//    private void reListAdapter(boolean init) {
//        reListAdapter(init, true);
//        adapter.notifyDataChanged();
//    }

    /**
     * @param init    true  初始化显示 ,false 搜索结果 显示
     * @param isExpand false 表头不展开, true 展开
     */
    private void reListAdapter(boolean init, boolean isExpand) {
        if (init) {
            adapter.setmGroups(GroupModel.getExpandableGroups(SingletonData.getInstance().getContent(), isExpand,false));
        } else {
            if (SingletonData.getInstance().getSearchResList() != null)
                adapter.setmGroups(GroupModel.getExpandableGroups(SingletonData.getInstance().getSearchResList(), isExpand,true));
        }
        adapter.notifyDataChanged();
    }

    /**
     * 判断 CharSequence 是否为空。
     *
     * @param charSequence 需要判断的 CharSequence
     * @return 如果为 null 或长度为 0，则返回 true；否则返回 false
     */
    public boolean charSequenceIsEmpty(CharSequence charSequence) {
        boolean bl = false;
        if (charSequence == null || charSequence.length() == 0) {
            this.searchText = null;
            bl = true;
        }
        return bl;
    }

    @SuppressLint("DefaultLocale")
    public void setSearchText(String searchText) {
        this.searchText = searchText;

        // 检查搜索文本是否有效（不为 null、不为空且不是数字）
        if (searchText != null && !searchText.isEmpty()) {
            // 将搜索词拆分并过滤掉空白项
            String[] searchTerms = searchText.split(" ");
            List<String> validSearchTerms = new ArrayList<>();
            for (String term : searchTerms) {
                if (!term.isEmpty()) {
                    validSearchTerms.add(term);
                }
            }
            this.totalNum = 0; // 重置匹配结果数量
            ArrayList<HH2SectionData> filteredData = new ArrayList<>(); // 用于保存过滤后的结果

            // 从单例数据中获取必要的映射和列表
            Map<String, String> yaoAliasDict = SingletonData.getInstance().getYaoAliasDict();
            Map<String, String> fangAliasDict = SingletonData.getInstance().getFangAliasDict();
//            List<String> allYao = SingletonData.getInstance().getAllYao();
//            List<String> allFang = SingletonData.getInstance().getAllFang();

            // 遍历数据以进行过滤
            for (HH2SectionData sectionData : SingletonData.getInstance().getContent()) {
                List<DataItem> matchedItems = new ArrayList<>(); // 用于保存当前部分中的匹配项
                boolean sectionHasMatches = false;

                // 检查当前部分中的每一个数据项
                for (DataItem dataItem : sectionData.getData()) {
                    boolean itemMatched = false;

                    // 检查每个搜索词
                    for (String term : validSearchTerms) {
                        String sanitizedTerm = sanitizeTerm(term); // 清理搜索词
                        Pattern pattern;

                        // 从清理后的搜索词编译正则表达式
                        try {
                            pattern = Pattern.compile(sanitizedTerm);
                        } catch (Exception e) {
                            // 如果正则表达式失败，则回退到基本模式
                            pattern = Pattern.compile(".");
                        }

                        // 检查数据项是否符合搜索条件
                        if (matchDataItem(dataItem, pattern, sanitizedTerm, yaoAliasDict, fangAliasDict)) {
                            itemMatched = true;
                            // 突出显示数据项中的匹配文本
                            highlightMatchingText(dataItem, pattern);
                            break; // 一旦匹配，继续下一个数据项
                        }
                    }

                    // 如果有任何搜索词匹配，则加入匹配项
                    if (itemMatched) {
                        matchedItems.add(dataItem.getCopy());
                        sectionHasMatches = true;
                        this.totalNum++;
                    }
                }

                // 如果有匹配项，则将其添加到过滤后的结果中
                if (sectionHasMatches) {
                    filteredData.add(new HH2SectionData(matchedItems, sectionData.getSection(), sectionData.getHeader()));
                }
            }

            // 更新展示的数据和结果
            SingletonData.getInstance().setSearchResList(filteredData);
            if (this.numTips != null) {
                this.numTips.setText(String.format("%d个结果", this.totalNum));
            }
            if (this.adapter != null) {
                reListAdapter(false,true);
            }
        } else {
            if (this.adapter != null) {
                reListAdapter(true, false);
            }
        }

    }

    /**
     * 清理搜索词，去除不必要的字符。
     */
    private String sanitizeTerm(String term) {
        // 去除前后破折号，并替换特殊字符（如果需要）
        return term.replace("-", "").replace("#", ".");
    }

    /**
     * 根据指定的模式匹配数据项。
     */
    private boolean matchDataItem(DataItem dataItem, Pattern pattern, String term,
                                  Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 检查数据项的属性是否与搜索词和正则匹配
        String attributeText = dataItem.getAttributedText().toString();
        // 这里可以进一步处理别名

        // 检查主属性文本是否匹配
        return pattern.matcher(attributeText).find() || checkAliases(dataItem, term, pattern, yaoAliasDict, fangAliasDict);
    }

    /**
     * 在数据项中突出显示匹配文本。
     */
    private void highlightMatchingText(DataItem dataItem, Pattern pattern) {
        SpannableStringBuilder spannableText = new SpannableStringBuilder(dataItem.getAttributedText());
        Matcher matcher = pattern.matcher(spannableText);
        //todo 需要更换突出颜色变更位置点 new ForegroundColorSpan(0xFFFF0000)
        // 在 Spannable 文本中突出显示所有匹配项
        while (matcher.find()) {
            spannableText.setSpan(new ForegroundColorSpan(0xFFFF0000), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        dataItem.setAttributedText(spannableText);
    }

    /**
     * 检查别名以寻找额外的匹配项。
     */
    private boolean checkAliases(DataItem dataItem, String term, Pattern pattern,
                                 Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 检查 YaoList 和 FangList 是否与模式匹配
        for (String yao : dataItem.getYaoList()) {
            String alias = yaoAliasDict.get(yao);
            if (alias != null && pattern.matcher(alias).find()) {
                return true;
            }
        }

        for (String fang : dataItem.getFangList()) {
            String alias = fangAliasDict.get(fang);
            if (alias != null && pattern.matcher(alias).find()) {
                return true;
            }
        }

        return false;
    }


    //    @NonNull
//    @Override
//    protected ImmersionBar createStatusBarConfig() {
//        return super.createStatusBarConfig()
//                // 指定导航栏背景颜色
//                .navigationBarColor(R.color.white);
//    }
    public static void start(Context context) {
        Intent intent = new Intent(context, TipsBookReadActivity.class);
        // intent.putExtra(APPCONST.BOOK, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }


}
