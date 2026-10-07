package run.yigou.gxzy.data.local.helper;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.data.local.entity.About;
import run.yigou.gxzy.data.local.entity.AiConfig;
import run.yigou.gxzy.data.local.entity.AiConfigBody;
import run.yigou.gxzy.data.local.entity.BeiMingCi;
import run.yigou.gxzy.data.local.entity.BookChapter;
import run.yigou.gxzy.data.local.entity.BookChapterBody;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.YaoFang;
import run.yigou.gxzy.data.local.entity.YaoFangBody;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.gen.BookChapterBodyDao;
import run.yigou.gxzy.data.local.gen.BookChapterDao;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.data.local.gen.TabNavBodyDao;
import run.yigou.gxzy.data.local.gen.TabNavDao;
import run.yigou.gxzy.data.local.gen.YaoFangBodyDao;
import run.yigou.gxzy.data.local.gen.YaoFangDao;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoAlia;
import run.yigou.gxzy.data.model.YaoUse;
import run.yigou.gxzy.log.EasyLog;
import androidx.lifecycle.LifecycleOwner;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

import run.yigou.gxzy.data.remote.api.ChapterListApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import org.greenrobot.greendao.query.WhereCondition;

/**
 * 数据库操作仓库类
 *
 * 负责所有数据库 CRUD 操作的编排，从 ConvertEntity 中拆分出来，
 * 使 ConvertEntity 专注于纯实体转换与加解密逻辑。
 *
 * 职责划分：
 * - DataRepository：数据库读写操作编排 + 章节列表网络获取与持久化
 * - ConvertEntity：实体⇔模型转换、加解密、序列化工具
 *
 * @see ConvertEntity 实体转换与加解密工具
 * @since 2024/06/21
 */
public final class DataRepository {

    private static final String TAG = "DataRepository";

    private DataRepository() {
    }

    // ==================== 药材别名 ====================

    /**
     * 保存药材别名列表（全量替换）
     *
     * <p>清空与写入在同一事务内完成（见 {@code BaseService#replaceAllInTx}）：失败整体回滚、
     * 保留旧数据。原实现是「先 deleteAll 再逐条插入 + 逐条吞异常」——失败会留下空表/半截数据；
     * 且 343 条逐条提交实测约 2.7s（事务内批量插入约 0.1s），是启动序列的主要耗时项。
     *
     * @param yaoAliaList 别名数据列表
     */
    public static void saveYaoAlia(List<YaoAlia> yaoAliaList) {
        if (yaoAliaList == null || yaoAliaList.isEmpty()) {
            EasyLog.print(TAG, "药材别名列表为空，跳过保存");
            return;
        }

        try {
            List<ZhongYaoAlia> entities = new ArrayList<>(yaoAliaList.size());
            for (YaoAlia yaoAlia : yaoAliaList) {
                if (yaoAlia == null) {
                    continue;
                }

                ZhongYaoAlia zhongYaoAlia = new ZhongYaoAlia();
                zhongYaoAlia.setName(yaoAlia.getName());
                zhongYaoAlia.setBieming(yaoAlia.getBieming());
                entities.add(zhongYaoAlia);
            }

            LocalServices.getInstance().mYaoAliasService.replaceAllInTx(entities);
            EasyLog.print(TAG, "保存 " + entities.size() + "/" + yaoAliaList.size() + " 条药材别名");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存药材别名失败（已回滚，保留旧数据）: " + e.getMessage());
        }
    }

    // ==================== 关于信息 ====================

    /**
     * 保存关于信息列表（全量替换）
     *
     * @param aboutList 关于数据列表
     */
    public static void saveAbout(List<About> aboutList) {
        if (aboutList == null || aboutList.isEmpty()) {
            EasyLog.print(TAG, "关于信息列表为空，跳过保存");
            return;
        }

        try {
            LocalServices.getInstance().mAboutService.deleteAll();

            int successCount = 0;
            for (About about : aboutList) {
                if (about == null) {
                    continue;
                }

                try {
                    LocalServices.getInstance().mAboutService.addEntity(about);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "保存关于信息失败: " + e.getMessage());
                }
            }

            EasyLog.print(TAG, "保存 " + successCount + "/" + aboutList.size() + " 条关于信息");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存关于信息总异常: " + e.getMessage());
        }
    }

    // ==================== 导航数据 ====================

    /**
     * 保存底部导航数据到数据库
     * 
     * 对每个导航项执行去重检查后写入，并触发章节列表下载。
     *
     * @param bookNavList     导航数据列表
     * @param lifecycleOwner 生命周期宿主，用于网络请求
     */
    public static void saveTabNvaInDb(List<TabNav> bookNavList, LifecycleOwner lifecycleOwner) {
        if (bookNavList == null || bookNavList.isEmpty()) {
            EasyLog.print(TAG, "导航数据列表为空，跳过保存");
            return;
        }

        if (lifecycleOwner == null) {
            EasyLog.print(TAG, "LifecycleOwner 为 null，跳过保存");
            return;
        }

        int order = 0;
        int processedNavCount = 0;
        int processedBodyCount = 0;

        for (TabNav nav : bookNavList) {
            if (nav == null || nav.getNavList() == null || nav.getNavList().isEmpty()) {
                continue;
            }

            String tabNavId = processTabNav(nav, order);
            if (tabNavId == null) {
                continue;
            }
            order++;
            processedNavCount++;

            for (TabNavBody item : nav.getNavList()) {
                if (item == null) {
                    continue;
                }

                if (processTabNavBody(item, tabNavId, lifecycleOwner)) {
                    processedBodyCount++;
                }
            }
        }

        EasyLog.print(TAG, "共处理导航数据 " + processedNavCount + " 个导航, " + processedBodyCount + " 个子项");
    }
    
    /**
     * 全量覆盖保存导航数据（DELETE + INSERT）
     * 
     * <p>用于数据更新场景，确保数据一致性：
     * <ul>
     *   <li>先清空 TabNav 和 TabNavBody 表的所有数据</li>
     *   <li>再插入新的导航数据</li>
     *   <li>自动处理服务端删除的导航分类</li>
     * </ul>
     * 
     * <p>与 saveTabNvaInDb() 的区别：
     * <ul>
     *   <li>saveTabNvaInDb()：增量保存（检查已存在则跳过）</li>
     *   <li>clearAndSaveNavTabs()：全量覆盖（先清空再插入）</li>
     * </ul>
     *
     * @param navList        新的导航数据列表
     * @param lifecycleOwner 生命周期宿主，用于网络请求
     */
    public static void clearAndSaveNavTabs(List<TabNav> navList, LifecycleOwner lifecycleOwner) {
        if (navList == null || navList.isEmpty()) {
            EasyLog.print(TAG, "导航数据列表为空，跳过保存");
            return;
        }
        
        if (lifecycleOwner == null) {
            EasyLog.print(TAG, "LifecycleOwner 为 null，跳过保存");
            return;
        }
        
        EasyLog.print(TAG, "🔄 开始全量覆盖导航数据...");
        
        try {
            // 清空与写入包在同一个事务里：中途失败整体回滚，旧导航数据完整保留。
            // 跨 TabNav / TabNavBody 两张表（分属两个 service），故用 DbService 的跨表事务入口。
            // ⚠️ 事务内抛出的异常必须能传出来，否则会照常提交、留下删了一半的表——
            // 这正是下面 processTabNav / processTabNavBody 不再用 executeDatabaseOperation 的原因。
            DbService dbService = DbService.getInstance();
            dbService.runInTransaction(() -> {
                // 1. 清空旧数据（service 定位已拆到 LocalServices，见 ADR-0001 Q1=C）
                LocalServices.getInstance().mTabNavService.deleteAll();
                LocalServices.getInstance().mTabNavBodyService.deleteAll();
                EasyLog.print(TAG, "✅ 已清空导航数据表");

                // 2. 保存新数据（复用 saveTabNvaInDb 逻辑）
                saveTabNvaInDb(navList, lifecycleOwner);
            });
            
            EasyLog.print(TAG, "🎉 导航数据全量覆盖完成");
        } catch (Exception e) {
            EasyLog.print(TAG, "❌ 导航数据全量覆盖失败（已回滚，保留旧数据）: " + e.getMessage());
            EasyLog.print(e);
        }
    }

    /**
     * 全量覆盖保存导航数据（异步版）：内存态由调用方先行同步（UI 立即可用），
     * 落库本身提交到 {@link DbService#runInBackgroundSerial} 的串行后台线程执行。
     *
     * <p>为什么必须异步：本方法会清空导航表并逐项写入，还会为每本书触发章节列表下载，
     * 同步执行会阻塞主线程（实测约 0.4s）。
     *
     * <p>为什么必须串行：它与紧随其后的「按书删除 + 批量插入」章节落库写同一个 SQLite 库，
     * 并发执行会互相抢锁。
     *
     * <p>失败语义：{@link #clearAndSaveNavTabs} 内部已把「清空 + 写入」包在同一个事务里，
     * 失败会整体回滚并记录日志（旧导航数据保留），不向调用方抛出，
     * 也不影响调用方已同步好的内存态。
     *
     * @param navList        新的导航数据列表
     * @param lifecycleOwner 生命周期宿主，用于章节列表的网络请求
     */
    public static void clearAndSaveNavTabsAsync(List<TabNav> navList, LifecycleOwner lifecycleOwner) {
        DbService.getInstance().runInBackgroundSerial(
            () -> clearAndSaveNavTabs(navList, lifecycleOwner));
    }

    /**
     * 处理单个导航项：检查去重后写入数据库
     *
     * @return tabNavId，失败返回 null
     */
    private static String processTabNav(TabNav nav, int order) {
        try {
            // 直接调用 service，不经 ConvertEntity.executeDatabaseOperation：
            // 那个包装会吞掉异常，导致 clearAndSaveNavTabs 的事务照常提交、留下删了一半的表。
            ArrayList<TabNav> existingNavList = LocalServices.getInstance().mTabNavService.find(
                TabNavDao.Properties.CaseId.eq(nav.getCaseId()));

            if (existingNavList != null && !existingNavList.isEmpty()) {
                EasyLog.print(TAG, "导航已存在，CaseID: " + nav.getCaseId());
                return existingNavList.get(0).getTabNavId();
            }

            // 写入新导航
            String tabNavId = StringHelper.getUuid();
            nav.setTabNavId(tabNavId);
            nav.setOrder(order);

            LocalServices.getInstance().mTabNavService.addEntity(nav);

            EasyLog.print(TAG, "已保存导航: " + nav.getCaseId());
            return tabNavId;

        } catch (Exception e) {
            EasyLog.print(TAG, "处理导航失败 " + nav.getCaseId() + ": " + e.getMessage());
            // 向上抛：让 clearAndSaveNavTabs 的事务整体回滚，保留旧导航数据
            throw new IllegalStateException("处理导航失败: " + nav.getCaseId(), e);
        }
    }

    /**
     * 处理导航子项：检查更新条件后写入，并触发章节下载
     */
    private static boolean processTabNavBody(TabNavBody item, String tabNavId, LifecycleOwner lifecycleOwner) {
        try {
            // 同 processTabNav：直接调用 service，避免异常被吞掉后事务照常提交
            ArrayList<TabNavBody> existingBodyList = LocalServices.getInstance().mTabNavBodyService.find(
                TabNavBodyDao.Properties.BookNo.eq(item.getBookNo()));

            boolean needsUpdate = shouldUpdateTabNavBody(existingBodyList, item.getChapterCount());

            if (!needsUpdate) {
                EasyLog.print(TAG, "导航子项无需更新: " + item.getBookNo());
                return true;
            }

            // 写入子项
            item.setTabNavId(tabNavId);
            item.setTabNavBodyId(StringHelper.getUuid());

            LocalServices.getInstance().mTabNavBodyService.addEntity(item);

            EasyLog.print(TAG, "已保存导航子项，触发章节下载: " + item.getBookNo());
            // 异步下载章节列表（原 NetworkDataFetcher 逻辑已合并）。
            // 只发起网络请求；章节落库走 DbService 串行执行器，会排在本事务之后，不会并发写库。
            // 边界：若本事务随后回滚，已发出的下载仍可能落库成「孤儿章节」——章节按 bookId 存放、
            // 与导航行无外键关系，下次导航保存成功即恢复可见，故不额外处理。
            ThreadUtil.runInBackground(() -> fetchAndSaveChapterList(lifecycleOwner, item));
            return true;

        } catch (Exception e) {
            EasyLog.print(TAG, "处理导航子项失败 " + item.getBookNo() + ": " + e.getMessage());
            // 向上抛：让 clearAndSaveNavTabs 的事务整体回滚，保留旧导航数据
            throw new IllegalStateException("处理导航子项失败: " + item.getBookNo(), e);
        }
    }

    // ==================== 名词数据 ====================

    /**
     * 保存名词内容列表（全量替换，加密存储）
     *
     * <p>清空与写入在同一事务内完成（见 {@code BaseService#replaceAllInTx}）：失败整体回滚、
     * 保留旧数据。原实现是「先 deleteAll 再逐条插入 + 逐条吞异常」，失败会留下空表/半截数据。
     *
     * @param detailList 名词数据列表
     */
    public static void saveMingCiContent(List<MingCiContent> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "名词数据列表为空，跳过保存");
            return;
        }

        try {
            List<BeiMingCi> entities = new ArrayList<>(detailList.size());
            for (MingCiContent mingCiContent : detailList) {
                if (mingCiContent == null) {
                    continue;
                }

                BeiMingCi beiMingCi = new BeiMingCi();
                beiMingCi.setText(ConvertEntity.encryptIfNotEmpty(mingCiContent.getText()));
                beiMingCi.setName(mingCiContent.getName());
                beiMingCi.setMingCiList(ConvertEntity.listToString(mingCiContent.getYaoList()));
                beiMingCi.setSignature(mingCiContent.getSignature());
                beiMingCi.setSignatureId(mingCiContent.getSignatureId());
                beiMingCi.setImageUrl(mingCiContent.getImageUrl());
                beiMingCi.setID(mingCiContent.getID());
                entities.add(beiMingCi);
            }

            LocalServices.getInstance().mBeiMingCiService.replaceAllInTx(entities);
            EasyLog.print(TAG, "保存 " + entities.size() + "/" + detailList.size() + " 条名词数据");
        } catch (Exception e) {
            EasyLog.print(TAG, "保存名词数据失败（已回滚，保留旧数据）: " + e.getMessage());
        }
    }

    // ==================== 药材数据 ====================

    /**
     * 保存药材数据列表（全量替换，加密存储）
     *
     * <p>清空旧数据与写入新数据在同一事务内完成（见 {@code BaseService#replaceAllInTx}）：
     * 写入失败时整体回滚并保留旧数据，避免出现「表被清空且新数据没写进去」的数据丢失。
     *
     * <p>本方法只做数据持久化，不负责内存态（GlobalDataHolder）同步；
     * 调用方需保证在后台线程执行，避免主线程同步写库造成 ANR。
     *
     * @param detailList 药材数据列表
     */
    public static void saveYaoData(List<Yao> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "药材数据列表为空，跳过保存");
            return;
        }

        List<ZhongYao> entities = new ArrayList<>(detailList.size());
        for (Yao yao : detailList) {
            if (yao == null) {
                continue;
            }

            ZhongYao zhongYao = new ZhongYao();
            zhongYao.setText(ConvertEntity.encryptIfNotEmpty(yao.getText()));
            zhongYao.setName(yao.getName());
            zhongYao.setYaoList(ConvertEntity.listToString(yao.getYaoList()));
            zhongYao.setID(yao.getID());
            zhongYao.setSignature(yao.getSignature());
            zhongYao.setSignatureId(yao.getSignatureId());
            entities.add(zhongYao);
        }

        try {
            LocalServices.getInstance().mYaoService.replaceAllInTx(entities);
            EasyLog.print(TAG, "保存 " + entities.size() + "/" + detailList.size() + " 条药材数据");
        } catch (Exception e) {
            // 事务已回滚，旧数据仍在；此处只记录，不重试——重试策略由调用方决定
            EasyLog.print(TAG, "保存药材数据失败（已回滚，保留旧数据）: " + e.getMessage());
        }
    }

    // ==================== 方剂数据 ====================

    /**
     * 保存方剂数据到数据库（先删后插，含药味明细）
     *
     * @param netFangDetailList 方剂数据列表
     * @param bookId            书籍ID
     */
    public static void saveFangDetailList(List<Fang> netFangDetailList, String bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId == null || bookId.isEmpty()) {
            EasyLog.print(TAG, "方剂数据列表为空或书籍ID无效，跳过保存");
            return;
        }

        ConvertEntity.executeDatabaseOperation(() -> {
            // 清除旧数据
            ArrayList<YaoFang> yaoFangList = LocalServices.getInstance().mYaoFangService.find(
                YaoFangDao.Properties.BookId.eq(bookId));
            if (yaoFangList != null && !yaoFangList.isEmpty()) {
                for (YaoFang fang : yaoFangList) {
                    if (fang != null) {
                        LocalServices.getInstance().mYaoFangBodyService.deleteAll(
                            YaoFangBodyDao.Properties.YaoFangID.eq(fang.getYaoFangID()));
                        LocalServices.getInstance().mYaoFangService.deleteEntity(fang);
                    }
                }
                EasyLog.print(TAG, "已删除书籍" + bookId + "旧方剂数据");
            }

            // 保存新数据
            int successCount = 0;
            for (Fang fang : netFangDetailList) {
                if (fang == null) {
                    continue;
                }

                String yaoFangId = StringHelper.getUuid();
                YaoFang yaoFang = ConvertEntity.convertFangToYaoFang(fang, bookId, yaoFangId);
                if (yaoFang != null) {
                    LocalServices.getInstance().mYaoFangService.addEntity(yaoFang);
                    successCount++;

                    // 保存药味明细
                    saveYaoFangBodies(fang.getStandardYaoList(), yaoFangId);
                }
            }

            EasyLog.print(TAG, "保存书籍" + bookId + "共 " + successCount + "/" + netFangDetailList.size() + " 个方剂");
            return null;

        }, "保存方剂数据");
    }

    /**
     * 批量保存药味明细
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

            YaoFangBody yaoFangBody = ConvertEntity.convertYaoUseToYaoFangBody(content, yaoFangId);
            if (yaoFangBody != null) {
                LocalServices.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
                successCount++;
            }
        }

        if (successCount > 0) {
            EasyLog.print(TAG, "保存药味明细 " + yaoFangId + ": " + successCount + "/" + yaoUseList.size() + " 条");
        }
    }

    // ==================== AI配置 ====================

    /**
     * 保存AI配置列表（全量替换，API Key 加密存储）
     *
     * @param aiConfigs AI配置列表
     * @return 保存是否成功
     */
    public static boolean saveAiConfigList(List<AiConfig> aiConfigs) {
        if (aiConfigs == null || aiConfigs.isEmpty()) {
            EasyLog.print(TAG, "AI配置列表为空，跳过保存");
            return false;
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
            // 清除旧数据
            LocalServices.getInstance().mAiConfigService.deleteAll();
            LocalServices.getInstance().mAiConfigBodyService.deleteAll();

            int configCount = 0;
            int bodyCount = 0;

            for (AiConfig aiConfig : aiConfigs) {
                if (aiConfig == null) {
                    continue;
                }

                String aiConfigId = StringHelper.getUuid();
                aiConfig.setAiConfigId(aiConfigId);

                // 加密 API Key
                if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
                    aiConfig.setApiKey(ConvertEntity.encryptIfNotEmpty(aiConfig.getApiKey()));
                }

                LocalServices.getInstance().mAiConfigService.addEntity(aiConfig);
                configCount++;

                // 保存模型列表
                if (aiConfig.getModelList() != null) {
                    for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                        if (aiConfigBody == null) {
                            continue;
                        }

                        aiConfigBody.setAiConfigBodyId(StringHelper.getUuid());
                        aiConfigBody.setAiConfigId(aiConfigId);
                        LocalServices.getInstance().mAiConfigBodyService.addEntity(aiConfigBody);
                        bodyCount++;
                    }
                }
            }

            EasyLog.print(TAG, "保存 " + configCount + " 个AI配置, " + bodyCount + " 个模型");
            return true;

        }, "保存AI配置");
    }

    // ==================== 书籍章节数据 ====================

    /**
     * 保存书籍章节详情列表（按 Chapter 维度，先删后插）
     *
     * @param chapter       章节标识
     * @param netDetailList 章节内容数据
     * @return 保存是否成功
     */
    public static boolean saveBookChapterDetailList(Chapter chapter, List<HH2SectionData> netDetailList) {
        if (chapter == null || netDetailList == null || netDetailList.isEmpty() || chapter.getBookId() == null || chapter.getBookId().isEmpty()) {
            EasyLog.print(TAG, "参数无效: chapter=" + chapter + ", netDetailList长度=" +
                       (netDetailList != null ? netDetailList.size() : "null") + ", bookId=" + (chapter != null ? chapter.getBookId() : -1));
            return false;
        }

        return saveBookChapterData(netDetailList, chapter.getSignatureId(), chapter.getBookId(),
            BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId()),
            "保存章节内容");
    }

    /**
     * 保存书籍详情数据（按 bookId 维度，先删后插）
     *
     * @param netDetailList 章节内容数据
     * @param bookId        书籍ID
     * @return 保存是否成功
     */
    public static boolean saveBookDetailData(List<HH2SectionData> netDetailList, String bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId == null || bookId.isEmpty()) {
            EasyLog.print(TAG, "书籍详情数据为空或书籍ID无效，跳过保存");
            return false;
        }

        return saveBookChapterData(netDetailList, null, bookId,
            BookChapterDao.Properties.BookId.eq(bookId),
            "保存书籍详情");
    }

    /**
     * 书籍章节数据保存的通用实现
     *
     * @param netDetailList     章节内容数据
     * @param signatureId       签名ID（用于日志，可为 null）
     * @param bookId            书籍ID
     * @param deleteCondition   删除旧数据的查询条件
     * @param operationName     操作名称（用于日志）
     * @return 保存是否成功
     */
    private static boolean saveBookChapterData(List<HH2SectionData> netDetailList,
                                                String signatureId, String bookId,
                                                WhereCondition deleteCondition,
                                                String operationName) {
        return ConvertEntity.executeDatabaseOperation(() -> {
            // 清除旧数据
            ArrayList<BookChapter> existingChapters = ConvertEntity.executeDatabaseOperation(() ->
                LocalServices.getInstance().mBookChapterService.find(deleteCondition),
                "查询旧章节" + (signatureId != null ? signatureId : bookId)
            );

            if (existingChapters != null && !existingChapters.isEmpty()) {
                for (BookChapter existingChapter : existingChapters) {
                    if (existingChapter != null) {
                        LocalServices.getInstance().mBookChapterBodyService
                                .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(existingChapter.getBookChapterId()));
                        LocalServices.getInstance().mBookChapterService.deleteEntity(existingChapter);
                    }
                }
                EasyLog.print(TAG, "已删除 " + existingChapters.size() + " 条旧章节数据");
            }

            // 保存新数据
            int sectionCount = 0;
            int contentCount = 0;

            for (HH2SectionData sectionData : netDetailList) {
                if (sectionData == null) {
                    continue;
                }

                String chapterId = StringHelper.getUuid();

                BookChapter bookChapter = ConvertEntity.createBookChapterByBookId(bookId, sectionData, chapterId);
                if (bookChapter == null) {
                    continue;
                }

                ConvertEntity.executeDatabaseOperation(() -> {
                    LocalServices.getInstance().mBookChapterService.addEntity(bookChapter);
                    return true;
                }, "保存章节" + bookChapter.getSignatureId());

                sectionCount++;

                // 保存章节内容
                if (sectionData.getData() != null) {
                    for (DataItem content : sectionData.getData()) {
                        if (content == null) {
                            continue;
                        }

                        BookChapterBody bookChapterBody = ConvertEntity.createBookChapterBody(chapterId, content);
                        if (bookChapterBody != null) {
                            ConvertEntity.executeDatabaseOperation(() -> {
                                LocalServices.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                                return true;
                            }, "保存章节内容" + content.getID());

                            contentCount++;
                        }
                    }
                }
            }

            EasyLog.print(TAG, "[" + operationName + "] 保存 " + sectionCount + " 个章节, " + contentCount + " 条内容");
            return true;

        }, operationName);
    }

    // ==================== 数据库读取方法（从 ConvertEntity 迁入）====================

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

    /**
     * 获取药材别名列表
     */
    public static List<ZhongYaoAlia> getYaoAlia() {
        return ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mYaoAliasService.findAll(),
            "获取药材别名"
        );
    }

    /**
     * 获取关于信息列表
     */
    public static List<About> getAbout() {
        return ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mAboutService.findAll(),
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

        ArrayList<BookChapter> bookChapterList = ConvertEntity.executeDatabaseOperation(() ->
            LocalServices.getInstance().mBookChapterService.find(
                BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
            "查询章节" + chapter.getSignatureId() + "的内容"
        );

        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "章节 " + chapter.getSignatureId() + " 无内容数据");
            return new ArrayList<>();
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
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

                    DataItem content = ConvertEntity.convertBookChapterBodyToDataItem(bookChapterBody);
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
    public static List<HH2SectionData> getBookChapterDetailList(String bookId) {
        if (bookId == null || bookId.isEmpty()) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }

        ArrayList<BookChapter> bookChapterList = ConvertEntity.executeDatabaseOperation(() ->
            LocalServices.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
            "查询书籍" + bookId + "的章节"
        );

        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "书籍 " + bookId + " 无章节数据");
            return new ArrayList<>();
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
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

                    DataItem content = ConvertEntity.convertBookChapterBodyToDataItem(bookChapterBody);
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
    public static ArrayList<Fang> getFangDetailList(String bookId) {
        if (bookId == null || bookId.isEmpty()) {
            EasyLog.print(TAG, "书籍ID无效: " + bookId);
            return new ArrayList<>();
        }

        ArrayList<YaoFang> fangList = ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId)),
            "查询书籍" + bookId + "的方剂"
        );

        if (fangList == null || fangList.isEmpty()) {
            EasyLog.print(TAG, "书籍 " + bookId + " 无方剂数据");
            return new ArrayList<>();
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
            ArrayList<Fang> detailList = new ArrayList<>();

            for (YaoFang yaoFang : fangList) {
                if (yaoFang == null) {
                    continue;
                }

                Fang fang = ConvertEntity.convertYaoFangToFang(yaoFang);
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
        ArrayList<ZhongYao> yaoList = ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mYaoService.findAll(),
            "获取药材数据"
        );

        if (yaoList == null || yaoList.isEmpty()) {
            EasyLog.print(TAG, "无药材数据");
            return new ArrayList<>();
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
            ArrayList<Yao> detailList = new ArrayList<>();

            for (ZhongYao yao : yaoList) {
                if (yao == null) {
                    continue;
                }

                Yao yao1 = ConvertEntity.convertZhongYaoToYao(yao);
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
        ArrayList<BeiMingCi> beiMingCiList = ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mBeiMingCiService.findAll(),
            "获取名词数据"
        );

        if (beiMingCiList == null || beiMingCiList.isEmpty()) {
            EasyLog.print(TAG, "无名词数据");
            return new ArrayList<>();
        }

        return ConvertEntity.executeDatabaseOperation(() -> {
            ArrayList<MingCiContent> detailList = new ArrayList<>();

            for (BeiMingCi beiMingCi : beiMingCiList) {
                if (beiMingCi == null) {
                    continue;
                }

                MingCiContent content = ConvertEntity.convertBeiMingCiToMingCiContent(beiMingCi);
                if (content != null) {
                    detailList.add(content);
                }
            }

            EasyLog.print(TAG, "读取 " + detailList.size() + "/" + beiMingCiList.size() + " 条名词数据");
            return detailList;

        }, "转换名词数据");
    }

    // ==================== 章节列表网络获取（原 NetworkDataFetcher 逻辑）====================

    /**
     * 从网络获取章节列表并保存到数据库
     *
     * 流程：发起 HTTP 请求 → 处理响应 → 判断是否需要更新 → 批量保存
     *
     * @param lifecycleOwner 生命周期宿主（用于绑定请求生命周期）
     * @param item           书籍导航项，包含 bookNo 等标识
     */
    private static void fetchAndSaveChapterList(LifecycleOwner lifecycleOwner, TabNavBody item) {
        if (lifecycleOwner == null || item == null) {
            EasyLog.print(TAG, "参数无效，跳过章节列表获取");
            return;
        }

        EasyHttp.get(lifecycleOwner)
                .api(new ChapterListApi().setBookId(item.getBookNo()))
                .request(new HttpCallback<HttpData<List<Chapter>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<Chapter>> data) {
                        if (data == null || data.getData() == null || data.getData().isEmpty()) {
                            EasyLog.print(TAG, "书籍 " + item.getBookNo() + " 无章节数据");
                            return;
                        }

                        // 落库交给 DbService 的串行后台线程：hjq 的成功回调跑在主线程，而
                        // processChapterList 会「按书删旧章 + 批量插入」（单本最多 81 章），
                        // 13 本书串行跑在主线程时实测占用约 16s（HWUI 长帧 Vsync 迟到 12.97s），
                        // 是启动卡顿的主因。必须串行：这些落库写同一个 SQLite 库，并发会抢锁。
                        DbService.getInstance().runInBackgroundSerial(
                            () -> processChapterList(data.getData(), item));
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "获取书籍 " + item.getBookNo() + " 章节列表失败: " + e.getMessage());
                        super.onFail(e);
                    }
                });
    }

    /**
     * 处理网络返回的章节列表，判断是否需要更新后批量保存
     */
    private static void processChapterList(List<Chapter> chapters, TabNavBody item) {
        try {
            // 查询已有章节
            ArrayList<Chapter> existingChapters = ConvertEntity.executeDatabaseOperation(
                () -> LocalServices.getInstance().mChapterService.find(
                    ChapterDao.Properties.BookId.eq(item.getBookNo())),
                "查询书籍" + item.getBookNo() + "的章节"
            );

            boolean needsUpdate = shouldUpdateChapters(existingChapters, item.getChapterCount());

            if (!needsUpdate) {
                EasyLog.print(TAG, "书籍 " + item.getBookNo() + " 章节数未变化，跳过更新");
                return;
            }

            // 事务内「按书替换」：删旧章与批量插入要么都提交、要么都回滚，失败时旧数据完整保留
            // （与 saveYaoData 的 replaceAllInTx 同一契约）。原来这里是「先 deleteAll 再逐条插入」，
            // 两步各自独立、且逐条吞异常，一旦中途失败就留下已删/半截的章节表——静默的数据丢失。
            int successCount = saveChaptersBatch(chapters, item.getBookNo());
            EasyLog.print(TAG, "保存书籍 " + item.getBookNo() + " 共 " + successCount + "/" + chapters.size() + " 章");

        } catch (Exception e) {
            EasyLog.print(TAG, "处理书籍 " + item.getBookNo() + " 章节列表失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存章节数据（事务内按书全量替换）。
     *
     * <p>先按 bookId 删除旧章节，再批量插入新章节，整体一个事务：失败时旧数据完整保留。
     * 旧实现是逐条插入 + 逐条吞异常，不具备这个保证——中途失败会留下已删/半截的数据。
     *
     * <p>写入失败会抛出异常，由 {@link #processChapterList} 统一记录日志。
     *
     * @param chapters 章节列表
     * @param bookId   书籍ID
     * @return 实际写入的章节数
     */
    private static int saveChaptersBatch(List<Chapter> chapters, String bookId) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }

        // 先剔除 null：事务内遇到 null 元素会让整批回滚，不如提前过滤掉
        List<Chapter> entities = new ArrayList<>(chapters.size());
        for (Chapter chapter : chapters) {
            if (chapter != null) {
                entities.add(chapter);
            }
        }

        LocalServices.getInstance().mChapterService.replaceWhereInTx(
            entities, ChapterDao.Properties.BookId.eq(bookId));

        return entities.size();
    }
    
    /**
     * 获取导航数据列表
     * 
     * @return 导航数据列表
     */
    public static List<TabNav> getNavigationData() {
        return ConvertEntity.executeDatabaseOperation(
            () -> LocalServices.getInstance().mTabNavService.findAll(),
            "获取导航数据"
        );
    }
}
