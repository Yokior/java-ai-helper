package com.yokior.service.load;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:10
 */
@Service
public class LoadFileServiceImpl implements ILoadFileService {

    private static final String ROOT_PATH = System.getProperty("java.io.tmpdir");

    @Override
    public String loadFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        // 判断是否是压缩包
        if (isZipFile(path)) {
            // 解压到ROOT_PATH下
            String extractedDir = unzipFile(path, ROOT_PATH);
            return extractedDir;
        } else {
            throw new IllegalArgumentException("文件不是压缩包格式: " + filePath);
        } 
    }

    /**
     * 判断文件是否是ZIP格式
     */
    private boolean isZipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read == 4) {
                // ZIP文件的前4个字节是 0x504B0304 (PK\x03\x04)
                return header[0] == 0x50 && header[1] == 0x4B &&
                       header[2] == 0x03 && header[3] == 0x04;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * 解压ZIP文件到指定目录
     */
    private String unzipFile(Path zipPath, String destDir) {
        // 获取压缩文件名（不含扩展名）作为解压目录名
        String fileName = zipPath.getFileName().toString();
        String extractedDirName = fileName.substring(0, fileName.lastIndexOf('.'));
        Path extractedDir = Paths.get(destDir, extractedDirName);

        // 如果目录已存在，先删除
        if (Files.exists(extractedDir)) {
            try {
                deleteDirectory(extractedDir);
            } catch (IOException e) {
                // 删除失败，继续
            }
        }

        // 创建解压目录
        try {
            Files.createDirectories(extractedDir);
        } catch (IOException e) {
            throw new RuntimeException("创建解压目录失败: " + extractedDir, e);
        }

        // 尝试多种编码方式
        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
        Exception lastException = null;

        for (String encoding : encodings) {
            try {
                unzipWithEncoding(zipPath, extractedDir.toString(), encoding);
                return extractedDir.toString(); // 解压成功，返回目录路径
            } catch (Exception e) {
                lastException = e;
                // 清理可能的部分解压结果
                try {
                    cleanUpPartialExtraction(extractedDir.toString());
                } catch (IOException cleanupEx) {
                    // 清理失败，继续尝试下一种编码
                }
            }
        }

        // 所有编码都失败，抛出最后一个异常
        throw new RuntimeException("解压文件失败，尝试了所有编码方式", lastException);
    }

    /**
     * 使用指定编码解压ZIP文件
     */
    private void unzipWithEncoding(Path zipPath, String destDir, String encoding) throws IOException {
        Charset charset = Charset.forName(encoding);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), charset)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            byte[] buffer = new byte[1024];

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = Paths.get(destDir, entry.getName());

                // 防止Zip Slip攻击
                if (!entryPath.normalize().startsWith(Paths.get(destDir).normalize())) {
                    throw new IOException("非法的ZIP条目路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 确保父目录存在
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry);
                         OutputStream fos = Files.newOutputStream(entryPath)) {
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    /**
     * 清理部分解压的结果
     */
    private void cleanUpPartialExtraction(String destDir) throws IOException {
        Path dir = Paths.get(destDir);
        if (Files.exists(dir)) {
            // 这里只清理空目录，避免误删其他文件
            // 实际项目中可能需要更复杂的清理逻辑
            try (var stream = java.nio.file.Files.list(dir)) {
                stream.filter(path -> {
                            try {
                                return Files.isDirectory(path) && isEmptyDirectory(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                     .forEach(path -> {
                         try {
                             deleteDirectory(path);
                         } catch (IOException e) {
                             // 忽略删除失败
                         }
                     });
            }
        }
    }

    /**
     * 判断目录是否为空
     */
    private boolean isEmptyDirectory(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (var stream = java.nio.file.Files.list(path)) {
            return !stream.findAny().isPresent();
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = java.nio.file.Files.list(path)) {
                stream.forEach(entry -> {
                    try {
                        deleteDirectory(entry);
                    } catch (IOException e) {
                        // 忽略删除失败
                    }
                });
            }
        }
        Files.delete(path);
    }
}
