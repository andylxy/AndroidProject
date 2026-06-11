package run.yigou.gxzy.greendao.util;

import androidx.annotation.NonNull;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.AiConfigBody;
import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.http.api.ChapterListApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.model.Fang;
import run.yigou.gxzy.model.MingCiContent;
import run.yigou.gxzy.model.Yao;
import run.yigou.gxzy.model.YaoAlia;
import run.yigou.gxzy.model.YaoUse;
import run.yigou.gxzy.model.DataItem;
import run.yigou.gxzy.model.HH2SectionData;

import run.yigou.gxzy.crypto.SecurityUtils;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * ConvertEntity 模块使用指南和最佳实践
 * 
 * 快速添加新转换方法的标准步骤：
 * 
 * 1. 保存数据方法：
 *    - 参数验证：检查输入列表是否为空
 *    - 使用 executeDatabaseOperation() 包装数据库操作
 *    - 删除旧数据，批量插入新数据
 *    - 统计成功数量，提供详细日志
 *    - 返回操作结果（boolean）
 * 
 * 2. 获取数据方法：
 *    - 参数验证：检查查询条件有效性
 *    - 使用 executeDatabaseOperation() 包装数据库查询
 *    - 遍历结果集，转换为业务对象
 *    - 统计转换数量，提供详细日志
 *    - 返回结果列表（ArrayList）
 * 
 * 3. 数据转换方法：
 *    - 参数验证：检查源对象是否为空
 *    - 创建目标对象，设置属性值
 *    - 对需要加密的字段使用 encryptIfNotEmpty()
 *    - 对列表字段使用 listToString() 转换
 *    - 异常处理：记录日志，返回null
 * 
 * 最佳实践：
 * 1. 始终使用 executeDatabaseOperation() 包装数据库操作
 * 2. 始终进行参数验证（空值、有效性检查）
 * 3. 使用 encryptIfNotEmpty() / decryptIfNotEmpty() 处理加密字段
 * 4. 使用 splitStringToList() / listToString() 处理列表字段
 * 5. 提供详细的日志记录（操作前后都有日志）
 * 6. 返回合理的默认值（空列表、false等）
 * 7. 添加完整的JavaDoc注释
 * 8. 保持方法职责单一，易于测试和维护
 * 9. 遵循统一的命名规范和代码风格
 * 10. 在catch块中记录详细的异常信息
 * 
 * 常用工具方法：
 * - executeDatabaseOperation() - 统一的数据库操作执行器，自动处理异常
 * - encryptIfNotEmpty() - 条件加密，只在字符串非空时进行加密
 * - decryptIfNotEmpty() - 条件解密，只在字符串非空时进行解密
 * - splitStringToList() - 将逗号分隔的字符串转换为列表，支持正则表达式
 * - listToString() - 将列表转换为逗号分隔的字符串
 * - StringHelper.getUuid() - 生成唯一的UUID字符串
 * - EasyLog.print() - 统一的日志记录方法
 */

/**
 * 数据实体转换工具类
 * 
 * 主要功能：
 * 1. 网络数据与数据库实体的相互转换
 * 2. 数据加密/解密处理
 * 3. 数据库批量操作
 * 4. 数据格式转换
 * 
 * 使用场景：
 * - 从网络获取数据后保存到本地数据库
 * - 从本地数据库读取数据转换为业务对象
 * - 数据同步和缓存处理
 * 
 * @author Android 轮子哥
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public class ConvertEntity {
    
    /**
     * 日志标签
     */
    private static final String TAG = "ConvertEntity";
    
    /**
     * 列表分隔符
     */
    private static final String LIST_SEPARATOR = ",";
    
    /**
     * 正则表达式分隔符（支持中文逗号、句号等）
     */
    private static final String REGEX_SEPARATOR = "[,，。、.;]";
    
    /**
     * 执行数据库操作，统一异常处理
     * 
     * @param operation 数据库操作
     * @param operationName 操作名称，用于日志记录
     * @param <T> 返回类型
     * @return 操作结果，失败时返回null
     */
    private static <T> T executeDatabaseOperation(DatabaseOperation<T> operation, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            EasyLog.print(TAG, "数据库操作失败 [" + operationName + "]: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 数据库操作接口
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * 条件加密字符串
     * 
     * @param text 要加密的文本
     * @return 加密后的文本，如果输入为空则返回空字符串
     */
    private static String encryptIfNotEmpty(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Encrypt(text);
    }
    
    /**
     * 条件解密字符串
     * 
     * @param encryptedText 要解密的文本
     * @return 解密后的文本，如果输入为空则返回空字符串
     */
    private static String decryptIfNotEmpty(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Decrypt(encryptedText);
    }
    
    /**
     * 将字符串分割为列表
     * 
     * @param text 要分割的字符串
     * @param useRegex 是否使用正则表达式分隔符
     * @return 分割后的列表，如果输入为空则返回空列表
     */
    private static List<String> splitStringToList(String text, boolean useRegex) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String separator = useRegex ? REGEX_SEPARATOR : LIST_SEPARATOR;
        return Arrays.asList(text.split(separator));
    }
    
    /**
     * 将列表转换为字符串
     * 
     * @param list 要转换的列表
     * @return 转换后的字符串，如果列表为空则返回空字符串
     */
    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(LIST_SEPARATOR, list);
    }



/**
     * 保存药物别名数据到数据库
     * 
     * @param yaoAliaList 药物别名列表
     */
    public static void saveYaoAlia(List<YaoAlia> yaoAliaList) {
        if (yaoAliaList == null || yaoAliaList.isEmpty()) {
            EasyLog.print(TAG, "药物别名列表为空，跳过保存");
            return;
        }
        
        try {
            DbService.getInstance().mYaoAliasService.deleteAll();
            
            int successCount = 0;
            for (YaoAlia yaoAlia : yaoAliaList) {
                if (yaoAlia == null) {
                    continue;
                }
                
                ZhongYaoAlia zhongYaoAlia = new ZhongYaoAlia();
                zhongYaoAlia.setName(yaoAlia.getName());
                zhongYaoAlia.setBieming(yaoAlia.getBieming());
                
                try {
                    DbService.getInstance().mYaoAliasService.addEntity(zhongYaoAlia);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "保存药物别名失败: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + successCount + "/" + yaoAliaList.size() + " 个药物别名数据");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存药物别名数据失败: " + e.getMessage());
        }
    }

/**
     * 获取所有药物别名数据
     * 
     * @return 药物别名列表，如果获取失败则返回空列表
     */
    public static List<ZhongYaoAlia> getYaoAlia() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mYaoAliasService.findAll(),
            "获取药物别名数据"
        );
    }

/**
     * 保存关于信息到数据库
     * 
     * @param aboutList 关于信息列表
     */
    public static void saveAbout(List<About> aboutList) {
        if (aboutList == null || aboutList.isEmpty()) {
            EasyLog.print(TAG, "关于信息列表为空，跳过保存");
            return;
        }
        
        try {
            DbService.getInstance().mAboutService.deleteAll();
            
            int successCount = 0;
            for (About about : aboutList) {
                if (about == null) {
                    continue;
                }
                
                try {
                    DbService.getInstance().mAboutService.addEntity(about);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "保存关于信息失败: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + successCount + "/" + aboutList.size() + " 个关于信息数据");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存关于信息数据失败: " + e.getMessage());
        }
    }

/**
     * 获取所有关于信息
     * 
     * @return 关于信息列表，如果获取失败则返回空列表
     */
    public static List<About> getAbout() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mAboutService.findAll(),
            "获取关于信息"
        );
    }

/**
     * 保存导航数据到数据库
     * 
     * @param bookNavList 导航数据列表
     * @param homeFragment 主页片段，用于获取章节列表
     */
    public static void saveTabNvaInDb(List<TabNav> bookNavList, HomeFragment homeFragment) {
        if (bookNavList == null || bookNavList.isEmpty()) {
            EasyLog.print(TAG, "导航数据列表为空，跳过保存");
            return;
        }
        
        if (homeFragment == null) {
            EasyLog.print(TAG, "HomeFragment为空，无法获取章节列表");
            return;
        }
        
        int order = 0;
        int processedNavCount = 0;
        int processedBodyCount = 0;
        
        for (TabNav nav : bookNavList) {
            if (nav == null || nav.getNavList() == null || nav.getNavList().isEmpty()) {
                continue; // 跳过无效的导航数据
            }
            
            String tabNavId = processTabNav(nav, order);
            if (tabNavId == null) {
                continue;
            }
            order++;
            processedNavCount++;
            
            // 处理导航内容
            for (TabNavBody item : nav.getNavList()) {
                if (item == null) {
                    continue;
                }
                
                if (processTabNavBody(item, tabNavId, homeFragment)) {
                    processedBodyCount++;
                }
            }
        }
        
        EasyLog.print(TAG, "导航数据保存完成：处理了 " + processedNavCount + " 个导航，" + processedBodyCount + " 个内容项");
    }
    
    /**
     * 处理单个导航数据
     * 
     * @param nav 导航数据
     * @param order 排序序号
     * @return 导航ID，如果处理失败则返回null
     */
    private static String processTabNav(TabNav nav, int order) {
        try {
            // 检查是否已存在
            ArrayList<TabNav> existingNavList = executeDatabaseOperation(() ->
                DbService.getInstance().mTabNavService.find(TabNavDao.Properties.CaseId.eq(nav.getCaseId())),
                "查询导航" + nav.getCaseId()
            );
            
            if (existingNavList != null && !existingNavList.isEmpty()) {
                EasyLog.print(TAG, "导航已存在，使用现有ID: " + nav.getCaseId());
                return existingNavList.get(0).getTabNavId();
            }
            
            // 创建新导航
            String tabNavId = StringHelper.getUuid();
            nav.setTabNavId(tabNavId);
            nav.setOrder(order);
            
            executeDatabaseOperation(() -> {
                DbService.getInstance().mTabNavService.addEntity(nav);
                return true;
            }, "保存导航" + nav.getCaseId());
            
            EasyLog.print(TAG, "成功保存导航: " + nav.getCaseId());
            return tabNavId;
            
        } catch (Exception e) {
            EasyLog.print(TAG, "处理导航失败 " + nav.getCaseId() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 处理单个导航内容
     * 
     * @param item 导航内容
     * @param tabNavId 导航ID
     * @param homeFragment 主页片段
     * @return 是否处理成功
     */
    private static boolean processTabNavBody(TabNavBody item, String tabNavId, HomeFragment homeFragment) {
        try {
            // 检查是否需要更新（章节数量发生变化）
            ArrayList<TabNavBody> existingBodyList = executeDatabaseOperation(() ->
                DbService.getInstance().mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(item.getBookNo())),
                "查询导航内容" + item.getBookNo()
            );
            
            boolean needsUpdate = shouldUpdateTabNavBody(existingBodyList, item.getChapterCount());
            
            if (!needsUpdate) {
                EasyLog.print(TAG, "导航内容无需更新: " + item.getBookNo());
                return true;
            }
            
            // 更新或创建导航内容
            item.setTabNavId(tabNavId);
            item.setTabNavBodyId(StringHelper.getUuid());
            
            executeDatabaseOperation(() -> {
                DbService.getInstance().mTabNavBodyService.addEntity(item);
                return true;
            }, "保存导航内容" + item.getBookNo());
            
            EasyLog.print(TAG, "成功保存导航内容，异步获取章节列表: " + item.getBookNo());
            // 异步获取章节列表
            ThreadUtil.runInBackground(() -> getChapterList(homeFragment, item));
            return true;
            
        } catch (Exception e) {
            EasyLog.print(TAG, "处理导航内容失败 " + item.getBookNo() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断是否需要更新导航内容
     * 
     * @param existingBodyList 现有内容列表
     * @param newChapterCount 新章节数量
     * @return 是否需要更新
     */
    private static boolean shouldUpdateTabNavBody(ArrayList<TabNavBody> existingBodyList, int newChapterCount) {
        if (existingBodyList == null || existingBodyList.isEmpty()) {
            return true; // 没有现有数据，需要更新
        }
        
        // 如果现有章节数量与期望数量不一致，需要更新
        return existingBodyList.get(0).getChapterCount() != newChapterCount;
    }

/**
     * 获取书籍章节列表并保存到数据库
     * 
     * @param homeFragment 主页片段
     * @param item 导航内容项
     */
    public static void getChapterList(HomeFragment homeFragment, TabNavBody item) {
        if (homeFragment == null || item == null) {
            EasyLog.print(TAG, "参数无效，无法获取章节列表");
            return;
        }
        
        EasyHttp.get(homeFragment)
                .api(new ChapterListApi().setBookId(item.getBookNo()))
                .request(new HttpCallback<HttpData<List<Chapter>>>(homeFragment) {
                    @Override
                    public void onSucceed(HttpData<List<Chapter>> data) {
                        if (data == null || data.getData() == null || data.getData().isEmpty()) {
                            EasyLog.print(TAG, "书籍" + item.getBookNo() + "的章节数据为空");
                            return;
                        }
                        
                        processChapterList(data.getData(), item);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "获取书籍" + item.getBookNo() + "章节列表失败: " + e.getMessage());
                        super.onFail(e);
                    }
                });
    }
    
    /**
     * 处理章节列表数据
     * 
     * @param chapters 章节列表
     * @param item 导航内容项
     */
    private static void processChapterList(List<Chapter> chapters, TabNavBody item) {
        try {
            // 检查现有章节数据
            ArrayList<Chapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo())),
                "查询书籍" + item.getBookNo() + "的现有章节"
            );
            
            boolean needsUpdate = shouldUpdateChapters(existingChapters, chapters.size(), item.getChapterCount());
            
            if (!needsUpdate) {
                EasyLog.print(TAG, "书籍" + item.getBookNo() + "的章节数据无需更新");
                return;
            }
            
            // 删除旧数据并保存新数据
            if (existingChapters != null && !existingChapters.isEmpty()) {
                DbService.getInstance().mChapterService.deleteAll(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                EasyLog.print(TAG, "已删除书籍" + item.getBookNo() + "的旧章节数据");
            }
            
            // 批量保存新章节
            int successCount = saveChaptersBatch(chapters, item.getBookNo());
            EasyLog.print(TAG, "成功保存书籍" + item.getBookNo() + "的 " + successCount + "/" + chapters.size() + " 个章节");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "处理书籍" + item.getBookNo() + "章节数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 判断是否需要更新章节数据
     * 
     * @param existingChapters 现有章节列表
     * @param newChapterCount 新章节数量
     * @param expectedChapterCount 期望的章节数量
     * @return 是否需要更新
     */
    private static boolean shouldUpdateChapters(ArrayList<Chapter> existingChapters, int newChapterCount, int expectedChapterCount) {
        if (existingChapters == null || existingChapters.isEmpty()) {
            return true; // 没有现有数据，需要更新
        }
        
        // 如果现有章节数量与期望数量不一致，需要更新
        return existingChapters.size() != expectedChapterCount;
    }
    
    /**
     * 批量保存章节数据
     * 
     * @param chapters 章节列表
     * @param bookId 书籍ID
     * @return 成功保存的章节数量
     */
    private static int saveChaptersBatch(List<Chapter> chapters, int bookId) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            
            try {
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mChapterService.addEntity(chapter);
                    return true;
                }, "保存章节" + chapter.getId());
                successCount++;
            } catch (Exception e) {
                EasyLog.print(TAG, "保存章节失败: " + e.getMessage());
            }
        }
        
        return successCount;
    }

/**
     * 保存名词内容数据到数据库
     * 
     * @param detailList 名词内容列表
     */
    public static void saveMingCiContent(List<MingCiContent> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "名词内容列表为空，跳过保存");
            return;
        }
        
        try {
            DbService.getInstance().mBeiMingCiService.deleteAll();
            
            int successCount = 0;
            for (MingCiContent mingCiContent : detailList) {
                if (mingCiContent == null) {
                    continue;
                }
                
                BeiMingCi beiMingCi = new BeiMingCi();
                beiMingCi.setText(encryptIfNotEmpty(mingCiContent.getText()));
                beiMingCi.setName(mingCiContent.getName());
                beiMingCi.setMingCiList(listToString(mingCiContent.getYaoList()));
                beiMingCi.setSignature(mingCiContent.getSignature());
                beiMingCi.setSignatureId(mingCiContent.getSignatureId());
                beiMingCi.setImageUrl(mingCiContent.getImageUrl());
                beiMingCi.setID(mingCiContent.getID());
                
                try {
                    DbService.getInstance().mBeiMingCiService.addEntity(beiMingCi);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "保存名词内容失败: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + successCount + "/" + detailList.size() + " 个名词内容数据");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存名词内容数据失败: " + e.getMessage());
        }
    }

/**
     * 保存药物数据到数据库
     * 
     * @param detailList 药物数据列表
     */
    public static void saveYaoData(List<Yao> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "药物数据列表为空，跳过保存");
            return;
        }
        
        try {
            DbService.getInstance().mYaoService.deleteAll();
            
            int successCount = 0;
            for (Yao yao : detailList) {
                if (yao == null) {
                    continue;
                }
                
                ZhongYao zhongYao = new ZhongYao();
                zhongYao.setText(encryptIfNotEmpty(yao.getText()));
                zhongYao.setName(yao.getName());
                zhongYao.setYaoList(listToString(yao.getYaoList()));
                zhongYao.setID(yao.getID());
                zhongYao.setSignature(yao.getSignature());
                zhongYao.setSignatureId(yao.getSignatureId());
                
                try {
                    DbService.getInstance().mYaoService.addEntity(zhongYao);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "保存药物数据失败: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + successCount + "/" + detailList.size() + " 个药物数据");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存药物数据失败: " + e.getMessage());
        }
    }

/**
     * 保存方剂详情数据到数据库
     * 
     * @param netFangDetailList 方剂详情列表
     * @param bookId 书籍ID
     */
    public static void getFangDetailList(List<Fang> netFangDetailList, int bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print(TAG, "方剂详情列表为空或书籍ID无效，跳过保存");
            return;
        }

        executeDatabaseOperation(() -> {
            // 删除旧的方剂数据
            ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId));
            if (yaoFangList != null && !yaoFangList.isEmpty()) {
                for (YaoFang fang : yaoFangList) {
                    if (fang != null) {
                        DbService.getInstance().mYaoFangBodyService.deleteAll(YaoFangBodyDao.Properties.YaoFangID.eq(fang.getYaoFangID()));
                        DbService.getInstance().mYaoFangService.deleteEntity(fang);
                    }
                }
                EasyLog.print(TAG, "已删除书籍" + bookId + "的旧方剂数据");
            }

            // 保存新的方剂数据
            int successCount = 0;
            for (Fang fang : netFangDetailList) {
                if (fang == null) {
                    continue;
                }
                
                String yaoFangId = StringHelper.getUuid();
                YaoFang yaoFang = convertFangToYaoFang(fang, bookId, yaoFangId);
                if (yaoFang != null) {
                    DbService.getInstance().mYaoFangService.addEntity(yaoFang);
                    successCount++;
                    
                    // 保存方剂内容
                    saveYaoFangBodies(fang.getStandardYaoList(), yaoFangId);
                }
            }
            
            EasyLog.print(TAG, "成功保存书籍" + bookId + "的 " + successCount + "/" + netFangDetailList.size() + " 个方剂数据");
            return null;
            
        }, "保存方剂详情数据");
    }
    
    /**
     * 将Fang转换为YaoFang
     * 
     * @param fang 方剂对象
     * @param bookId 书籍ID
     * @param yaoFangId 方剂ID
     * @return 转换后的YaoFang实体
     */
    private static YaoFang convertFangToYaoFang(Fang fang, int bookId, String yaoFangId) {
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
            EasyLog.print(TAG, "转换方剂数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存方剂内容列表
     * 
     * @param yaoUseList 药物使用列表
     * @param yaoFangId 方剂ID
     */
    private static void saveYaoFangBodies(List<YaoUse> yaoUseList, String yaoFangId) {
        if (yaoUseList == null || yaoUseList.isEmpty() || yaoFangId == null) {
            return;
        }
        
        int successCount = 0;
        for (YaoUse content : yaoUseList) {
            if (content == null) {
                continue;
            }
            
            YaoFangBody yaoFangBody = convertYaoUseToYaoFangBody(content, yaoFangId);
            if (yaoFangBody != null) {
                DbService.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
                successCount++;
            }
        }
        
        if (successCount > 0) {
            EasyLog.print(TAG, "成功保存方剂" + yaoFangId + "的 " + successCount + "/" + yaoUseList.size() + " 个内容项");
        }
    }

/**
     * 将YaoUse转换为YaoFangBody
     * 
     * @param content 药物使用对象
     * @param yaoFangId 方剂ID
     * @return 转换后的YaoFangBody实体
     */
    private static @NonNull YaoFangBody convertYaoUseToYaoFangBody(YaoUse content, String yaoFangId) {
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
     * 根据章节签名ID获取章节详情列表
     * 
     * @param chapter 章节对象
     * @return 章节详情列表，如果获取失败则返回空列表
     */
    public static List<DataItem> getBookChapterDetailList(Chapter chapter) {
        if (chapter == null || chapter.getSignatureId() == null) {
            EasyLog.print(TAG, "章节对象无效");
            return new ArrayList<>();
        }
        
        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
            "获取章节" + chapter.getSignatureId() + "的详情数据"
        );
        
        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "章节" + chapter.getSignatureId() + "的详情数据为空");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<DataItem> dataList = new ArrayList<>();
            int validChapterCount = 0;
            int validBodyCount = 0;
            
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue; // 跳过无效的章节
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
            
            EasyLog.print(TAG, "成功转换 " + dataList.size() + " 个章节内容 (有效章节:" + validChapterCount + ", 有效内容:" + validBodyCount + ")");
            return dataList;
            
        }, "转换章节详情数据");
    }
    
    /**
     * 将BookChapterBody转换为DataItem
     * 
     * @param bookChapterBody 章节内容实体
     * @return 转换后的数据项，如果转换失败则返回null
     */
    private static DataItem convertBookChapterBodyToDataItem(BookChapterBody bookChapterBody) {
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
     * 保存AI配置列表到数据库
     * 
     * @param aiConfigs AI配置列表
     * @return 是否保存成功
     */
    public static boolean saveAiConfigList(List<AiConfig> aiConfigs) {
        if (aiConfigs == null || aiConfigs.isEmpty()) {
            EasyLog.print(TAG, "AI配置列表为空，跳过保存");
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // 删除旧数据
            DbService.getInstance().mAiConfigService.deleteAll();
            DbService.getInstance().mAiConfigBodyService.deleteAll();
            
            int configCount = 0;
            int bodyCount = 0;
            
            for (AiConfig aiConfig : aiConfigs) {
                if (aiConfig == null) {
                    continue;
                }
                
                String aiConfigId = StringHelper.getUuid();
                aiConfig.setAiConfigId(aiConfigId);
                
                // 加密API密钥
                if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
                    aiConfig.setApiKey(encryptIfNotEmpty(aiConfig.getApiKey()));
                }
                
                DbService.getInstance().mAiConfigService.addEntity(aiConfig);
                configCount++;

                // 保存配置体
                if (aiConfig.getModelList() != null) {
                    for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                        if (aiConfigBody == null) {
                            continue;
                        }
                        
                        aiConfigBody.setAiConfigBodyId(StringHelper.getUuid());
                        aiConfigBody.setAiConfigId(aiConfigId);
                        DbService.getInstance().mAiConfigBodyService.addEntity(aiConfigBody);
                        bodyCount++;
                    }
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + configCount + " 个AI配置，" + bodyCount + " 个配置体");
            return true;
            
        }, "保存AI配置数据");
    }

/**
     * 保存章节详情数据到数据库
     * 
     * @param chapter 章节对象
     * @param netDetailList 网络详情数据列表
     * @return 是否保存成功
     */
    public static boolean saveBookChapterDetailList(Chapter chapter, List<HH2SectionData> netDetailList) {
        if (chapter == null || netDetailList == null || netDetailList.isEmpty() || chapter.getBookId() <= 0) {
            EasyLog.print(TAG, "参数无效：chapter=" + chapter + ", netDetailList大小=" + 
                       (netDetailList != null ? netDetailList.size() : "null") + ", bookId=" + chapter.getBookId());
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // 删除旧数据
            ArrayList<BookChapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
                "查询现有章节" + chapter.getSignatureId()
            );
            
            if (existingChapters != null && !existingChapters.isEmpty()) {
                for (BookChapter existingChapter : existingChapters) {
                    if (existingChapter != null) {
                        DbService.getInstance().mBookChapterBodyService
                                .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(existingChapter.getBookChapterId()));
                        DbService.getInstance().mBookChapterService.deleteEntity(existingChapter);
                    }
                }
                EasyLog.print(TAG, "已删除 " + existingChapters.size() + " 个现有章节数据");
            }
            
            // 保存新数据
            int sectionCount = 0;
            int contentCount = 0;
            
            for (HH2SectionData sectionData : netDetailList) {
                if (sectionData == null) {
                    continue;
                }
                
                String chapterId = StringHelper.getUuid();
                
                // 创建章节
                BookChapter bookChapter = createBookChapter(chapter, sectionData, chapterId);
                if (bookChapter == null) {
                    continue;
                }
                
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
                    return true;
                }, "保存章节" + bookChapter.getSignatureId());
                
                sectionCount++;
                
                // 保存章节内容
                if (sectionData.getData() != null) {
                    for (DataItem content : sectionData.getData()) {
                        if (content == null) {
                            continue;
                        }
                        
                        BookChapterBody bookChapterBody = createBookChapterBody(chapterId, content);
                        if (bookChapterBody != null) {
                            executeDatabaseOperation(() -> {
                                DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                                return true;
                            }, "保存章节内容" + content.getID());
                            
                            contentCount++;
                        }
                    }
                }
            }
            
            EasyLog.print(TAG, "成功保存 " + sectionCount + " 个章节，" + contentCount + " 个内容项");
            return true;
            
        }, "保存章节详情数据");
    }
    
    /**
     * 创建BookChapter实体
     * 
     * @param chapter 原始章节
     * @param sectionData 章节数据
     * @param chapterId 章节ID
     * @return 创建的BookChapter实体
     */
    private static BookChapter createBookChapter(Chapter chapter, HH2SectionData sectionData, String chapterId) {
        if (chapter == null || sectionData == null || chapterId == null) {
            return null;
        }
        
        try {
            BookChapter bookChapter = new BookChapter();
            bookChapter.setSection(sectionData.getSection());
            bookChapter.setHeader(sectionData.getHeader());
            bookChapter.setBookId(chapter.getBookId());
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
     * 创建BookChapterBody实体
     * 
     * @param chapterId 章节ID
     * @param content 内容数据
     * @return 创建的BookChapterBody实体
     */
    private static BookChapterBody createBookChapterBody(String chapterId, DataItem content) {
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
     * 保存书籍详情数据到数据库（根据书籍ID）
     * 
     * @param netDetailList 网络详情数据列表
     * @param bookId 书籍ID
     * @return 是否保存成功
     */
    public static boolean getBookDetailList(List<HH2SectionData> netDetailList, int bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print(TAG, "书籍详情列表为空或书籍ID无效，跳过保存");
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // 删除旧数据
            ArrayList<BookChapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
                "查询书籍" + bookId + "的现有章节"
            );
            
            if (existingChapters != null && !existingChapters.isEmpty()) {
                for (BookChapter existingChapter : existingChapters) {
                    if (existingChapter != null) {
                        DbService.getInstance().mBookChapterBodyService
                                .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(existingChapter.getBookChapterId()));
                        DbService.getInstance().mBookChapterService.deleteEntity(existingChapter);
                    }
                }
                EasyLog.print(TAG, "已删除书籍" + bookId + "的 " + existingChapters.size() + " 个现有章节数据");
            }
            
            // 保存新数据
            int sectionCount = 0;
            int contentCount = 0;
            
            for (HH2SectionData sectionData : netDetailList) {
                if (sectionData == null) {
                    continue;
                }
                
                String chapterId = StringHelper.getUuid();
                
                // 创建章节
                BookChapter bookChapter = createBookChapterByBookId(bookId, sectionData, chapterId);
                if (bookChapter == null) {
                    continue;
                }
                
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
                    return true;
                }, "保存书籍章节" + bookChapter.getSignatureId());
                
                sectionCount++;
                
                // 保存章节内容
                if (sectionData.getData() != null) {
                    for (DataItem content : sectionData.getData()) {
                        if (content == null) {
                            continue;
                        }
                        
                        BookChapterBody bookChapterBody = createBookChapterBody(chapterId, content);
                        if (bookChapterBody != null) {
                            executeDatabaseOperation(() -> {
                                DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                                return true;
                            }, "保存书籍章节内容" + content.getID());
                            
                            contentCount++;
                        }
                    }
                }
            }
            
            EasyLog.print(TAG, "成功保存书籍" + bookId + "的 " + sectionCount + " 个章节，" + contentCount + " 个内容项");
            return true;
            
        }, "保存书籍详情数据");
    }
    
    /**
     * 创建BookChapter实体（根据书籍ID）
     * 
     * @param bookId 书籍ID
     * @param sectionData 章节数据
     * @param chapterId 章节ID
     * @return 创建的BookChapter实体
     */
    private static BookChapter createBookChapterByBookId(int bookId, HH2SectionData sectionData, String chapterId) {
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
            EasyLog.print(TAG, "创建书籍章节实体失败: " + e.getMessage());
            return null;
        }
    }

/**
     * 根据书籍ID获取章节详情列表
     * 
     * @param bookId 书籍ID
     * @return 章节详情列表，如果获取失败则返回空列表
     */
    public static List<HH2SectionData> getBookChapterDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }
        
        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
            "获取书籍" + bookId + "的章节数据"
        );
        
        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "书籍" + bookId + "的章节数据为空");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<HH2SectionData> detailList = new ArrayList<>();
            int validChapterCount = 0;
            int validContentCount = 0;
            
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue; // 跳过无效的章节
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
            
            EasyLog.print(TAG, "成功转换书籍" + bookId + "的 " + detailList.size() + " 个章节 (有效章节:" + validChapterCount + ", 有效内容:" + validContentCount + ")");
            return detailList;
            
        }, "转换书籍章节数据");
    }

/**
     * 根据书籍ID获取方剂详情列表
     * 
     * @param bookId 书籍ID
     * @return 方剂详情列表，如果获取失败则返回空列表
     */
    public static ArrayList<Fang> getFangDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }
        
        ArrayList<YaoFang> fangList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId)),
            "获取书籍" + bookId + "的方剂数据"
        );
        
        if (fangList == null || fangList.isEmpty()) {
            EasyLog.print(TAG, "书籍" + bookId + "的方剂数据为空");
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
            
            EasyLog.print(TAG, "成功转换 " + detailList.size() + "/" + fangList.size() + " 个方剂数据");
            return detailList;
            
        }, "转换方剂数据");
    }
    
    /**
     * 将YaoFang实体转换为Fang对象
     * 
     * @param yaoFang YaoFang实体
     * @return 转换后的Fang对象
     */
    private static Fang convertYaoFangToFang(YaoFang yaoFang) {
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
            fang.setID(yaoFang.getID());
            
            // 转换方剂内容列表
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
     * 将YaoFangBody实体转换为YaoUse对象
     * 
     * @param content YaoFangBody实体
     * @return 转换后的YaoUse对象
     */
    private static YaoUse convertYaoFangBodyToYaoUse(YaoFangBody content) {
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
            EasyLog.print(TAG, "转换方剂内容失败: " + e.getMessage());
            return null;
        }
    }

/**
     * 获取所有药物数据
     * 
     * @return 药物数据列表，如果获取失败则返回空列表
     */
    public static ArrayList<Yao> getYaoData() {
        ArrayList<ZhongYao> yaoList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoService.findAll(),
            "获取药物数据"
        );
        
        if (yaoList == null || yaoList.isEmpty()) {
            EasyLog.print(TAG, "药物数据为空");
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
            
            EasyLog.print(TAG, "成功转换 " + detailList.size() + "/" + yaoList.size() + " 个药物数据");
            return detailList;
            
        }, "转换药物数据");
    }
    
    /**
     * 将ZhongYao实体转换为Yao对象
     * 
     * @param zhongYao ZhongYao实体
     * @return 转换后的Yao对象
     */
    private static Yao convertZhongYaoToYao(ZhongYao zhongYao) {
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
            EasyLog.print(TAG, "转换药物数据失败: " + e.getMessage());
            return null;
        }
    }

/**
     * 获取所有名词内容数据
     * 
     * @return 名词内容列表，如果获取失败则返回空列表
     */
    public static ArrayList<MingCiContent> getMingCi() {
        ArrayList<BeiMingCi> beiMingCiList = executeDatabaseOperation(
            () -> DbService.getInstance().mBeiMingCiService.findAll(),
            "获取名词内容数据"
        );
        
        if (beiMingCiList == null || beiMingCiList.isEmpty()) {
            EasyLog.print(TAG, "名词内容数据为空");
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
            
            EasyLog.print(TAG, "成功转换 " + detailList.size() + "/" + beiMingCiList.size() + " 个名词内容数据");
            return detailList;
            
        }, "转换名词内容数据");
    }
    
    /**
     * 将BeiMingCi实体转换为MingCiContent对象
     * 
     * @param beiMingCi BeiMingCi实体
     * @return 转换后的MingCiContent对象
     */
    private static MingCiContent convertBeiMingCiToMingCiContent(BeiMingCi beiMingCi) {
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
            EasyLog.print(TAG, "转换名词内容失败: " + e.getMessage());
            return null;
        }
    }
}