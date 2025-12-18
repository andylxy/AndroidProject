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
import com.hjq.base.BaseAdapter;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import timber.log.Timber;

public final class TipsAiChatAdapter extends AppAdapter<ChatMessageBean> {
    
    public static final String PAYLOAD_UPDATE_CONTENT = "PAYLOAD_UPDATE_CONTENT";


    // 定义布局资源ID常量
    private static final int LAYOUT_RECEIVED = R.layout.tips_ai_msg_chat_receive;
    private static final int LAYOUT_SEND = R.layout.tips_ai_msg_chat_send;
    private static final int LAYOUT_SYSTEM = R.layout.tips_ai_msg_chat_system;
    private static final int LAYOUT_THINKING = R.layout.tips_ai_msg_chat_thinking;
    private static final int LAYOUT_DEFAULT = R.layout.tips_ai_msg_chat_system; // 默认布局资源ID

    // 初始化 Markwon
    private Markwon markwon;
    private final Map<Long, TypewriterHelper> typewriterHelpers = new HashMap<>();

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

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter<?>.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            boolean processed = false;
            if (holder instanceof TipsAiChatAdapter.ViewHolder) {
                 TipsAiChatAdapter.ViewHolder myHolder = (TipsAiChatAdapter.ViewHolder) holder;
                 for (Object payload : payloads) {
                    if (PAYLOAD_UPDATE_CONTENT.equals(payload)) {
                        myHolder.onBindPayload(position);
                        processed = true;
                    }
                }
            }

            if (!processed) {
                super.onBindViewHolder(holder, position, payloads);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull BaseAdapter<?>.ViewHolder holder) {
        super.onViewRecycled(holder);
        // 清理打字机 helper 防止内存泄漏
        if (holder instanceof TipsAiChatAdapter.ViewHolder) {
            int pos = holder.getAdapterPosition();
            if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION && pos < getCount()) {
                ChatMessageBean bean = getItem(pos);
                if (bean != null && typewriterHelpers.containsKey(bean.getId())) {
                     TypewriterHelper helper = typewriterHelpers.get(bean.getId());
                     if(helper != null) helper.stop();
                     typewriterHelpers.remove(bean.getId());
                }
            }
        }
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private ViewHolder(int viewlayout) {
            super(viewlayout);
            Timber.tag("TipsAiChatAdapter").d("ViewHolder created with layout: %d", viewlayout);
        }

        public void onBindPayload(int position) {
            ChatMessageBean bean = getItem(position);
            // 仅对流式消息进行增量更新
            if (bean.getType() == ChatMessageBean.TYPE_RECEIVED && bean.isStreaming()) {
                 TypewriterHelper helper = typewriterHelpers.get(bean.getId());
                 if (helper != null) {
                     helper.setContent(bean.getContent(), true);
                 } else {
                     onBindView(position);
                 }
            }
        }

        @Override
        public void onBindView(int position) {
            ChatMessageBean bean = getItem(position);
            
            switch (bean.getType()) {
                case ChatMessageBean.TYPE_RECEIVED:
                    TextView tv_receive_content = findViewById(R.id.tv_receive_content);
                    ImageView iv_receive_picture = findViewById(R.id.iv_receive_picture);
                    TextView tv_receive_nick = findViewById(R.id.tv_receive_nick);
                    
                    if (tv_receive_nick != null) {
                        tv_receive_nick.setText(bean.getNick());
                    }

                    if (tv_receive_content != null) {
                        if (bean.isStreaming()) {
                            TypewriterHelper helper = typewriterHelpers.get(bean.getId());
                            if (helper == null) {
                                helper = new TypewriterHelper(tv_receive_content, markwon);
                                typewriterHelpers.put(bean.getId(), helper);
                            }
                            helper.setContent(bean.getContent(), true);
                        } else {
                            // 流式传输结束
                             TypewriterHelper helper = typewriterHelpers.remove(bean.getId());
                             if (helper != null) helper.stop();
                             
                             if (markwon != null && bean.getContent() != null) {
                                 markwon.setMarkdown(tv_receive_content, bean.getContent());
                             }
                        }
                    }
                    break;

                case ChatMessageBean.TYPE_SEND:
                    TextView tv_send_content = findViewById(R.id.tv_send_content);
                    ImageView iv_send_picture = findViewById(R.id.iv_send_picture);
                    if (tv_send_content != null) {
                        tv_send_content.setText(bean.getContent());
                    }
                    if (iv_send_picture != null) {
                        if (bean.getPic_url() != null && !bean.getPic_url().isEmpty()) {
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


    private static class TypewriterHelper {
        private final TextView textView;
        private final Markwon markwon;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private String targetContent = "";
        private int displayedLength = 0;
        private boolean isTyping = false;
        private boolean showCursor = true;
        // 光标字符：使用 ▋ 或者 solid block
        private static final String CURSOR_SYMBOL = " ▋";
        
        private final Runnable cursorBlinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTyping) {
                    showCursor = !showCursor;
                    render();
                    handler.postDelayed(this, 500);
                }
            }
        };

        private final Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (displayedLength < targetContent.length()) {
                    // 动态调整步长
                    int remaining = targetContent.length() - displayedLength;
                    // 如果剩余很多，步长加大，确保不会落后太多
                    int increment = Math.max(1, remaining / 20); 
                    // 也不要太快
                    increment = Math.min(increment, 5);
                    
                    displayedLength = Math.min(displayedLength + increment, targetContent.length());
                    render();
                    
                    // 降低渲染频率：80ms 一次，减少 Markdown 渲染次数
                    handler.postDelayed(this, 80);
                } else {
                    // 内容追平了，但仍处于流式状态，保持光标闪烁
                    render();
                    // 这里不自动 stop，因为SSE可能还在传输，只是暂时没有新内容
                    // 但 typingLoop 可以暂停，等待新内容唤醒
                }
            }
        };

        public TypewriterHelper(TextView textView, Markwon markwon) {
            this.textView = textView;
            this.markwon = markwon;
        }

        public void setContent(String content, boolean animate) {
            this.targetContent = content;
            if (animate) {
                if (!isTyping) {
                    isTyping = true;
                    showCursor = true;
                    handler.post(typingRunnable);
                    handler.post(cursorBlinkRunnable);
                } else {
                    // 如果已经在 typing，只需更新 targetContent，typingRunnable 会自动发现并追赶
                    // 确保 Runnable 正在运行 (防止之前追平停止了)
                    handler.removeCallbacks(typingRunnable);
                    handler.post(typingRunnable);
                }
            } else {
                stop();
                displayedLength = content.length();
                isTyping = false;
                showCursor = false;
                render();
            }
        }
        
        private void render() {
            if (textView == null) return;
            
            String textToShow = targetContent.substring(0, displayedLength);
            if (isTyping && showCursor) {
                textToShow += CURSOR_SYMBOL;
            }
            
            // 使用 Markwon 渲染以保持格式
            // 节流策略改进：通过减少渲染频率（50ms）而不是跳过 Markwon 调用
            if (markwon != null) {
                markwon.setMarkdown(textView, textToShow);
            } else {
                textView.setText(textToShow);
            }
        }

        public void stop() {
            isTyping = false;
            handler.removeCallbacks(typingRunnable);
            handler.removeCallbacks(cursorBlinkRunnable);
            // 最后渲染一次无光标状态
             if (markwon != null) {
                markwon.setMarkdown(textView, targetContent);
            } else {
                textView.setText(targetContent);
            }
        }
    }
}