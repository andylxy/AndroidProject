package run.yigou.gxzy.data.local.helper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.crypto.SecurityUtils;
import run.yigou.gxzy.data.local.entity.About;
import run.yigou.gxzy.data.local.entity.BeiMingCi;
import run.yigou.gxzy.data.local.entity.BookChapter;
import run.yigou.gxzy.data.local.entity.BookChapterBody;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.YaoFang;
import run.yigou.gxzy.data.local.entity.YaoFangBody;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.gen.BookChapterDao;
import run.yigou.gxzy.data.local.gen.YaoFangDao;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoUse;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.StringHelper;

/**
 * 数据实体转换工具类（重构后精简版）
 *
 * 专注于纯实体转换、加解密和序列化工具。
 * 数据库 CRUD 操作已迁移至 {@link DataRepository}，
 *
 * 职责划分：
 * - ConvertEntity：实体⇔模型转换、RC4加解密、字符串序列化
 * - {@link DataRepository}：数据库读写操作编排
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
    private static final String REGEX_SEPARATOR = "[,，、.;]";

    // ==================== 数据库操作工具（供 DataRepository / NetworkDataFetcher 使用） ====================

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

    // ==================== 更新判断工具（供 DataRepository / NetworkDataFetcher 调用） ====================

    /**
     * 判断导航子项是否需要更新（章节数变化时需要重新下载）
     */
    public static boolean shouldUpdateTabNavBody(ArrayList<TabNavBody> existingBodyList, int newChapterCount) {
        if (existingBodyList == null || existingBodyList.isEmpty()) {
            return true;
        }
        return existingBodyList.get(0).getChapterCount() != newChapterCount;
    }

    /**
     * 判断章节数据是否需要更新（数量变化时需要重新下载）
     */
    public static boolean shouldUpdateChapters(ArrayList<Chapter> existingChapters, int expectedChapterCount) {
        if (existingChapters == null || existingChapters.isEmpty()) {
            return true;
        }
        return existingChapters.size() != expectedChapterCount;
    }

    // ==================== 数据库读取方法 ====================

    /**
     * 获取药材别名列表
     */
    public static List<ZhongYaoAlia> getYaoAlia() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mYaoAliasService.findAll(),
            "获取药材别名"
        );
    }

    /**
     * 获取关于信息列表
     */
    public static List<About> getAbout() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mAboutService.findAll(),
            "获取关于信息"
        );
    }

    /**
     * 根据章节获取内容详情列表（解密后返回）
     *
     * @param chapter 章节标识
     * @return 内容列表
     */
    public static List<DataItem> getBookChapterDetailList(Chapter chapter) {
        if (chapter == null || chapter.getSignatureId() == null) {
            EasyLog.print(TAG, "章节参数无效");
            return new ArrayList<>();
        }

        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(
                BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
            "查询章节" + chapter.getSignatureId() + "的内容"
        );

        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "章节 " + chapter.getSignatureId() + " 无内容数据");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<DataItem> dataList = new ArrayList<>();
            int validChapterCount = 0;
            int validBodyCount = 0;

            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue;
                }
                validChapterCount++;

                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    if (bookChapterBody == null) {
                        continue;
                    }

                    DataItem content = convertBookChapterBodyToDataItem(bookChapterBody);
                    if (content != null) {
                        dataList.add(content);
                        validBodyCount++;
                    }
                }
            }

            EasyLog.print(TAG, "读取 " + dataList.size() + " 条内容 (有效章节:" + validChapterCount + ", 有效内容:" + validBodyCount + ")");
            return dataList;

        }, "读取章节内容");
    }

    /**
     * 根据书籍ID获取章节详情列表（按 Section 分组）
     *
     * @param bookId 书籍ID
     * @return 按章节分组的内容列表
     */
    public static List<HH2SectionData> getBookChapterDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }

        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
            "查询书籍" + bookId + "的章节"
        );

        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "书籍 " + bookId + " 无章节数据");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<HH2SectionData> detailList = new ArrayList<>();
            int validChapterCount = 0;
            int validContentCount = 0;

            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue;
                }

                List<DataItem> dataList = new ArrayList<>();
                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    if (bookChapterBody == null) {
                        continue;
                    }

                    DataItem content = convertBookChapterBodyToDataItem(bookChapterBody);
                    if (content != null) {
                        dataList.add(content);
                        validContentCount++;
                    }
                }

                if (!dataList.isEmpty()) {
                    detailList.add(new HH2SectionData(dataList, bookChapter.getSection(), bookChapter.getHeader()));
                    validChapterCount++;
                }
            }

            EasyLog.print(TAG, "读取书籍 " + bookId + " 共 " + detailList.size() + " 章 (有效章节:" + validChapterCount + ", 有效内容:" + validContentCount + ")");
            return detailList;

        }, "读取书籍章节");
    }

    /**
     * 根据书籍ID获取方剂详情列表（解密后返回）
     *
     * @param bookId 书籍ID
     * @return 方剂列表
     */
    public static ArrayList<Fang> getFangDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }

        ArrayList<YaoFang> fangList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId)),
            "查询书籍" + bookId + "的方剂"
        );

        if (fangList == null || fangList.isEmpty()) {
            EasyLog.print(TAG, "书籍 " + bookId + " 无方剂数据");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            ArrayList<Fang> detailList = new ArrayList<>();

            for (YaoFang yaoFang : fangList) {
                if (yaoFang == null) {
                    continue;
                }

                Fang fang = convertYaoFangToFang(yaoFang);
                if (fang != null) {
                    detailList.add(fang);
                }
            }

            EasyLog.print(TAG, "读取 " + detailList.size() + "/" + fangList.size() + " 个方剂数据");
            return detailList;

        }, "读取方剂数据");
    }

    /**
     * 获取所有药材数据（解密后返回）
     */
    public static ArrayList<Yao> getYaoData() {
        ArrayList<ZhongYao> yaoList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoService.findAll(),
            "获取药材数据"
        );

        if (yaoList == null || yaoList.isEmpty()) {
            EasyLog.print(TAG, "无药材数据");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            ArrayList<Yao> detailList = new ArrayList<>();

            for (ZhongYao yao : yaoList) {
                if (yao == null) {
                    continue;
                }

                Yao yao1 = convertZhongYaoToYao(yao);
                if (yao1 != null) {
                    detailList.add(yao1);
                }
            }

            EasyLog.print(TAG, "读取 " + detailList.size() + "/" + yaoList.size() + " 条药材数据");
            return detailList;

        }, "转换药材数据");
    }

    /**
     * 获取所有名词数据（解密后返回）
     */
    public static ArrayList<MingCiContent> getMingCi() {
        ArrayList<BeiMingCi> beiMingCiList = executeDatabaseOperation(
            () -> DbService.getInstance().mBeiMingCiService.findAll(),
            "获取名词数据"
        );

        if (beiMingCiList == null || beiMingCiList.isEmpty()) {
            EasyLog.print(TAG, "无名词数据");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            ArrayList<MingCiContent> detailList = new ArrayList<>();

            for (BeiMingCi beiMingCi : beiMingCiList) {
                if (beiMingCi == null) {
                    continue;
                }

                MingCiContent content = convertBeiMingCiToMingCiContent(beiMingCi);
                if (content != null) {
                    detailList.add(content);
                }
            }

            EasyLog.print(TAG, "读取 " + detailList.size() + "/" + beiMingCiList.size() + " 条名词数据");
            return detailList;

        }, "转换名词数据");
    }
}
