package run.yigou.gxzy.ui.feature.reader.ai.helper;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import run.yigou.gxzy.R;

/**
 * 输入助手：管理输入框、键盘和发送按钮
 */
public class ChatInputHelper {
    private final Activity activity;
    private final View rootView;
    private final OnInputActionListener actionListener;

    private EditText chatContent;
    private ImageView clearButton;
    private Button sendButton;

    public interface OnInputActionListener {
        void onSendMessage(String message);
    }

    public ChatInputHelper(Activity activity, View rootView, OnInputActionListener listener) {
        this.activity = activity;
        this.rootView = rootView;
        this.actionListener = listener;
        initView();
    }

    private void initView() {
        chatContent = rootView.findViewById(R.id.chat_content);
        clearButton = rootView.findViewById(R.id.iv_clear);
        sendButton = rootView.findViewById(R.id.chat_send);

        // 设置清除按钮的点击事件
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                if (chatContent != null) chatContent.setText("");
            });
        }

        if (chatContent != null) {
            // 合并所有的文本监听功能到一个监听器中
            chatContent.addTextChangedListener(new TextWatcher() {
                int lines = 1;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearButton != null) {
                        clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    int currentLines = chatContent.getLineCount();
                    if (currentLines > lines) {
                        chatContent.scrollTo(0, 0);
                    }
                    lines = currentLines;
                }
            });

            chatContent.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // chatContent.setSelection(0, 0); // 取消此行，否则每次获取焦点光标都在开头，体验不好
                    if (clearButton != null) {
                        clearButton.setVisibility(chatContent.getText().length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }
            });

            // 设置键盘回车/发送键监听
            chatContent.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    performSend();
                    return true;
                }
                return false;
            });
        }

        // 发送按钮逻辑
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> performSend());
        }
    }

    private void performSend() {
        if (chatContent == null) return;
        
        String msg = chatContent.getText().toString().trim(); // 使用 trim 避免发送纯空格
        if (!msg.isEmpty()) {
            hideKeyboard();
            chatContent.setText("");
            if (actionListener != null) {
                actionListener.onSendMessage(msg);
            }
        }
    }

    public void hideKeyboard() {
        if (activity == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && activity.getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    
    public void clearInput() {
        if (chatContent != null) {
            chatContent.setText("");
        }
    }
}
