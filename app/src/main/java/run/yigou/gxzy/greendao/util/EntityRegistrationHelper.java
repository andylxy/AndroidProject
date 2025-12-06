package run.yigou.gxzy.greendao.util;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体注册助手类
 * 用于自动检测和注册新的GreenDao实体
 * 
 * 使用说明：
 * 1. 当添加新的实体类时，只需将其对应的Dao类添加到getAllDaos方法中
 * 2. 系统会在数据库升级时自动处理新实体的注册
 * 
 * 最佳实践：
 * 1. 每添加一个新实体，就在getAllDaos方法中添加对应的Dao类
 * 2. 如果只是添加新实体而不修改现有实体结构，使用增量升级方式
 * 3. 如果修改了现有实体结构，需要增加数据库版本号并实现特定迁移逻辑
 */
public class EntityRegistrationHelper {
    
    // 版本与实体映射关系
    private static final Map<Integer, List<Class<? extends AbstractDao<?, ?>>>> VERSION_ENTITY_MAP = new HashMap<>();
    
    // 初始化版本与实体的映射关系
    static {
        // 版本1包含的实体
        List<Class<? extends AbstractDao<?, ?>>> v1Entities = new ArrayList<>();
        v1Entities.add(run.yigou.gxzy.greendao.gen.BookDao.class);
        v1Entities.add(run.yigou.gxzy.greendao.gen.ChatMessageBeanDao.class);
        v1Entities.add(run.yigou.gxzy.greendao.gen.ChatSessionBeanDao.class);
        v1Entities.add(run.yigou.gxzy.greendao.gen.SearchHistoryDao.class);
        v1Entities.add(run.yigou.gxzy.greendao.gen.UserInfoDao.class);
        VERSION_ENTITY_MAP.put(1, v1Entities);
        
        // 版本2没有新增实体，只是修改了UserInfo表结构
        List<Class<? extends AbstractDao<?, ?>>> v2Entities = new ArrayList<>();
        VERSION_ENTITY_MAP.put(2, v2Entities);
        
        // 后续添加更多版本...
    }
    
    /**
     * 获取所有Dao类列表
     * 当添加新的实体类时，需要在此处添加对应的Dao类
     * 
     * 注意事项：
     * 1. 此列表应包含所有当前项目中使用的Dao类
     * 2. 顺序无关紧要，但建议按功能或字母顺序排列以便维护
     * 3. 注释标明每个Dao对应的实体类，便于查找
     * 
     * @return 所有Dao类的数组
     */
    public static Class<? extends AbstractDao<?, ?>>[] getAllDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daoList = new ArrayList<>();
        
        // 添加所有Dao类（按字母顺序排列）
        daoList.addAll(getUserManagementDaos());      // 用户管理相关实体
        daoList.addAll(getChatDaos());               // 聊天相关实体
        daoList.addAll(getContentDaos());            // 内容相关实体
        daoList.addAll(getSearchDaos());             // 搜索相关实体
        daoList.addAll(getNavigationDaos());         // 导航相关实体
        daoList.addAll(getMedicineDaos());           // 中药相关实体
        daoList.addAll(getAiDaos());                 // AI相关实体
        
        @SuppressWarnings("unchecked")
        Class<? extends AbstractDao<?, ?>>[] daos = new Class[daoList.size()];
        return daoList.toArray(daos);
    }
    
    /**
     * 获取指定版本新增的Dao类列表
     * 
     * @param version 版本号
     * @return 该版本新增的Dao类列表
     */
    public static List<Class<? extends AbstractDao<?, ?>>> getDaosForVersion(int version) {
        return VERSION_ENTITY_MAP.getOrDefault(version, new ArrayList<>());
    }
    
    /**
     * 获取用户管理相关的Dao类
     * 包含用户信息等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getUserManagementDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 用户信息实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.UserInfoDao.class);
        return daos;
    }
    
    /**
     * 获取聊天相关的Dao类
     * 包含聊天会话、消息等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getChatDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 聊天消息实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.ChatMessageBeanDao.class);
        // 聊天会话实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.ChatSessionBeanDao.class);
        return daos;
    }
    
    /**
     * 获取内容相关的Dao类
     * 包含书籍、章节等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getContentDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 关于我们实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.AboutDao.class);
        // 书籍实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.BookDao.class);
        // 书籍章节实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.BookChapterDao.class);
        // 书籍章节内容实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.BookChapterBodyDao.class);
        // 章节实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.ChapterDao.class);
        return daos;
    }
    
    /**
     * 获取搜索相关的Dao类
     * 包含搜索历史等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getSearchDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 搜索历史实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.SearchHistoryDao.class);
        return daos;
    }
    
    /**
     * 获取导航相关的Dao类
     * 包含底部导航、导航项等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getNavigationDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 底部导航实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.TabNavDao.class);
        // 底部导航项实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.TabNavBodyDao.class);
        return daos;
    }
    
    /**
     * 获取中药相关的Dao类
     * 包含中药材、药方等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getMedicineDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // 别名词典实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.BeiMingCiDao.class);
        // 中药材实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.ZhongYaoDao.class);
        // 中药材别名实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.ZhongYaoAliaDao.class);
        // 药方实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.YaoFangDao.class);
        // 药方明细实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.YaoFangBodyDao.class);
        return daos;
    }
    
    /**
     * 获取AI相关的Dao类
     * 包含AI配置等相关实体
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getAiDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        // AI配置实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.AiConfigDao.class);
        // AI配置详情实体Dao
        daos.add(run.yigou.gxzy.greendao.gen.AiConfigBodyDao.class);
        return daos;
    }
    
    /**
     * 获取新增的Dao类列表（相对于某个旧版本）
     * 用于增量升级时只创建新表
     * 
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     * @return 新增的Dao类数组
     */
    public static Class<? extends AbstractDao<?, ?>>[] getNewDaosSince(int oldVersion, int newVersion) {
        List<Class<? extends AbstractDao<?, ?>>> newDaos = new ArrayList<>();
        
        // 获取指定版本范围内的所有新实体
        for (int version = oldVersion + 1; version <= newVersion; version++) {
            List<Class<? extends AbstractDao<?, ?>>> versionDaos = getDaosForVersion(version);
            newDaos.addAll(versionDaos);
        }
        
        @SuppressWarnings("unchecked")
        Class<? extends AbstractDao<?, ?>>[] daos = new Class[newDaos.size()];
        return newDaos.toArray(daos);
    }
    
    /**
     * 检查Dao类是否已注册
     * 
     * @param daoClass 要检查的Dao类
     * @return 如果已注册返回true，否则返回false
     */
    public static boolean isDaoRegistered(Class<? extends AbstractDao<?, ?>> daoClass) {
        for (List<Class<? extends AbstractDao<?, ?>>> daos : VERSION_ENTITY_MAP.values()) {
            if (daos.contains(daoClass)) {
                return true;
            }
        }
        return false;
    }
}