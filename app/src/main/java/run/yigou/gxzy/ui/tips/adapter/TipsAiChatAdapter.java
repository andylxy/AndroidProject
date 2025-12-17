package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import timber.log.Timber;

public final class TipsAiChatAdapter extends AppAdapter<ChatMessageBean> {


    // 定义布局资源ID常量
    private static final int LAYOUT_RECEIVED = R.layout.tips_ai_msg_chat_receive;
    private static final int LAYOUT_SEND = R.layout.tips_ai_msg_chat_send;
    private static final int LAYOUT_SYSTEM = R.layout.tips_ai_msg_chat_system;
    private static final int LAYOUT_THINKING = R.layout.tips_ai_msg_chat_thinking;
    private static final int LAYOUT_DEFAULT = R.layout.tips_ai_msg_chat_system; // 默认布局资源ID

    // 初始化 Markwon
    private Markwon markwon;

    public TipsAiChatAdapter(Context context) {
        super(context);
        // 初始化 Markwon - 使用更完整的配置
        initMarkwon(context);
        Timber.tag("TipsAiChatAdapter").d("Adapter created");
    }

    private void initMarkwon(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(CorePlugin.create()) // 核心插件
                .usePlugin(HtmlPlugin.create()) // HTML 支持
                .usePlugin(LinkifyPlugin.create()) // 链接识别
                .usePlugin(StrikethroughPlugin.create()) // 删除线支持
                .usePlugin(TablePlugin.create(context)) // 表格支持
                .usePlugin(TaskListPlugin.create(context)) // 任务列表支持
                .usePlugin(ImagesPlugin.create()) // 图片支持
                .build();
    }

    @Override
    public int getItemViewType(int position) {
        int type = getItem(position).getType();
        Timber.tag("TipsAiChatAdapter").d("getItemViewType: position=%d, type=%d", position, type);
        return type;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public void updateData() {
        if (getData() != null) {
            notifyItemChanged(getData().size() - 1);
        }
    }
    private int viewTypeLayout;

    @NonNull
    @Override
    public TipsAiChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Timber.tag("TipsAiChatAdapter").d("onCreateViewHolder: viewType=%d", viewType);

        switch (viewType) {
            case ChatMessageBean.TYPE_RECEIVED:
                viewTypeLayout = LAYOUT_RECEIVED;
                break;
            case ChatMessageBean.TYPE_SEND:
                viewTypeLayout = LAYOUT_SEND;
                break;
            case ChatMessageBean.TYPE_SYSTEM:
                viewTypeLayout = LAYOUT_SYSTEM;
                break;
            case ChatMessageBean.TYPE_THINKING:
                viewTypeLayout = LAYOUT_THINKING;
                break;
            default:
                // 记录日志并使用默认布局
                Timber.tag("TipsAiChatAdapter").w("Unknown view type: %s", viewType);
                viewTypeLayout = LAYOUT_DEFAULT;
                break;
        }

        Timber.tag("TipsAiChatAdapter").d("onCreateViewHolder: viewTypeLayout=%d", viewTypeLayout);
        return new TipsAiChatAdapter.ViewHolder(viewTypeLayout);
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private ViewHolder(int viewlayout) {
            super(viewlayout);
            Timber.tag("TipsAiChatAdapter").d("ViewHolder created with layout: %d", viewlayout);
        }

        @Override
        public void onBindView(int position) {
            ChatMessageBean bean = getItem(position);
            Timber.tag("TipsAiChatAdapter").d("onBindView: position=%d, type=%d", position, bean.getType());
            
            switch (bean.getType()) {
                case ChatMessageBean.TYPE_RECEIVED:
                    TextView tv_receive_content = findViewById(R.id.tv_receive_content);
                    ImageView iv_receive_picture = findViewById(R.id.iv_receive_picture);
                    TextView tv_receive_nick = findViewById(R.id.tv_receive_nick);
                    //tv_receive_content.setText(bean.getContent());
                    // 设置 Markdown 文本
                    if (markwon != null && tv_receive_content != null && bean.getContent() != null) {
                        markwon.setMarkdown(tv_receive_content, bean.getContent());
                    }
                    if (tv_receive_nick != null) {
                        tv_receive_nick.setText(bean.getNick());
                    }
                    // 不再强制设置图片资源，使用布局文件中定义的背景
                    break;

                case ChatMessageBean.TYPE_SEND:
                    TextView tv_send_content = findViewById(R.id.tv_send_content);
                    ImageView iv_send_picture = findViewById(R.id.iv_send_picture);
                    if (tv_send_content != null) {
                        tv_send_content.setText(bean.getContent());
                    }
                    if (iv_send_picture != null) {
                        // 检查是否有自定义头像URL，如果没有则使用默认头像
                        if (bean.getPic_url() != null && !bean.getPic_url().isEmpty()) {
                            // 这里应该加载网络图片，但由于缺少图片加载库，暂时使用默认图片
                            // 在实际项目中，你可以使用 Glide、Picasso 等库来加载网络图片
                            iv_send_picture.setImageResource(R.drawable.chat_user_default);
                        } else {
                            iv_send_picture.setImageResource(R.drawable.chat_user_default);
                        }
                    }
                    break;
                case ChatMessageBean.TYPE_SYSTEM:
                    TextView tv_system_content = findViewById(R.id.tv_system_content);
                    if (tv_system_content != null) {
                        tv_system_content.setText(bean.getContent());
                    }
                    break;
                    
                case ChatMessageBean.TYPE_THINKING:
                    TextView tv_thinking_content = findViewById(R.id.tv_thinking_content);
                    android.view.View ll_thinking_header = findViewById(R.id.ll_thinking_header);
                    TextView tv_thinking_arrow = findViewById(R.id.tv_thinking_arrow);
                    // 使用自定义的 InterceptableScrollView
                    run.yigou.gxzy.widget.InterceptableScrollView sv_thinking_content = findViewById(R.id.sv_thinking_content);
                    android.view.View v_separator = findViewById(R.id.v_separator);
                    
                    if (markwon != null && tv_thinking_content != null && bean.getContent() != null) {
                        markwon.setMarkdown(tv_thinking_content, bean.getContent());
                    }
                    
                    // 设置折叠状态
                    if (sv_thinking_content != null && tv_thinking_arrow != null) {
                        if (bean.isThinkingCollapsed()) {
                            sv_thinking_content.setVisibility(android.view.View.GONE);
                            if (v_separator != null) v_separator.setVisibility(android.view.View.GONE);
                            tv_thinking_arrow.setRotation(-90); // 旋转箭头表示折叠
                        } else {
                            sv_thinking_content.setVisibility(android.view.View.VISIBLE);
                            if (v_separator != null) v_separator.setVisibility(android.view.View.VISIBLE);
                            tv_thinking_arrow.setRotation(0); // 默认箭头向下

                            // 只在最后一个位置（正在生成时）自动滚动到底部
                            if (position == getItemCount() - 1 || position == getItemCount() - 2) {
                                sv_thinking_content.post(() -> {
                                    sv_thinking_content.fullScroll(android.view.View.FOCUS_DOWN);
                                });
                            }
                        }
                    }
                    
                    // 点击切换折叠状态
                    if (ll_thinking_header != null) {
                        ll_thinking_header.setOnClickListener(v -> {
                            bean.setThinkingCollapsed(!bean.isThinkingCollapsed());
                            notifyItemChanged(position); // 局部刷新
                        });
                    }
                    break;
            }
        }
    }
}