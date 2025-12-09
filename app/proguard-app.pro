# 忽略警告
#-ignorewarning

# 混淆保护自己项目的部分代码以及引用的第三方jar包
#-libraryjars libs/xxxxxxxxx.jar

# 不混淆这个包下的类
-keep class run.yigou.gxzy.http.api.** {
    <fields>;
}
-keep class run.yigou.gxzy.http.response.** {
    <fields>;
}
-keep class run.yigou.gxzy.http.model.** {
    <fields>;
}
-keep class run.yigou.gxzy.http.model.** {
    <fields>;
}

# 保护国密算法相关类，防止混淆导致运行时找不到类
-keep class run.yigou.gxzy.Security.** {
    *;
}
-keep class run.yigou.gxzy.Security.Cryptogram.** {
    *;
}
-keep class run.yigou.gxzy.Security.Cryptogram.Sm.** {
    *;
}

#-keep class run.yigou.gxzy.ui.fragment.noproguard.** {
#    <fields>;
#}
#-keep class run.yigou.gxzy.ui.activity.noproguard.** {
#    <fields>;
#}
# 不混淆被 Log 注解的方法信息
-keepclassmembernames class ** {
    @run.yigou.gxzy.aop.Log <methods>;
}
#
## 保护新增的下载管理器和 MVP 相关类
#-keep class run.yigou.gxzy.ui.tips.tipsutils.ChapterDownloadManager { *; }
#-keep class run.yigou.gxzy.ui.tips.repository.BookRepository { *; }
#-keep class run.yigou.gxzy.ui.tips.contract.** { *; }
#-keep class run.yigou.gxzy.ui.tips.presenter.** { *; }
#
## 保护 Application 类（防止被移除）
#-keep class run.yigou.gxzy.app.AppApplication { *; }