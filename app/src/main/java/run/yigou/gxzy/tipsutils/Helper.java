package run.yigou.gxzy.tipsutils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.core.internal.view.SupportMenu;
import androidx.core.view.ViewCompat;

import com.lxj.xpopup.XPopup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.ui.tips.TipsWindowYao_BubbleAttachPopup;
import run.yigou.gxzy.ui.tips.TipsWindow_BubbleAttachPopup;

/* loaded from: classes.dex */
public class Helper {

    /* loaded from: classes.dex */
    public interface IBool<T> {
        boolean isOK(T t);
    }

    /* loaded from: classes.dex */
    public interface IFilter<T> {
        boolean filter(T t);
    }

    /* loaded from: classes.dex */
    public interface IFilterThenForEach<T> {
        boolean filter(T t);

        void forEachDo(T t);
    }

    /* loaded from: classes.dex */
    public interface IFilterThenMap<T, K> {
        boolean filter(T t);

        K map(T t);
    }

    /* loaded from: classes.dex */
    public interface IForEach<T> {
        void forEachDo(T t);
    }

    /* loaded from: classes.dex */
    public interface IForEachIdx<T> {
        void forEachDo(T t, int i);
    }

    /* loaded from: classes.dex */
    public interface IMap<T, K> {
        K map(T t);
    }

    /* loaded from: classes.dex */
    public interface IReduce<P, T> {
        P reduce(P p, T t);
    }

    /* loaded from: classes.dex */
    public interface IURLExist {
        void isExist(boolean z);
    }

    public static <T> T def(T t, T t2) {
        return t == null ? t2 : t;
    }

    public static ArrayList<Integer> getAllSubStringPos(String str, String str2) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        int length = str.length();
        int i = 0;
        while (i < length && str.substring(i).contains(str2)) {
            Integer valueOf = Integer.valueOf(i + str.substring(i).indexOf(str2));
            arrayList.add(valueOf);
            i = valueOf.intValue() + str2.length();
        }
        return arrayList;
    }

    public static boolean isNumeric(String str) {
        char charAt;
        if (str == null || str.length() == 0) {
            return false;
        }
        int length = str.length();
        do {
            length--;
            if (length < 0) {
                return true;
            }
            charAt = str.charAt(length);
            if (charAt < '0') {
                break;
            }
        } while (charAt <= '9');
        return false;
    }

    public static void renderItemNumber(SpannableStringBuilder spannableStringBuilder) {
        String spannableStringBuilder2 = spannableStringBuilder.toString();
        if (spannableStringBuilder2.contains("、") && isNumeric(spannableStringBuilder2.substring(0, spannableStringBuilder2.indexOf("、")))) {
            spannableStringBuilder.setSpan(new ForegroundColorSpan(-16776961), 0, spannableStringBuilder2.indexOf("、"), 33);
        }
    }
    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);

        while (true) {
            // 查找下一个"$"符号的位置
            int indexOf = str.indexOf("$");
            if (indexOf >= 0) {
                // 查找"{"符号的位置
                int indexOf2 = str.indexOf("}");
                // 计算"$"的数量以确定匹配的结束位置
                int size = getAllSubStringPos(str.substring(indexOf, indexOf2), "$").size();
                int i = indexOf2;
                int i2 = 1;

                // 根据找到的"$"数量来调整结束位置
                while (size > i2) {
                    for (int i3 = 0; i3 < size - i2; i3++) {
                        i += str.substring(i + 1).indexOf("}") + 1;
                    }
                    int i4 = size;
                    size = getAllSubStringPos(str.substring(indexOf, i), "$").size();
                    i2 = i4;
                }

                // 提取"$"后面的第一个字符作为标记
                String substring = str.substring(indexOf + 1, indexOf + 2);

                // 根据标记设置不同的样式
                if (substring.equals("a") || substring.equals("w")) {
                    spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (substring.equals("u")) {
                    spannableStringBuilder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            clickLink.clickYaoLink((TextView) view, this);
                        }
                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (substring.equals("f")) {
                    spannableStringBuilder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            clickLink.clickFangLink((TextView) view, this);
                        }
                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // 设置文本颜色
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getColoredTextByStrClass(substring));
                spannableStringBuilder.setSpan(foregroundColorSpan, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // 替换处理过的部分
                spannableStringBuilder.replace(i, i + 1, "");
                spannableStringBuilder.replace(indexOf, indexOf + 3, "");

                // 更新原始字符串为处理后的字符串
                str = spannableStringBuilder.toString();
            } else {
                // 处理完所有"$"后，进行额外的渲染
                renderItemNumber(spannableStringBuilder);
                return spannableStringBuilder;
            }
        }
    }


    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, new ClickLink() {
            @Override
            public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {
              //  Helper.closeKeyboard(SingletonData.getInstance().curActivity);
                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                System.out.println("tapped:" + charSequence);
                PointF pointF =(PointF) textView.getTag();
                new XPopup.Builder(textView.getContext())
                        .isTouchThrough(true)
                       // .popupPosition(PopupPosition.Right)
                        .positionByWindowCenter(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .isViewMode(true)
                        .atView(textView)
                        //.atPoint(pointF)
                        //.hasShadowBg(false) // 去掉半透明背景
                        .asCustom(new TipsWindowYao_BubbleAttachPopup(textView.getContext(),charSequence))
                        .show();
            }

            @Override
            public void clickFangLink(TextView textView, ClickableSpan clickableSpan) {
               // Helper.closeKeyboard(SingletonData.getInstance().curActivity);
                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                System.out.println("tapped:" + charSequence);
//                MotionEvent org_event =(MotionEvent) textView.getTag();
//                if (org_event!=null){
//                    Log.e("Helper-->--XPopup", "可点击的文本 (按下或抬起) mRawX : "+org_event.getRawX() +" mRawY: "+org_event.getRawY());
//                }
                new XPopup.Builder(textView.getContext())
                        .isTouchThrough(true)
                        .positionByWindowCenter(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .isViewMode(true)
                        .atView(textView)
                        //.offsetX((int) org_event.getRawX())
                       // .offsetY((int) org_event.getRawY())
                        // .atPoint(pointF)
                        //.hasShadowBg(false) // 去掉半透明背景
                        .asCustom(
                                new TipsWindow_BubbleAttachPopup(textView.getContext(),charSequence)
                                       // .setArrowHeight(XPopupUtils.dp2px(textView.getContext(), 6f) )
                        )
                        .show();
                }

        });
    }


    private static Rect getTextRect(ClickableSpan clickableSpan, TextView textView) {
        Rect rect = new Rect(); // 用于存储文本的矩形区域
        SpannableString spannableString = (SpannableString) textView.getText(); // 获取 TextView 的文本
        Layout layout = textView.getLayout(); // 获取文本布局
        int start = spannableString.getSpanStart(clickableSpan); // 获取可点击 span 的起始位置
        int end = spannableString.getSpanEnd(clickableSpan); // 获取可点击 span 的结束位置

        int startLine = layout.getLineForOffset(start); // 获取起始位置所在的行号
        int endLine = layout.getLineForOffset(end); // 获取结束位置所在的行号
        boolean isMultiLine = startLine != endLine; // 判断 span 是否跨越多行

        layout.getLineBounds(startLine, rect); // 获取起始行的边界
        int[] location = new int[2]; // 存储 TextView 在屏幕上的位置
        textView.getLocationOnScreen(location); // 获取 TextView 在屏幕上的位置
        int scrollY = textView.getScrollY(); // 获取 TextView 的垂直滚动量
        rect.offset(0, location[1] - scrollY + textView.getCompoundPaddingTop()); // 调整矩形区域的顶部位置

        if (isMultiLine) {
            // 处理 span 跨越多行的情况
            if (rect.top > AppApplication.getmContext().getResources().getDisplayMetrics().heightPixels - rect.bottom) {
                // 如果矩形区域的底部超出屏幕高度，使用起始行的右边界
                rect.right = (int) layout.getLineRight(startLine);
            } else {
                // 否则，处理结束行的边界
                layout.getLineBounds(endLine, rect);
                rect.offset(0, location[1] - scrollY + textView.getCompoundPaddingTop()); // 调整矩形区域的顶部位置
                rect.left = (int) layout.getLineLeft(endLine); // 设置矩形区域的左边界
            }
        } else {
            // 处理 span 不跨越多行的情况
            rect.right = (int) (layout.getPrimaryHorizontal(end) - layout.getPrimaryHorizontal(start)) + rect.left; // 计算右边界
        }

        rect.left += textView.getCompoundPaddingLeft() - textView.getScrollX(); // 调整矩形区域的左边界
        return rect; // 返回最终计算的矩形区域
    }

    @SuppressLint("RestrictedApi")
    public static int getColoredTextByStrClass(String str) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("r", Integer.valueOf(SupportMenu.CATEGORY_MASK));
        hashMap.put("n", Color.BLUE); // 蓝色
        hashMap.put("f", Color.BLUE); // 蓝色
        hashMap.put("a", Color.parseColor("#C0C0C0")); // 浅灰色
        hashMap.put("m", Color.rgb(255, 0, 0)); // 红色
        hashMap.put("s", Color.argb(128, 0, 0, 255)); // 半透明蓝色
        hashMap.put("u", Color.rgb(77, 0, 255)); // 紫色
        hashMap.put("v", Color.rgb(77, 0, 255)); // 紫色
        hashMap.put("w", Color.rgb(0, 128, 0)); // 绿色
        hashMap.put("q", Color.rgb(61, 200, 120)); // 浅绿色
        hashMap.put("h", Color.BLACK); // 默认颜色，黑色
        hashMap.put("x", Color.parseColor("#EA8E3B")); // 橙色
        hashMap.put("y", Color.parseColor("#9A764F")); // 棕色

        Integer num = hashMap.get(str);
        return num == null ? ViewCompat.MEASURED_STATE_MASK : num.intValue();
    }


    public static int strLengh(String str) {
        if (str == null) {
            return 0;
        }
        return str.length();
    }

    public static String getAliasString(Map<String, String> map, String str) {
        if (map == null || str == null) {
            return null;
        }
        String str2 = map.get(str);
        return str2 == null ? str : str2;
    }

    public static void putStringToClipboard(String str) {
        ((ClipboardManager) AppApplication.application.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("shangHanLun", str));
    }

    public static List<HH2SectionData> searchText(List<HH2SectionData> list, DataItemCompare dataItemCompare) {
        ArrayList arrayList = new ArrayList();
        for (HH2SectionData hH2SectionData : list) {
            ArrayList arrayList2 = null;
            for (DataItem dataItem : hH2SectionData.getData()) {
                if (dataItemCompare.useThisItem(dataItem)) {
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                    }
                    arrayList2.add(dataItem);
                }
            }
            if (arrayList2 != null) {
                arrayList.add(new HH2SectionData(arrayList2, hH2SectionData.getSection(), hH2SectionData.getHeader()));
            }
        }
        return arrayList;
    }

    public static <T> List<T> filter(List<T> list, IFilter<T> iFilter) {
        ArrayList arrayList = new ArrayList();
        for (T t : list) {
            if (iFilter.filter(t)) {
                arrayList.add(t);
            }
        }
        return arrayList;
    }

    public static <T, K> List<K> map(List<T> list, IMap<T, K> iMap) {
        ArrayList arrayList = new ArrayList();
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(iMap.map(it.next()));
        }
        return arrayList;
    }

    public static <T, K> List<K> filterThenMap(List<T> list, IFilterThenMap<T, K> iFilterThenMap) {
        ArrayList arrayList = new ArrayList();
        for (T t : list) {
            if (iFilterThenMap.filter(t)) {
                arrayList.add(iFilterThenMap.map(t));
            }
        }
        return arrayList;
    }

    public static <T> void filterThenForEachDo(List<T> list, IFilterThenForEach<T> iFilterThenForEach) {
        for (T t : list) {
            if (iFilterThenForEach.filter(t)) {
                iFilterThenForEach.forEachDo(t);
            }
        }
    }

    public static <P, T> P reduce(List<T> list, P p, IReduce<P, T> iReduce) {
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            p = iReduce.reduce(p, it.next());
        }
        return p;
    }

    public static <T> List<T> uniq(List<T> list) {
        ArrayList arrayList = new ArrayList();
        for (T t : list) {
            if (!arrayList.contains(t)) {
                arrayList.add(t);
            }
        }
        return arrayList;
    }

    public static <T> void forEachDo(List<T> list, IForEach<T> iForEach) {
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            iForEach.forEachDo(it.next());
        }
    }

    public static <T> void forEachDo(List<T> list, IForEachIdx<T> iForEachIdx) {
        Iterator<T> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            iForEachIdx.forEachDo(it.next(), i);
            i++;
        }
    }

    public static <T> boolean some(List<T> list, IBool<T> iBool) {
        if (list == null) {
            return false;
        }
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            if (iBool.isOK(it.next())) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean every(List<T> list, IBool<T> iBool) {
        if (list == null) {
            return false;
        }
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            if (!iBool.isOK(it.next())) {
                return false;
            }
        }
        return true;
    }

    public static <T> int index(List<T> list, IBool<T> iBool) {
        for (int i = 0; i < list.size(); i++) {
            if (iBool.isOK(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> split(String str, String str2) {
        return Arrays.asList(str.split(str2));
    }

    public static String join(String[] strArr, String str) {
        return join((List<String>) Arrays.asList(strArr), str);
    }

    public static String join(List<String> list, String str) {
        String str2 = "";
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            str2 = str2 + it.next() + str;
        }
        return str2.length() < str.length() ? "" : str2.substring(0, str2.length() - str.length());
    }

    public static int dip2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static int px2dip(Context context, float f) {
        return (int) ((f / context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static int getScreenWidth(Activity activity) {
        return activity.getWindow().getDecorView().getWidth();
    }

    public static int getScreenHeight(Activity activity) {
        return activity.getWindow().getDecorView().getHeight();
    }

    public static String getDateString(int i) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() - (((i * 24) * 3600) * 1000)));
    }







    public static <T extends Activity> ViewGroup getWindow(T t) {
        return (ViewGroup) t.getWindow().getDecorView();
    }



    public static String getPriceInputTitle(String str) {
        return str.replace(" ", "").replace("：", "");
    }

    public static <T extends Activity> void closeKeyboard(T t) {
//        InputMethodManager inputMethodManager = (InputMethodManager) t.getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (inputMethodManager != null) {
//            inputMethodManager.hideSoftInputFromWindow(t.getWindow().getDecorView().getWindowToken(), 0);
//        }
        InputMethodManager inputMethodManager = (InputMethodManager) t.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            View view = t.getWindow().getDecorView();
            if (view != null) {
                // 使用 post 方法确保视图已完全加载
                view.post(() -> inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0));
            }
        }
    }


}
