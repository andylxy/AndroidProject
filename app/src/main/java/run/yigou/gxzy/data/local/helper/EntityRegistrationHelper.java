package run.yigou.gxzy.data.local.helper;

import org.greenrobot.greendao.AbstractDao;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体注册助手类
 * 用于管理所有 GreenDao 实体的 Dao 注册
 * 
 * 使用说明：
 * 1. 当添加新的实体类时，只需将其对应的 Dao 类添加到对应分类方法中
 * 2. 系统会在数据库升级时通过 getAllDaos() 自动处理所有实体
 */
public class EntityRegistrationHelper {
    
    /**
     * 获取所有 Dao 类列表
     * 当添加新的实体类时，需要在对应分类方法中添加 Dao 类
     * 
     * @return 所有 Dao 类的数组
     */
    public static Class<? extends AbstractDao<?, ?>>[] getAllDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daoList = new ArrayList<>();
        
        daoList.addAll(getUserManagementDaos());      // 用户管理相关实体
        daoList.addAll(getChatDaos());               // 聊天相关实体
        daoList.addAll(getContentDaos());            // 内容相关实体
        daoList.addAll(getSearchDaos());             // 搜索相关实体
        daoList.addAll(getNavigationDaos());         // 导航相关实体
        daoList.addAll(getMedicineDaos());           // 中药相关实体
        daoList.addAll(getAiDaos());                 // AI 相关实体
        
        @SuppressWarnings("unchecked")
        Class<? extends AbstractDao<?, ?>>[] daos = new Class[daoList.size()];
        return daoList.toArray(daos);
    }
    
    /**
     * 用户管理相关的 Dao 类
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getUserManagementDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.UserInfoDao.class);
        return daos;
    }
    
    /**
     * 聊天相关的 Dao 类
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getChatDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.ChatMessageBeanDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.ChatSessionBeanDao.class);
        return daos;
    }
    
    /**
     * 内容相关的 Dao 类（书籍、章节等）
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getContentDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.AboutDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.BookDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.BookChapterDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.BookChapterBodyDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.ChapterDao.class);
        return daos;
    }
    
    /**
     * 搜索相关的 Dao 类
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getSearchDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.SearchHistoryDao.class);
        return daos;
    }
    
    /**
     * 导航相关的 Dao 类
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getNavigationDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.TabNavDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.TabNavBodyDao.class);
        return daos;
    }
    
    /**
     * 中药相关的 Dao 类（药材、药方等）
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getMedicineDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.BeiMingCiDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.ZhongYaoDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.ZhongYaoAliaDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.YaoFangDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.YaoFangBodyDao.class);
        return daos;
    }
    
    /**
     * AI 相关的 Dao 类
     */
    private static List<Class<? extends AbstractDao<?, ?>>> getAiDaos() {
        List<Class<? extends AbstractDao<?, ?>>> daos = new ArrayList<>();
        daos.add(run.yigou.gxzy.data.local.gen.AiConfigDao.class);
        daos.add(run.yigou.gxzy.data.local.gen.AiConfigBodyDao.class);
        return daos;
    }
}
