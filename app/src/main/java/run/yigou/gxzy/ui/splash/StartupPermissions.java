package run.yigou.gxzy.ui.splash;

import com.hjq.permissions.Permission;

import run.yigou.gxzy.manager.privacy.PrivacyAgreement;

/**
 * 启动硬门要求的**系统权限清单**：协议已同意（{@link PrivacyAgreement#isAgreed()}）
 * <b>且</b>这组权限全部授予，才允许进入 App。
 *
 * <p><b>为什么抽成常量</b>：这张清单原先在四个地方各写一遍字面量（申请处、去设置页、
 * 跳设置授权、判定是否已授予）。硬门的判据散落多处本身就是风险 —— 改一处漏三处，
 * 漏掉的那处会静默变成「少判一个权限」而无人发现。集中到一处后，加权限只改这一行。</p>
 */
final class StartupPermissions {

    /**
     * 需要的存储权限。
     *
     * <p>⚠️ 这三张权限在业务上其实没有被真正使用（全仓只在权限申请工具链里出现，无代码真去
     * 读图片/视频/音频，唯一真实用户是 {@code UpdateDialog} 的装包流程，它只要
     * {@code REQUEST_INSTALL_PACKAGES}）。按当前需求它们仍是硬门条件，索取理由文案写的是
     * 「为了保证您正常使用阅读、下载等核心功能」——该说法与代码实际用途不符，是合规风险点，
     * 改动此处用途时一并复核文案。</p>
     */
    static final String[] REQUIRED = new String[]{
            Permission.READ_MEDIA_IMAGES,
            Permission.READ_MEDIA_VIDEO,
            Permission.READ_MEDIA_AUDIO,
    };

    private StartupPermissions() {
    }
}
