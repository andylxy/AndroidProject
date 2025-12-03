package run.yigou.gxzy.utils;

import static run.yigou.gxzy.app.AppApplication.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存助手类
 * 作者: zhs
 * 时间: 2023-07-07 22:16:14
 * 包名: run.yigou.gxzy.utils
 * 类名: CacheHelper
 * 版本: 1.0
 * 描述: 提供对象的序列化缓存读写功能
 */
public class CacheHelper {

    private static String WRITING_OR_READING_FILE_NAME = "";
    private static final Lock fileLock = new ReentrantLock();

    /**
     * 读取序列化对象
     *
     * @param fileName 文件名
     * @return 序列化对象，如果读取失败或文件不存在则返回null
     */
    public static Serializable readObject(String fileName) {
        if (!isExistDataCache(fileName)) {
            return null;  // 如果缓存不存在，返回 null
        }

        // 等待文件完成当前操作
        waitForFileOperation(fileName);

        // 标记文件正在被读取或写入
        synchronized (WRITING_OR_READING_FILE_NAME) {
            WRITING_OR_READING_FILE_NAME = fileName;
        }

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = application.openFileInput(fileName);  // 打开文件输入流
            ois = new ObjectInputStream(fis);  // 创建对象输入流
            return (Serializable) ois.readObject();  // 返回反序列化的对象
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;  // 文件未找到时返回 null
        } catch (Exception e) {
            e.printStackTrace();
            // 反序列化失败 - 删除缓存文件
            if (e instanceof InvalidClassException) {
                File data = application.getFileStreamPath(fileName);
                if (data.exists()) {
                    data.delete();  // 删除文件
                }
            }
            return null;  // 其他异常返回 null
        } finally {
            closeStreams(ois, fis);
        }
    }

    /**
     * 删除文件，等待直到文件没有被其他操作占用。
     *
     * @param fileName 文件名
     * @return 是否成功删除文件
     */
    public static boolean deleteFile(String fileName) {
        // 使用锁，确保文件操作的同步
        fileLock.lock();
        try {
            // 等待文件完成读写操作
            waitForFileOperation(fileName);

            // 设置文件标记，表示当前文件正在被删除
            WRITING_OR_READING_FILE_NAME = fileName;

            // 执行删除文件操作
            return application.deleteFile(fileName);  // 返回删除是否成功
        } finally {
            // 清理文件操作标记
            WRITING_OR_READING_FILE_NAME = "";
            fileLock.unlock();  // 释放锁
        }
    }

    /**
     * 保存一个序列化对象到文件中。
     *
     * @param serializable 要保存的序列化对象
     * @param fileName     文件名
     * @return 是否成功保存对象
     */
    public static boolean saveObject(Serializable serializable, String fileName) {
        if (serializable == null) {
            return false;
        }

        fileLock.lock();  // 使用锁来控制文件的访问
        try {
            // 如果文件正在被读写，则等待
            waitForFileOperation(fileName);

            // 设置文件标记，表示文件正在被写入
            WRITING_OR_READING_FILE_NAME = fileName;

            // 创建输出流对象
            try (FileOutputStream fos = application.openFileOutput(fileName, application.MODE_PRIVATE);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                // 写入序列化对象并刷新流
                oos.writeObject(serializable);
                oos.flush();
                fos.flush();

                return true;  // 写入成功
            } catch (Exception e) {
                e.printStackTrace();
                return false;  // 捕获异常并返回失败
            }

        } finally {
            // 无论成功还是失败，都清理文件标记
            WRITING_OR_READING_FILE_NAME = "";
            fileLock.unlock();  // 释放锁
        }
    }

    /**
     * 判断缓存文件是否存在
     *
     * @param cacheFile 缓存文件名
     * @return 文件是否存在
     */
    private static boolean isExistDataCache(String cacheFile) {
        if (cacheFile == null) {
            return false;
        }
        File data = application.getFileStreamPath(cacheFile);
        return data != null && data.exists();  // 直接返回结果，添加空值检查
    }

    /**
     * 等待文件完成当前操作
     *
     * @param fileName 文件名
     */
    private static void waitForFileOperation(String fileName) {
        while (WRITING_OR_READING_FILE_NAME.equals(fileName)) {
            try {
                Thread.sleep(100);  // 每100ms检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // 恢复中断状态
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭输入流
     *
     * @param ois ObjectInputStream对象
     * @param fis FileInputStream对象
     */
    private static void closeStreams(ObjectInputStream ois, FileInputStream fis) {
        try {
            if (ois != null) {
                ois.close();  // 关闭输入流
            }
            if (fis != null) {
                fis.close();  // 关闭文件输入流
            }
        } catch (IOException e) {
            e.printStackTrace();  // 捕获并打印异常
        } finally {
            // 清理标记，确保文件操作完成
            WRITING_OR_READING_FILE_NAME = "";
        }
    }
}