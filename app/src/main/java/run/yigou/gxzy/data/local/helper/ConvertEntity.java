package run.yigou.gxzy.data.local.helper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.crypto.SecurityUtils;
import run.yigou.gxzy.data.local.entity.BeiMingCi;
import run.yigou.gxzy.data.local.entity.BookChapter;
import run.yigou.gxzy.data.local.entity.BookChapterBody;
import run.yigou.gxzy.data.local.entity.YaoFang;
import run.yigou.gxzy.data.local.entity.YaoFangBody;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoUse;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.StringHelper;

/**
 * 数据实体转换与加解密工具类
 *
 * 专注于纯实体转换、加解密和序列化工具。
 * 数据库 CRUD 操作（含读取）已迁移至 {@link DataRepository}。
 *
 * 职责划分：
 * - ConvertEntity：实体⇔模型转换、RC4加解密、字符串序列化
 * - {@link DataRepository}：数据库读写操作编排 + 网络获取
 *
 * @author Android 开源项目
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public class ConvertEntity {

    /** 日志标签 */
    private static final String TAG = "ConvertEntity";

    /** 列表分隔符 */
    private static final String LIST_SEPARATOR = ",";

    /** 正则分隔符，用于中药别名等多分隔符场景 */
    private static final String REGEX_SEPARATOR = "[,\uff0c\u3001.;]";

    // ==================== 数据库操作工具（供 DataRepository 使用） ====================

    /**
     * 数据库操作函数式接口
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 执行数据库操作，统一异常捕获与日志记录
     *
     * @param operation     数据库操作
     * @param operationName 操作名称（用于日志）
     * @param <T>           返回类型
     * @return 操作结果，异常时返回 null
     */
    public static <T> T executeDatabaseOperation(DatabaseOperation<T> operation, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            EasyLog.print(TAG, "数据库操作失败 [" + operationName + "]: " + e.getMessage());
            return null;
        }
    }

    // ==================== 加解密工具 ====================

    /**
     * RC4 加密（非空时加密，空值返回空字符串）
     *
     * @param text 待加密文本
     * @return 加密后的文本，空值返回空字符串
     */
    public static String encryptIfNotEmpty(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Encrypt(text);
    }

    /**
     * RC4 解密（非空时解密，空值返回空字符串）
     *
     * @param encryptedText 待解密文本
     * @return 解密后的文本，空值返回空字符串
     */
    public static String decryptIfNotEmpty(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Decrypt(encryptedText);
    }

    // ==================== 字符串序列化工具 ====================

    /**
     * 字符串拆分为列表
     *
     * @param text     待拆分字符串
     * @param useRegex 是否使用正则分隔符（中药别名等场景）
     * @return 拆分后的列表，空值返回空列表
     */
    public static List<String> splitStringToList(String text, boolean useRegex) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String separator = useRegex ? REGEX_SEPARATOR : LIST_SEPARATOR;
        return Arrays.asList(text.split(separator));
    }

    /**
     * 列表合并为字符串
     *
     * @param list 字符串列表
     * @return 合并后的字符串，空值返回空字符串
     */
    public static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(LIST_SEPARATOR, list);
    }

    // ==================== 实体转换方法（供 DataRepository 调用） ====================

    /**
     * Fang 模型 → YaoFang 数据库实体
     */
    public static YaoFang convertFangToYaoFang(Fang fang, int bookId, String yaoFangId) {
        if (fang == null || yaoFangId == null) {
            return null;
        }

        try {
            YaoFang yaoFang = new YaoFang();
            yaoFang.setYaoCount(fang.getYaoCount());
            yaoFang.setName(fang.getName());
            yaoFang.setBookId(bookId);
            yaoFang.setID(fang.getID());
            yaoFang.setDrinkNum(fang.getDrinkNum());
            yaoFang.setText(encryptIfNotEmpty(fang.getText()));
            yaoFang.setFangList(listToString(fang.getFangList()));
            yaoFang.setYaoList(listToString(fang.getYaoList()));
            yaoFang.setYaoFangID(yaoFangId);
            yaoFang.setSignature(fang.getSignature());
            yaoFang.setSignatureId(fang.getSignatureId());
            return yaoFang;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换方剂失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * YaoUse 模型 → YaoFangBody 数据库实体
     */
    @NonNull
    public static YaoFangBody convertYaoUseToYaoFangBody(YaoUse content, String yaoFangId) {
        YaoFangBody yaoFangBody = new YaoFangBody();
        yaoFangBody.setYaoFangBodyId(StringHelper.getUuid());
        yaoFangBody.setYaoFangID(yaoFangId);
        yaoFangBody.setSuffix(content.getSuffix());
        yaoFangBody.setAmount(content.getAmount());
        yaoFangBody.setYaoID(content.getYaoID());
        yaoFangBody.setWeight(content.getWeight());
        yaoFangBody.setShowName(content.getShowName());
        yaoFangBody.setExtraProcess(content.getExtraProcess());
        yaoFangBody.setSignatureId(content.getSignatureId());
        yaoFangBody.setSignature(content.getSignature());
        return yaoFangBody;
    }

    /**
     * DataItem 模型 → BookChapterBody 数据库实体（加密存储）
     */
    public static BookChapterBody createBookChapterBody(String chapterId, DataItem content) {
        if (chapterId == null || content == null) {
            return null;
        }

        try {
            BookChapterBody bookChapterBody = new BookChapterBody();
            bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
            bookChapterBody.setBookChapterId(chapterId);
            bookChapterBody.setText(encryptIfNotEmpty(content.getText()));
            bookChapterBody.setNote(encryptIfNotEmpty(content.getNote()));
            bookChapterBody.setSectionvideo(encryptIfNotEmpty(content.getSectionvideo()));
            bookChapterBody.setID(content.getID());
            bookChapterBody.setSignature(content.getSignature());
            bookChapterBody.setSignatureId(content.getSignatureId());
            bookChapterBody.setFangList(listToString(content.getFangList()));
            return bookChapterBody;
        } catch (Exception e) {
            EasyLog.print(TAG, "创建章节内容实体失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * HH2SectionData + bookId → BookChapter 数据库实体
     */
    public static BookChapter createBookChapterByBookId(int bookId, HH2SectionData sectionData, String chapterId) {
        if (bookId <= 0 || sectionData == null || chapterId == null) {
            return null;
        }

        try {
            BookChapter bookChapter = new BookChapter();
            bookChapter.setSection(sectionData.getSection());
            bookChapter.setHeader(sectionData.getHeader());
            bookChapter.setBookId(bookId);
            bookChapter.setBookChapterId(chapterId);
            bookChapter.setSignature(sectionData.getSignature());
            bookChapter.setSignatureId(sectionData.getSignatureId());
            return bookChapter;
        } catch (Exception e) {
            EasyLog.print(TAG, "创建章节实体失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * YaoFang 数据库实体 → Fang 模型（解密）
     */
    public static Fang convertYaoFangToFang(YaoFang yaoFang) {
        if (yaoFang == null) {
            return null;
        }

        try {
            Fang fang = new Fang();
            fang.setYaoCount(yaoFang.getYaoCount());
            fang.setName(yaoFang.getName());
            fang.setID(yaoFang.getID());
            fang.setDrinkNum(yaoFang.getDrinkNum());
            fang.setText(decryptIfNotEmpty(yaoFang.getText()));
            fang.setFangList(splitStringToList(yaoFang.getFangList(), false));
            fang.setYaoList(splitStringToList(yaoFang.getYaoList(), false));

            // 转换药味明细
            if (yaoFang.getStandardYaoList() != null) {
                for (YaoFangBody content : yaoFang.getStandardYaoList()) {
                    YaoUse yaoUse = convertYaoFangBodyToYaoUse(content);
                    if (yaoUse != null) {
                        fang.setStandardYaoList(yaoUse);
                    }
                }
            }

            return fang;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换方剂数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * YaoFangBody 数据库实体 → YaoUse 模型
     */
    public static YaoUse convertYaoFangBodyToYaoUse(YaoFangBody content) {
        if (content == null) {
            return null;
        }

        try {
            YaoUse yaoUse = new YaoUse();
            yaoUse.setSuffix(content.getSuffix());
            yaoUse.setAmount(content.getAmount());
            yaoUse.setYaoID(content.getYaoID());
            yaoUse.setWeight(content.getWeight());
            yaoUse.setShowName(content.getShowName());
            yaoUse.setExtraProcess(content.getExtraProcess());
            yaoUse.setSignatureId(content.getSignatureId());
            yaoUse.setSignature(content.getSignature());
            return yaoUse;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换药味数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * BookChapterBody 数据库实体 → DataItem 模型（解密）
     */
    public static DataItem convertBookChapterBodyToDataItem(BookChapterBody bookChapterBody) {
        if (bookChapterBody == null) {
            return null;
        }

        try {
            DataItem content = new DataItem();
            content.setText(decryptIfNotEmpty(bookChapterBody.getText()));
            content.setNote(decryptIfNotEmpty(bookChapterBody.getNote()));
            content.setSectionvideo(decryptIfNotEmpty(bookChapterBody.getSectionvideo()));
            content.setID(bookChapterBody.getID());
            content.setSignature(bookChapterBody.getSignature());
            content.setSignatureId(bookChapterBody.getSignatureId());
            content.setFangList(splitStringToList(bookChapterBody.getFangList(), false));
            return content;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换章节内容失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * ZhongYao 数据库实体 → Yao 模型（解密）
     */
    public static Yao convertZhongYaoToYao(ZhongYao zhongYao) {
        if (zhongYao == null) {
            return null;
        }

        try {
            Yao yao = new Yao();
            yao.setText(decryptIfNotEmpty(zhongYao.getText()));
            yao.setName(zhongYao.getName());
            yao.setYaoList(splitStringToList(zhongYao.getYaoList(), true));
            yao.setID(zhongYao.getID());
            yao.setSignature(zhongYao.getSignature());
            yao.setSignatureId(zhongYao.getSignatureId());
            return yao;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换药材数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * BeiMingCi 数据库实体 → MingCiContent 模型（解密）
     */
    public static MingCiContent convertBeiMingCiToMingCiContent(BeiMingCi beiMingCi) {
        if (beiMingCi == null) {
            return null;
        }

        try {
            MingCiContent content = new MingCiContent();
            content.setText(decryptIfNotEmpty(beiMingCi.getText()));
            content.setName(beiMingCi.getName());
            content.setYaoList(splitStringToList(beiMingCi.getMingCiList(), false));
            content.setID(beiMingCi.getID());
            content.setSignature(beiMingCi.getSignature());
            content.setSignatureId(beiMingCi.getSignatureId());
            content.setImageUrl(beiMingCi.getImageUrl());
            return content;
        } catch (Exception e) {
            EasyLog.print(TAG, "转换名词数据失败: " + e.getMessage());
            return null;
        }
    }
}
