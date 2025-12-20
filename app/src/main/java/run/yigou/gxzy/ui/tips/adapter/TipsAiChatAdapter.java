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
       // Timber.tag("TipsAiChatAdapter").d("getItemViewType: position=%d, type=%d", position, type);
        return type;
    }
    
    @Override
    public long getItemId(int position) {
        ChatMessageBean bean = getItem(position);
        // 返回消息的数据库 ID，如果没有则返回 position
        // 这样 RecyclerView 可以正确追踪每条消息，避免重复 rebind
        if (bean != null && bean.getId() != null) {
            return bean.getId();
        }
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
       //Timber.tag("TipsAiChatAdapter").d("onCreateViewHolder: viewType=%d", viewType);

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
                //Timber.tag("TipsAiChatAdapter").w("Unknown view type: %s", viewType);
                viewTypeLayout = LAYOUT_DEFAULT;
                break;
        }

      //  Timber.tag("TipsAiChatAdapter").d("onCreateViewHolder: viewTypeLayout=%d", viewTypeLayout);
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
           // Timber.tag("TipsAiChatAdapter").d("ViewHolder created with layout: %d", viewlayout);
        }

        public void onBindPayload(int position) {
            ChatMessageBean bean = getItem(position);
            // 对 RECEIVED 类型消息进行更新
            if (bean.getType() == ChatMessageBean.TYPE_RECEIVED) {
                if (bean.isStreaming()) {
                    // 流式中：增量更新
                    TypewriterHelper helper = typewriterHelpers.get(bean.getId());
                    if (helper != null) {
                        helper.setContent(bean.getContent(), true);
                    } else {
                        onBindView(position);
                    }
                } else {
                    // 流式结束：完整绑定以触发 Markdown 渲染
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
                            // ===== SSE 流式期间 =====
                            // 1. 禁用文本选择，避免焦点导致滚动跳动
                            tv_receive_content.setTextIsSelectable(false);
                            // 2. 禁止获取焦点
                            tv_receive_content.setFocusable(false);
                            tv_receive_content.setFocusableInTouchMode(false);
                            // 3. 移除触摸监听器
                            tv_receive_content.setOnTouchListener(null);
                            
                            TypewriterHelper helper = typewriterHelpers.get(bean.getId());
                            if (helper == null) {
                                helper = new TypewriterHelper(tv_receive_content, markwon);
                                typewriterHelpers.put(bean.getId(), helper);
                            }
                            helper.setContent(bean.getContent(), true);
                        } else {
                            // ===== SSE 流式结束 或 从数据库加载的历史消息 =====
                            TypewriterHelper helper = typewriterHelpers.remove(bean.getId());
                            if (helper != null) helper.stop();
                             
                            if (markwon != null && bean.getContent() != null) {
                                markwon.setMarkdown(tv_receive_content, bean.getContent());
                            }
                            
                            // ⚠️ 彻底解决方案：不使用 setTextIsSelectable（它会导致焦点跳动）
                            // 改用长按弹出可选择文字的对话框
                            tv_receive_content.setTextIsSelectable(false);
                            tv_receive_content.setFocusable(false);
                            tv_receive_content.setFocusableInTouchMode(false);
                            tv_receive_content.setOnTouchListener(null);
                            
                            // 长按弹出可选择文字的对话框
                            final String contentToCopy = bean.getContent();
                            tv_receive_content.setOnLongClickListener(v -> {
                                showTextSelectionDialog(v.getContext(), contentToCopy);
                                return true;
                            });
                        }
                    }
                    break;

                case ChatMessageBean.TYPE_SEND:
                    TextView tv_send_content = findViewById(R.id.tv_send_content);
                    ImageView iv_send_picture = findViewById(R.id.iv_send_picture);
                    if (tv_send_content != null) {
                        tv_send_content.setText(bean.getContent());
                        // 发送的消息始终可选择复制
                        tv_send_content.setTextIsSelectable(true);
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


    /**
     * 显示可选择文字的对话框
     * 在对话框中可以自由选择和复制部分文字
     */
    private void showTextSelectionDialog(Context context, String content) {
        // 创建可选择文字的 TextView
        TextView textView = new TextView(context);
        textView.setText(content);
        textView.setTextIsSelectable(true);
        textView.setPadding(48, 32, 48, 32);
        textView.setTextSize(15);
        textView.setLineSpacing(0, 1.3f);
        
        // 使用 Markwon 渲染 Markdown（如果可用）
        if (markwon != null) {
            markwon.setMarkdown(textView, content);
        }
        
        // 包装在 ScrollView 中以支持长文本
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.addView(textView);
        
        // 设置最大高度为屏幕高度的 70%
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int maxHeight = (int) (displayMetrics.heightPixels * 0.7);
        scrollView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // 创建对话框
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("选择要复制的文字")
            .setView(scrollView)
            .setPositiveButton("复制全部", (d, which) -> {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("AI回复", content);
                clipboard.setPrimaryClip(clip);
                android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("关闭", null)
            .create();
        
        dialog.show();
        
        // 限制对话框最大高度
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(maxHeight, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    // 滚动回调接口
    public interface OnTypewriterRenderCallback {
        void onRender();
    }
    
    private static OnTypewriterRenderCallback scrollCallback;
    
    public static void setScrollCallback(OnTypewriterRenderCallback callback) {
        scrollCallback = callback;
    }

    private static class TypewriterHelper {
        private final TextView textView;
        private final Markwon markwon;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private String targetContent = "";
        private int displayedLength = 0;
        private boolean isTyping = false;
        private String lastRenderedText = "";

        private final Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (displayedLength < targetContent.length()) {
                    int remaining = targetContent.length() - displayedLength;
                    
                    // 丝滑打字效果：每次只增加 1-2 个字符
                    int increment;
                    if (remaining > 200) {
                        increment = 4; // 落后太多时快速追赶
                    } else if (remaining > 50) {
                        increment = 2; // 中等步长
                    } else {
                        increment = 1; // 丝滑单字符
                    }
                    
                    displayedLength = Math.min(displayedLength + increment, targetContent.length());
                    renderRawText();
                    
                    // 短延迟实现丝滑效果
                    long delay = 30; // 约 33fps
                    handler.postDelayed(this, delay);
                }
            }
        };

        public TypewriterHelper(TextView textView, Markwon markwon) {
            this.textView = textView;
            this.markwon = markwon;
        }

        public void setContent(String content, boolean animate) {
            if (content == null) content = "";
            this.targetContent = content;
            if (animate) {
                if (!isTyping) {
                    isTyping = true;
                    handler.post(typingRunnable);
                } else {
                    handler.removeCallbacks(typingRunnable);
                    handler.post(typingRunnable);
                }
            } else {
                stop();
            }
        }
        
        // 流式期间只显示原始文本，不渲染 Markdown（避免闪烁）
        private void renderRawText() {
            if (textView == null) return;
            
            String textToShow = targetContent.substring(0, displayedLength);
            
            if (!textToShow.equals(lastRenderedText)) {
                lastRenderedText = textToShow;
                textView.setText(textToShow); // 只用 setText，不用 Markwon
                
                // 触发滚动回调
                if (scrollCallback != null) {
                    scrollCallback.onRender();
                }
            }
        }

        // 停止流式时，用 Markwon 一次性渲染 Markdown
        public void stop() {
            isTyping = false;
            handler.removeCallbacks(typingRunnable);
            displayedLength = targetContent.length();
            lastRenderedText = "";
            
            if (textView != null) {
                // SSE 完成后，一次性渲染 Markdown
                if (markwon != null) {
                    markwon.setMarkdown(textView, targetContent);
                } else {
                    textView.setText(targetContent);
                }
            }
        }
    }
}
