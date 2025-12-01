package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;

public class ChatHistoryAdapter extends BaseAdapter {
    private Context context;
    private List<ChatHistoryItem> chatHistoryItems;

    public ChatHistoryAdapter(Context context) {
        this.context = context;
        this.chatHistoryItems = new ArrayList<>();
    }

    public void setData(List<ChatHistoryItem> data) {
        this.chatHistoryItems = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chatHistoryItems.size();
    }

    @Override
    public ChatHistoryItem getItem(int position) {
        return chatHistoryItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.chat_history_item, parent, false);
            holder = new ViewHolder();
            holder.tvChatTitle = convertView.findViewById(R.id.tv_chat_title);
            holder.tvChatPreview = convertView.findViewById(R.id.tv_chat_preview);
            holder.tvChatTime = convertView.findViewById(R.id.tv_chat_time);
            holder.tvMessageCount = convertView.findViewById(R.id.tv_message_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ChatHistoryItem item = getItem(position);
        holder.tvChatTitle.setText(item.getTitle());
        holder.tvChatPreview.setText(item.getPreview());
        holder.tvChatTime.setText(item.getTime());
        holder.tvMessageCount.setText(item.getMessageCount());
        
        // 设置斑马纹背景
        if (position % 2 == 0) {
            convertView.setBackgroundResource(R.drawable.chat_history_item_bg_even);
        } else {
            convertView.setBackgroundResource(R.drawable.chat_history_item_bg_odd);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvChatTitle;
        TextView tvChatPreview;
        TextView tvChatTime;
        TextView tvMessageCount;
    }

    public static class ChatHistoryItem {
        private String title;
        private String preview;
        private String time;
        private String messageCount;

        public ChatHistoryItem(String title, String preview, String time, String messageCount) {
            this.title = title;
            this.preview = preview;
            this.time = time;
            this.messageCount = messageCount;
        }

        public String getTitle() {
            return title;
        }

        public String getPreview() {
            return preview;
        }

        public String getTime() {
            return time;
        }

        public String getMessageCount() {
            return messageCount;
        }
    }
}