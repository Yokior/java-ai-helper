package com.yokior;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;
import java.lang.reflect.*;

/**
 * @author Yokior
 * @description 测试项目文件读取和解压功能
 * @date 2026/1/4 21:44
 */

public class LoadFileTest {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String TEST_PROJECT_DIR = Paths.get(TEMP_DIR, "test_project").toString();
    private static final String TEST_ZIP_PATH = "src/test/resources/test_project.zip";


    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.delete(path);
    }


    /**
     * 使用ZipFile而不是ZipInputStream（推荐）
     */
    @Test
    public void testUnzipProjectWithZipFile() throws IOException {
        System.out.println("开始使用ZipFile解压项目压缩包...");

        // 检查测试文件是否存在
        Path zipPath = Paths.get(TEST_ZIP_PATH);
        if (!Files.exists(zipPath)) {
            System.out.println("警告: 测试压缩包文件不存在: " + TEST_ZIP_PATH);
            System.out.println("请将测试项目压缩包放置在 src/test/resources/test_project.zip");
            return;
        }

        // 尝试多种编码方式
        boolean success = false;
        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};

        for (String encoding : encodings) {
            try {
                System.out.println("尝试使用编码: " + encoding);
                unzipWithZipFile(zipPath, TEST_PROJECT_DIR, encoding);
                success = true;
                System.out.println("使用编码 " + encoding + " 解压成功！");
                break;
            } catch (Exception e) {
                System.out.println("使用编码 " + encoding + " 解压失败: " + e.getMessage());

                // 清理可能的部分解压结果
                Path tempProjectDir = Paths.get(TEST_PROJECT_DIR);
                if (Files.exists(tempProjectDir)) {
                    deleteDirectory(tempProjectDir);
                    Files.createDirectories(tempProjectDir);
                }
            }
        }

        if (!success) {
            System.err.println("所有编码尝试都失败了");
            return;
        }

        // 验证解压结果
        Path projectDir = Paths.get(TEST_PROJECT_DIR);
        System.out.println("解压完成！目标目录: " + projectDir.toAbsolutePath());
    }

    /**
     * 使用ZipFile和指定编码解压
     */
    private void unzipWithZipFile(Path zipPath, String destDir, String encoding) throws IOException {
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
     * 测试读取项目中的所有Java文件
     */
    @Test
    public void testReadJavaFiles() throws IOException {
        System.out.println("开始读取项目中的Java文件...");

        Path projectRoot = Paths.get(TEST_PROJECT_DIR);
        if (!Files.exists(projectRoot)) {
            System.out.println("项目目录不存在，请先运行解压测试");
            return;
        }

        // 收集所有Java文件
        List<Path> javaFiles = Files.walk(projectRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

        if (javaFiles.isEmpty()) {
            System.out.println("未找到任何Java文件");
            return;
        }

        System.out.println("找到 " + javaFiles.size() + " 个Java文件:");
        System.out.println("----------------------------------------");

        for (Path javaFile : javaFiles) {
            // 计算相对于项目根目录的路径
            Path relativePath = projectRoot.relativize(javaFile);
            System.out.println(relativePath.toString());
        }

        System.out.println("----------------------------------------");
        System.out.println("Java文件读取完成");
    }

    /**
     * 诊断测试：检查文件系统状态
     */
    @Test
    public void testDiagnoseFileSystem() throws IOException {
        System.out.println("=== 文件系统诊断测试 ===");

        // 1. 检查临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        System.out.println("系统临时目录: " + tempDir);
        System.out.println("临时目录是否存在: " + Files.exists(Paths.get(tempDir)));
        System.out.println("临时目录是否可读: " + Files.isReadable(Paths.get(tempDir)));
        System.out.println("临时目录是否可写: " + Files.isWritable(Paths.get(tempDir)));

        // 2. 创建测试目录
        Path testDir = Paths.get(tempDir, "test_diagnosis");
        System.out.println("\n创建测试目录: " + testDir.toAbsolutePath());
        Files.createDirectories(testDir);
        System.out.println("测试目录创建后是否存在: " + Files.exists(testDir));

        // 3. 创建测试文件
        Path testFile = testDir.resolve("test.txt");
        Files.write(testFile, "Hello World".getBytes());
        System.out.println("测试文件创建后是否存在: " + Files.exists(testFile));
        System.out.println("测试文件内容: " + Files.readString(testFile));

        // 4. 尝试在用户目录下创建
        String userHome = System.getProperty("user.home");
        Path userTestDir = Paths.get(userHome, "test_project_backup");
        System.out.println("\n用户目录: " + userHome);
        System.out.println("在用户目录下创建: " + userTestDir.toAbsolutePath());
        Files.createDirectories(userTestDir);
        System.out.println("用户测试目录是否存在: " + Files.exists(userTestDir));

        // 5. 列出临时目录内容
        System.out.println("\n临时目录内容:");
        try (Stream<Path> paths = Files.list(Paths.get(tempDir))) {
            paths.limit(10).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        System.out.println("[DIR]  " + path.getFileName());
                    } else {
                        System.out.println("[FILE] " + path.getFileName() + " (" + Files.size(path) + " bytes)");
                    }
                } catch (IOException e) {
                    System.out.println("[ERROR] " + path.getFileName());
                }
            });
        }

        System.out.println("=== 诊断测试完成 ===\n");
    }

    /**
     * 修改版解压测试：使用用户目录
     */
    @Test
    public void testUnzipToUserDirectory() throws IOException {
        System.out.println("=== 解压到用户目录测试 ===");

        String userHome = System.getProperty("user.home");
        String userProjectDir = Paths.get(userHome, "test_project").toString();
        Path zipPath = Paths.get(TEST_ZIP_PATH);

        if (!Files.exists(zipPath)) {
            System.out.println("警告: 测试压缩包文件不存在: " + TEST_ZIP_PATH);
            return;
        }

        // 确保目录存在
        Path projectDir = Paths.get(userProjectDir);
        if (Files.exists(projectDir)) {
            deleteDirectory(projectDir);
        }
        Files.createDirectories(projectDir);
        System.out.println("目标目录: " + projectDir.toAbsolutePath());

        // 使用GBK编码解压
        boolean success = false;
        try {
            unzipWithZipFile(zipPath, userProjectDir, "GBK");
            success = true;
        } catch (Exception e) {
            System.err.println("解压失败: " + e.getMessage());
        }

        if (success) {
            System.out.println("解压成功！");

            // 列出文件
            System.out.println("\n解压后的文件:");
            try (Stream<Path> paths = Files.walk(projectDir)) {
                paths.forEach(path -> {
                    try {
                        Path relative = projectDir.relativize(path);
                        if (Files.isDirectory(path)) {
                            System.out.println("[DIR]  " + relative);
                        } else {
                            System.out.println("[FILE] " + relative + " (" + Files.size(path) + " bytes)");
                        }
                    } catch (IOException e) {
                        System.out.println("[ERROR] " + path.getFileName());
                    }
                });
            }

            // 检查是否真的可以访问
            System.out.println("\n文件访问测试:");
            try (Stream<Path> javaFiles = Files.walk(projectDir).filter(p -> p.toString().endsWith(".java"))) {
                long count = javaFiles.count();
                System.out.println("找到Java文件数量: " + count);
            }
        }

        System.out.println("=== 用户目录测试完成 ===\n");
    }

    /**
     * 仅解压测试（不自动清理）
     * 运行此测试后，文件夹会保留在临时目录中
     */
    @Test
    public void testUnzipOnly() throws IOException {
        System.out.println("=== 仅解压测试（文件夹会保留） ===");

        // 暂时禁用自动清理
        System.out.println("注意：此测试不会自动清理解压的文件夹");

        Path zipPath = Paths.get(TEST_ZIP_PATH);
        if (!Files.exists(zipPath)) {
            System.out.println("警告: 测试压缩包文件不存在: " + TEST_ZIP_PATH);
            return;
        }

        // 确保目录存在
        Path projectDir = Paths.get(TEST_PROJECT_DIR);
        if (Files.exists(projectDir)) {
            deleteDirectory(projectDir);
        }
        Files.createDirectories(projectDir);
        System.out.println("目标目录: " + projectDir.toAbsolutePath());

        // 使用GBK编码解压
        boolean success = false;
        try {
            unzipWithZipFile(zipPath, TEST_PROJECT_DIR, "GBK");
            success = true;
        } catch (Exception e) {
            System.err.println("解压失败: " + e.getMessage());
        }

        if (success) {
            System.out.println("\n✅ 解压成功！文件夹保留在: " + projectDir.toAbsolutePath());

            // 列出文件
            System.out.println("\n解压后的文件:");
            try (Stream<Path> paths = Files.walk(projectDir)) {
                paths.sorted().forEach(path -> {
                    try {
                        Path relative = projectDir.relativize(path);
                        if (Files.isDirectory(path)) {
                            System.out.println("[📁]  " + relative);
                        } else {
                            System.out.println("[📄] " + relative + " (" + Files.size(path) + " bytes)");
                        }
                    } catch (IOException e) {
                        System.out.println("[❌] " + path.getFileName());
                    }
                });
            }

            // 提示用户
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📌 文件夹已创建，您可以现在去查看:");
            System.out.println("   " + projectDir.toAbsolutePath());
            System.out.println("=".repeat(60));

            // 等待用户确认
            System.out.println("\n按回车键继续...");
            try {
                System.in.read();
            } catch (Exception e) {
                // 忽略
            }
        }

        System.out.println("=== 解压测试完成 ===\n");
    }

    /**
     * 完整测试：解压并读取（使用ZipFile方式）
     */
    @Test
    public void testUnzipAndReadProject() throws IOException {
        System.out.println("=== 执行完整测试：解压并读取项目 ===");
        System.out.println("提示: 设置 JVM 参数 -Dpreserve.test.files=true 可以保留解压的文件");

        // 先运行诊断
        testDiagnoseFileSystem();

        // 使用ZipFile方式解压（推荐）
        testUnzipProjectWithZipFile();

        // 再读取
        testReadJavaFiles();

        System.out.println("=== 完整测试完成 ===");
    }
}

