package run.yigou.gxzy.data.local.helper;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.greenrobot.greendao.AbstractDao;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表集合一致性门禁。
 *
 * <p>背景（为什么需要这个测试）：{@code GreenDaoUpgrade.smartMigrate} 只 DROP
 * {@code EntityRegistrationHelper.getAllDaos()} 登记的表，却用
 * {@code DaoMaster.createAllTables(...)} 重建"生成代码声明的全部表"。
 * 两边不一致时，漏登记的表**永不被 DROP、却仍被重建**，于是 {@code onUpgrade} 一触发就抛
 * {@code table ... already exists} → 经 {@code MySQLiteOpenHelper} 重抛 → 启动崩溃。
 * 历史上 {@code ChatSummaryBeanDao} 漏登记就是这样潜伏了很久（升级路径从未被真实走过）。
 *
 * <p>本测试用两条**互相独立**的途径取"表名集合"，再断言它们相等：
 * <ol>
 *   <li>登记侧：反射读 {@code getAllDaos()} 中每个 Dao 的 {@code TABLENAME}；</li>
 *   <li>生成侧：扫 {@code gen/} 下每个 {@code *Dao.java} 源码里声明的 {@code TABLENAME}。</li>
 * </ol>
 * 生成侧刻意读源码而不是写死一份表名清单——写死的话，新增实体后清单本身会过期，
 * 门禁就失去意义了。
 *
 * <p>放在 JVM 单测（而非仪器测试）的原因：{@code TABLENAME} 是 public static final 常量，
 * 反射读取不需要 {@code setAccessible}；{@code AbstractDao} 没有静态字段与 {@code <clinit>}，
 * 类初始化不触碰 Android。若哪天这里报 {@code NoClassDefFoundError}，说明类加载环境变了，
 * 应把本测试挪到 {@code androidTest}。
 */
public class EntityRegistrationHelperTest {

    /** 相对于模块工作目录（Gradle 跑 JVM 单测时通常是 app/）。 */
    private static final String GEN_SOURCE_DIR =
            "src/main/java/run/yigou/gxzy/data/local/gen";

    private static final Pattern TABLENAME_PATTERN =
            Pattern.compile("String\\s+TABLENAME\\s*=\\s*\"([^\"]+)\"");

    @Test
    public void getAllDaosCoversEveryGeneratedDao() throws Exception {
        Set<String> registered = registeredTableNames();
        Set<String> generated = generatedTableNames();

        assertTrue("没有从 gen/ 源码解析到任何表名，本测试已失去保护作用", !generated.isEmpty());

        Set<String> missing = new LinkedHashSet<>(generated);
        missing.removeAll(registered);
        Set<String> extra = new LinkedHashSet<>(registered);
        extra.removeAll(generated);

        assertTrue(
                "以下表在 gen/ 里声明、但没有登记进 getAllDaos()（升级时会因"
                        + "\"table ... already exists\" 导致启动崩溃）：" + missing,
                missing.isEmpty());
        assertTrue(
                "以下表登记进了 getAllDaos()，但 gen/ 里没有对应的 *Dao（登记项已过期）：" + extra,
                extra.isEmpty());
    }

    private static Set<String> registeredTableNames() throws Exception {
        Set<String> names = new LinkedHashSet<>();
        for (Class<? extends AbstractDao<?, ?>> daoClass : EntityRegistrationHelper.getAllDaos()) {
            Field tableName = daoClass.getField("TABLENAME");
            names.add((String) tableName.get(null));
        }
        return names;
    }

    private static Set<String> generatedTableNames() throws Exception {
        File dir = locateGenSourceDir();
        File[] daoFiles = dir.listFiles((d, name) -> name.endsWith("Dao.java"));
        if (daoFiles == null) {
            fail("无法列出 gen/ 源码目录：" + dir.getAbsolutePath());
            return new LinkedHashSet<>();
        }

        Set<String> names = new LinkedHashSet<>();
        for (File daoFile : daoFiles) {
            String source = new String(
                    Files.readAllBytes(daoFile.toPath()), StandardCharsets.UTF_8);
            Matcher matcher = TABLENAME_PATTERN.matcher(source);
            if (!matcher.find()) {
                fail("在 " + daoFile.getName() + " 里找不到 TABLENAME 声明，解析规则需要更新");
            }
            names.add(matcher.group(1));
        }
        return names;
    }

    private static File locateGenSourceDir() {
        File fromModuleDir = new File(GEN_SOURCE_DIR);
        if (fromModuleDir.isDirectory()) {
            return fromModuleDir;
        }
        File fromRepoRoot = new File("app", GEN_SOURCE_DIR);
        if (fromRepoRoot.isDirectory()) {
            return fromRepoRoot;
        }
        fail("找不到 gen/ 源码目录，试过：" + fromModuleDir.getAbsolutePath()
                + " 与 " + fromRepoRoot.getAbsolutePath());
        return fromModuleDir;
    }
}
