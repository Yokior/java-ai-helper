package com.yokior.service.split;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.yokior.common.CodeFragmentType;
import com.yokior.common.SplitChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Yokior
 * @description
 * @date 2026/1/7 16:47
 */
@Service
@Slf4j
public class SplitServiceImpl implements ISplitService {



    @Override
    public List<SplitChunk> loadAndSplit(String filePath, String projectName) throws Exception {
        return split(Paths.get(filePath), projectName);
    }

    @Override
    public List<SplitChunk> loadAndSplit(Path filePath, String projectName) throws Exception {
        return split(filePath, projectName);
    }


    private List<SplitChunk> split(Path filePath, String projectName) throws Exception {

//        // 从path中获取文件名去除.java作为类名
//        String className = filePath.getFileName().toString().replace(".java", "");

        // 去除路径前面的临时文件夹和项目名 site
        String tempDir = System.getProperty("java.io.tmpdir");
        String site = filePath.toString().substring(tempDir.length() + projectName.length() + 1);
        log.info(site);

        File file = filePath.toFile();
        if (!file.exists()) {
            throw new Exception("文件不存在");
        }

        try (FileInputStream in = new FileInputStream(file)) {
            CompilationUnit parseResult = StaticJavaParser.parse(in);

            if (parseResult != null) {
                // 创建代码切分器
                CodeSplitter splitter = new CodeSplitter();

                // 切分并收集所有分片
                List<CodeFragment> fragments = splitter.split(parseResult);

                // 输出每个分片的内容
//                log.info(className + " 类共切分成 " + fragments.size() + " 个分片:");

                List<SplitChunk> chunkList = new ArrayList<>();

                for (int i = 0; i < fragments.size(); i++) {
                    CodeFragment fragment = fragments.get(i);

                    SplitChunk chunk = SplitChunk.builder()
                            .projectName(projectName)
                            .type(fragment.getType().getCode())
                            .className(fragment.getClassName())
                            .methodName(fragment.getMethodName())
                            .site(site)
                            .content(splitter.compressCode(fragment.getContent()))
                            .build();

                    chunkList.add(chunk);

//                    System.out.println("\n【分片 " + (i + 1) + "】");
//                    System.out.println("类型: " + fragment.getType());
//                    System.out.println("标识: " + fragment.getDisplayTag());
//                    System.out.println("方法名: " + fragment.getMethodName());
//                    System.out.println("-".repeat(60));
//                    System.out.println(fragment.getContent());
//                    System.out.println(splitter.compressCode(fragment.getContent()));
//                    System.out.println("-".repeat(60));
                }

                return chunkList;
            }
        }

        return new ArrayList<>();
    }


    private static class CodeFragment {
        private CodeFragmentType type; // 分片类型：使用枚举
        private String content;        // 分片内容
        private String className;      // 所属类名
        private String memberName;     // 成员名称（可选）
        private String methodName;     // 方法名（如果不是方法片段可以置空）

        public CodeFragment(CodeFragmentType type, String content, String className) {
            this.type = type;
            this.content = content;
            this.className = className;
        }

        public CodeFragment(CodeFragmentType type, String content, String className, String memberName) {
            this(type, content, className);
            this.memberName = memberName;
        }

        public CodeFragment(CodeFragmentType type, String content, String className, String memberName, String methodName) {
            this(type, content, className, memberName);
            this.methodName = methodName;
        }

        // Getters
        public CodeFragmentType getType() { return type; }
        public String getContent() { return content; }
        public String getClassName() { return className; }
        public String getMemberName() { return memberName; }
        public String getMethodName() { return methodName; }

        /**
         * 获取带方括号的类型标识，用于显示
         */
        public String getDisplayTag() {
            return "[" + type.getCode() + "]";
        }
    }

    /**
     * 代码切分器类
     * 实现基于切分方案的代码切分功能
     */
    private static class CodeSplitter extends VoidVisitorAdapter<List<CodeFragment>> {

        private List<CodeFragment> fragments = new ArrayList<>();
        private String currentClassName = "";
        private Set<String> fieldNames = new HashSet<>();
        private Map<String, String> fieldTypes = new HashMap<>();

        /**
         * 切分CompilationUnit并返回所有分片
         */
        public List<CodeFragment> split(CompilationUnit cu) {
            fragments.clear();

            // 访问所有类型声明
            cu.accept(this, fragments);

            return fragments;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<CodeFragment> fragments) {
            currentClassName = n.getNameAsString();

            // 设置当前类信息，用于智能判断getter/setter
            setCurrentClass(n);

            if (n.isInterface()) {
                // 切分接口
                splitInterface(n);
            } else if (n.isAbstract()) {
                // 切分抽象类
                splitAbstractClass(n);
            } else {
                // 切分普通类
                splitNormalClass(n);
            }

            super.visit(n, fragments);
        }

        @Override
        public void visit(EnumDeclaration n, List<CodeFragment> fragments) {
            currentClassName = n.getNameAsString();

            // 切分枚举类
            splitEnum(n);

            super.visit(n, fragments);
        }

        /**
         * 切分普通类 - 采用方案1：简洁切分策略
         */
        private void splitNormalClass(ClassOrInterfaceDeclaration n) {
            // 分片1：类概览信息 [CLASS_OVERVIEW]
            // 包含：类定义行 + 类注解 + 类注释 + 类属性（含注解） + 所有方法定义行
            String classOverviewContent = buildClassOverviewContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.CLASS_OVERVIEW, classOverviewContent, currentClassName, null, null));

            // 分片2-N：方法详情 [METHOD_DETAIL]
            // 只有语义重要的方法才单独切分
            List<MethodDeclaration> importantMethods = n.getMethods().stream()
                    .filter(this::isMethodSemanticallyImportant)
                    .toList();
            for (MethodDeclaration method : importantMethods) {
                // 处理方法体切分
                addMethodFragments(method);
            }
        }

        /**
         * 计算包含方法头的完整内容的压缩长度
         */
        private int calculateCompressedLength(String methodHeader, String bodyPrefix, String bodyContent) {
            String fullContent = methodHeader + bodyPrefix + "  " + bodyContent.replace("\n", "\n  ") + "\n";
            return compressCode(fullContent).length();
        }

        /**
         * 添加方法分片，处理方法体切分
         */
        private void addMethodFragments(MethodDeclaration method) {
            StringBuilder methodHeader = new StringBuilder();
            methodHeader.append("方法: ").append(method.getDeclarationAsString()).append("\n");

            // 方法注释
            method.getJavadoc().ifPresent(javadoc -> {
                methodHeader.append("注释:\n");
                methodHeader.append("  ").append(javadoc.getDescription().toText()).append("\n");
            });

            // 方法体
            if (!method.isAbstract() && method.getBody().isPresent()) {
                String methodBody = method.getBody().get().toString();

                // 使用压缩长度进行判断
                int compressedTotalLength = calculateCompressedLength(methodHeader.toString(), "方法体:\n", methodBody);

                // 添加调试日志
                log.info("方法 {} 原始长度: {}, 压缩后长度: {}",
                        method.getName(),
                        methodHeader.length() + "方法体:\n".length() + methodBody.replace("\n", "\n  ").length(),
                        compressedTotalLength);

                if (compressedTotalLength <= 1000) {
                    // 不需要切分，直接添加
                    StringBuilder content = new StringBuilder(methodHeader);
                    content.append("方法体:\n");
                    content.append("  ").append(methodBody.replace("\n", "\n  ")).append("\n");
                    fragments.add(new CodeFragment(CodeFragmentType.METHOD_DETAIL, content.toString(), currentClassName, method.getNameAsString(), method.getNameAsString()));
                } else {
                    // 需要切分，计算每个分片的最大压缩长度
                    // 预留方法头和"方法体(分段X):\n"的压缩长度
                    String sampleSegmentHeader = methodHeader.toString() + "方法体(分段1):\n";
                    int segmentHeaderCompressedLength = compressCode(sampleSegmentHeader).length();
                    int maxCompressedBodyLength = 1000 - segmentHeaderCompressedLength - 20; // 留一些缓冲

                    log.info("方法 {} 需要切分，每个分片最大压缩长度: {}", method.getName(), maxCompressedBodyLength);

                    // 需要切分
                    List<String> semanticChunks = splitMethodBodySemantically(methodBody, maxCompressedBodyLength);

                    for (int i = 0; i < semanticChunks.size(); i++) {
                        StringBuilder content = new StringBuilder(methodHeader);
                        content.append("方法体(分段").append(i + 1).append("):\n");
                        content.append("  ").append(semanticChunks.get(i).replace("\n", "\n  ")).append("\n");

                        // 验证分片压缩长度
                        String finalContent = content.toString();
                        int finalCompressedLength = compressCode(finalContent).length();

                        if (finalCompressedLength > 1000) {
                            log.warn("分片 {} 压缩后长度仍超过1000字符: {}", method.getNameAsString() + "_segment" + (i + 1), finalCompressedLength);
                        } else {
                            log.info("分片 {} 压缩后长度: {}", method.getNameAsString() + "_segment" + (i + 1), finalCompressedLength);
                        }

                        fragments.add(new CodeFragment(CodeFragmentType.METHOD_DETAIL, finalContent, currentClassName, method.getNameAsString() + "_segment" + (i + 1), method.getNameAsString()));
                    }
                }
            } else if (method.isAbstract()) {
                StringBuilder content = new StringBuilder(methodHeader);
                content.append("[抽象方法 - 无实现]\n");
                fragments.add(new CodeFragment(CodeFragmentType.METHOD_DETAIL, content.toString(), currentClassName, method.getNameAsString(), method.getNameAsString()));
            }
        }

        /**
         * 切分接口
         */
        private void splitInterface(ClassOrInterfaceDeclaration n) {
            // 分片1：完整的接口定义（包含契约和方法签名）
            String interfaceDefinitionContent = buildInterfaceDefinitionContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.INTERFACE_DEFINITION, interfaceDefinitionContent, currentClassName, null, null));

            // 分片2-N：默认方法实现
            List<MethodDeclaration> defaultMethods = n.getMethods().stream()
                    .filter(m -> m.isDefault())
                    .toList();
            for (MethodDeclaration method : defaultMethods) {
                String methodContent = buildMethodDetailContent(method);
                fragments.add(new CodeFragment(CodeFragmentType.DEFAULT_METHOD, methodContent, currentClassName, method.getNameAsString(), method.getNameAsString()));
            }

            // 分片3-N：静态方法
            List<MethodDeclaration> staticMethods = n.getMethods().stream()
                    .filter(m -> m.isStatic())
                    .toList();
            for (MethodDeclaration method : staticMethods) {
                String methodContent = buildMethodDetailContent(method);
                fragments.add(new CodeFragment(CodeFragmentType.STATIC_METHOD, methodContent, currentClassName, method.getNameAsString(), method.getNameAsString()));
            }
        }

        /**
         * 切分抽象类
         */
        private void splitAbstractClass(ClassOrInterfaceDeclaration n) {
            // 分片1：抽象类定义
            String abstractClassContent = buildAbstractClassContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.ABSTRACT_CLASS_META, abstractClassContent, currentClassName, null, null));

            // 分片2：具体实现部分
            String concreteMembersContent = buildConcreteMembersContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.CONCRETE_MEMBERS, concreteMembersContent, currentClassName, null, null));

            // 分片3-N：抽象方法详情
            List<MethodDeclaration> abstractMethods = n.getMethods().stream()
                    .filter(MethodDeclaration::isAbstract)
                    .toList();
            for (MethodDeclaration method : abstractMethods) {
                String methodContent = buildAbstractMethodContent(method);
                fragments.add(new CodeFragment(CodeFragmentType.ABSTRACT_METHOD_DETAIL, methodContent, currentClassName, method.getNameAsString(), method.getNameAsString()));
            }
        }

        /**
         * 切分枚举类
         */
        private void splitEnum(EnumDeclaration n) {
            // 分片1：枚举定义
            String enumDefinitionContent = buildEnumDefinitionContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.ENUM_DEFINITION, enumDefinitionContent, currentClassName, null, null));

            // 分片2：属性和方法
            String enumMembersContent = buildEnumMembersContent(n);
            fragments.add(new CodeFragment(CodeFragmentType.ENUM_MEMBERS, enumMembersContent, currentClassName, null, null));

            // 分片3-N：枚举常量详情
            for (EnumConstantDeclaration constant : n.getEntries()) {
                String constantContent = buildEnumConstantContent(constant);
                fragments.add(new CodeFragment(CodeFragmentType.ENUM_CONSTANT_DETAIL, constantContent, currentClassName, constant.getNameAsString(), null));
            }
        }

        // 以下是构建各种分片内容的方法

        /**
         * 构建类概览信息 - 方案1
         * 包含：类定义行 + 类注解 + 类注释 + 类属性（含注解） + 所有方法定义行
         */
        private String buildClassOverviewContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            // 类定义行
            content.append("类定义: ");
            if (!n.getModifiers().isEmpty()) {
                content.append(n.getModifiers()).append(" ");
            }
            content.append("class ").append(n.getName());

            // 类型参数
            if (!n.getTypeParameters().isEmpty()) {
                content.append(n.getTypeParameters());
            }

            // 扩展类
            if (!n.getExtendedTypes().isEmpty()) {
                content.append(" extends ");
                content.append(n.getExtendedTypes().get(0));
            }

            // 实现接口
            if (!n.getImplementedTypes().isEmpty()) {
                content.append(" implements ");
                content.append(String.join(", ", n.getImplementedTypes().stream()
                        .map(Object::toString).toArray(String[]::new)));
            }
            content.append(" {\n");

            // 类注解
            if (!n.getAnnotations().isEmpty()) {
                content.append("\n类注解:\n");
                n.getAnnotations().forEach(annotation ->
                        content.append("  ").append(annotation).append("\n"));
            }

            // 类注释
            n.getJavadoc().ifPresent(javadoc -> {
                content.append("\n类注释:\n");
                content.append("  /**\n");
                Arrays.stream(javadoc.getDescription().toText().split("\n"))
                        .forEach(line -> content.append("   * ").append(line).append("\n"));
                content.append("   */\n");
            });

            // 类属性（含注解）
            List<FieldDeclaration> fields = n.getFields();
            if (!fields.isEmpty()) {
                content.append("\n类属性:\n");
                fields.forEach(field -> {
                    // 字段注解
                    if (!field.getAnnotations().isEmpty()) {
                        field.getAnnotations().forEach(annotation ->
                                content.append("  ").append(annotation).append("\n"));
                    }
                    // 字段定义
                    content.append("  ").append(field).append("\n");
                });
            }

            // 语义重要的方法定义行（不含方法体）
            List<MethodDeclaration> importantMethods = n.getMethods().stream()
                    .filter(this::isMethodSemanticallyImportant)
                    .toList();
            if (!importantMethods.isEmpty()) {
                content.append("\n重要方法定义:\n");
                importantMethods.forEach(method -> {
                    content.append("  ").append(method.getDeclarationAsString()).append(";\n");
                });
            }


            // 构造函数通常不需要单独显示在概览中，因为它们是标准的初始化逻辑
            // 只在有特殊注解或重要实现时才显示
            List<ConstructorDeclaration> importantConstructors = n.getConstructors().stream()
                    .filter(c -> !c.getAnnotations().isEmpty() || hasComplexLogicInConstructor(c))
                    .toList();
            if (!importantConstructors.isEmpty()) {
                content.append("\n重要构造函数定义:\n");
                importantConstructors.forEach(constructor -> {
                    content.append("  ").append(constructor.getDeclarationAsString()).append(" {\n");
                    content.append("    // 构造函数实现\n");
                    content.append("  }\n");
                });
            }

            content.append("}");

            return content.toString();
        }

        private String buildClassMetaContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            // 类定义
            content.append("类型: ").append(n.isInterface() ? "接口" : n.isAbstract() ? "抽象类" : "普通类").append("\n");
            content.append("名称: ").append(n.getName()).append("\n");

            // 修饰符
            if (!n.getModifiers().isEmpty()) {
                content.append("修饰符: ").append(n.getModifiers()).append("\n");
            }

            // 类型参数
            if (!n.getTypeParameters().isEmpty()) {
                content.append("类型参数: ").append(n.getTypeParameters()).append("\n");
            }

            // 扩展类
            n.getExtendedTypes().forEach(extType ->
                    content.append("扩展: ").append(extType).append("\n"));

            // 实现接口
            if (!n.getImplementedTypes().isEmpty()) {
                content.append("实现接口:\n");
                n.getImplementedTypes().forEach(implType ->
                        content.append("  ").append(implType).append("\n"));
            }

            // 注解
            if (!n.getAnnotations().isEmpty()) {
                content.append("注解:\n");
                n.getAnnotations().forEach(annotation ->
                        content.append("  @").append(annotation.getName()).append("\n"));
            }

            // JavaDoc
            n.getJavadoc().ifPresent(javadoc -> {
                content.append("\n注释:\n");
                content.append(javadoc.getDescription().toText()).append("\n");
            });

            return content.toString();
        }

        private String buildClassStructureContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            // 重要字段列表
            List<FieldDeclaration> fields = n.getFields().stream()
                    .filter(this::isFieldSemanticallyImportant)
                    .toList();
            if (!fields.isEmpty()) {
                content.append("\n重要字段列表:\n");
                fields.forEach(field -> {
                    field.getVariables().forEach(variable -> {
                        content.append("  ").append(field.getModifiers())
                                .append(" ").append(variable.getType())
                                .append(" ").append(variable.getName()).append("\n");
                        // 字段注解
                        field.getAnnotations().forEach(annotation ->
                                content.append("    @").append(annotation.getName()).append("\n"));
                    });
                });
            }

            // 构造函数列表
            List<ConstructorDeclaration> constructors = n.getConstructors();
            if (!constructors.isEmpty()) {
                content.append("\n构造函数列表:\n");
                constructors.forEach(constructor -> {
                    content.append("  ").append(constructor.getName())
                            .append(constructor.getParameters()).append("\n");
                    constructor.getAnnotations().forEach(annotation ->
                            content.append("    @").append(annotation.getName()).append("\n"));
                });
            }

            // 重要方法签名列表
            List<MethodDeclaration> methods = n.getMethods().stream()
                    .filter(this::isMethodSemanticallyImportant)
                    .toList();
            if (!methods.isEmpty()) {
                content.append("\n重要方法签名列表:\n");
                methods.forEach(method -> {
                    content.append("  ").append(method.getModifiers())
                            .append(" ").append(method.getType())
                            .append(" ").append(method.getName())
                            .append(method.getParameters()).append("\n");
                });
            }

            return content.toString();
        }

        private String buildMethodDetailContent(MethodDeclaration method) {
            StringBuilder content = new StringBuilder();

            content.append("方法: ").append(method.getDeclarationAsString()).append("\n");

            // 方法注释
            method.getJavadoc().ifPresent(javadoc -> {
                content.append("注释:\n");
                content.append("  ").append(javadoc.getDescription().toText()).append("\n");
            });

            // 方法体
            if (!method.isAbstract() && method.getBody().isPresent()) {
                String methodBody = method.getBody().get().toString();
                content.append("方法体:\n");
                content.append("  ").append(methodBody.replace("\n", "\n  ")).append("\n");
            } else if (method.isAbstract()) {
                content.append("[抽象方法 - 无实现]\n");
            }

            return content.toString();
        }

        private String buildConstructorDetailContent(ConstructorDeclaration constructor) {
            StringBuilder content = new StringBuilder();

            content.append("构造函数详情:\n");
            content.append(constructor.toString()).append("\n");

            return content.toString();
        }

        /**
         * 语义切分方法体内容
         * 识别代码逻辑块，如if语句、for循环、try-catch块等
         */
        private List<String> splitMethodBodySemantically(String methodBody, int maxCompressedLength) {
            List<String> chunks = new ArrayList<>();

            // 移除外层大括号
            String body = methodBody.trim();
            if (body.startsWith("{") && body.endsWith("}")) {
                body = body.substring(1, body.length() - 1).trim();
            }

            // 按照语义块进行切分
            List<String> semanticBlocks = identifySemanticBlocks(body);

            // 组合语义块，确保每个分片压缩后不超过maxCompressedLength字符
            StringBuilder currentChunk = new StringBuilder();

            for (String block : semanticBlocks) {
                // 计算当前块加上新块的压缩长度
                String testContent = currentChunk.toString() + block + "\n";
                int testCompressedLength = compressCode(testContent).length();

                // 如果单个语义块本身就超过限制，需要进一步切分
                int blockCompressedLength = compressCode(block).length();
                if (blockCompressedLength > maxCompressedLength) {
                    // 先保存当前分片（如果不为空）
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder();
                    }
                    // 对超长块进行强制切分
                    List<String> subBlocks = forceSplitBlock(block, maxCompressedLength);
                    chunks.addAll(subBlocks);
                } else {
                    // 正常情况：检查加上新块后是否超过限制
                    if (testCompressedLength > maxCompressedLength && currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder();
                    }
                    currentChunk.append(block).append("\n");
                }
            }

            // 添加最后一个分片
            if (currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
            }

            return chunks;
        }

        /**
         * 强制切分超长块
         */
        private List<String> forceSplitBlock(String block, int maxCompressedLength) {
            List<String> subBlocks = new ArrayList<>();
            String[] lines = block.split("\n");
            StringBuilder currentSubBlock = new StringBuilder();

            for (String line : lines) {
                // 测试添加当前行后的压缩长度
                String testContent = currentSubBlock.toString() + line + "\n";
                int testCompressedLength = compressCode(testContent).length();

                if (testCompressedLength > maxCompressedLength && currentSubBlock.length() > 0) {
                    subBlocks.add(currentSubBlock.toString());
                    currentSubBlock = new StringBuilder();
                }
                currentSubBlock.append(line).append("\n");
            }

            if (currentSubBlock.length() > 0) {
                subBlocks.add(currentSubBlock.toString());
            }

            return subBlocks;
        }

        /**
         * 识别方法体中的语义块
         */
        private List<String> identifySemanticBlocks(String body) {
            List<String> blocks = new ArrayList<>();
            String[] lines = body.split("\n");
            StringBuilder currentBlock = new StringBuilder();
            int braceLevel = 0;
            boolean inBlock = false;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String trimmedLine = line.trim();

                // 检查是否是语义块的开始
                if (isStartOfSemanticBlock(trimmedLine)) {
                    // 如果当前块不为空，先保存
                    if (currentBlock.length() > 0) {
                        blocks.add(currentBlock.toString());
                        currentBlock = new StringBuilder();
                    }
                    inBlock = true;
                }

                currentBlock.append(line).append("\n");

                // 更新大括号层级
                braceLevel += countBraces(line);

                // 如果是语义块且大括号层级回到0，表示块结束
                if (inBlock && braceLevel == 0) {
                    blocks.add(currentBlock.toString());
                    currentBlock = new StringBuilder();
                    inBlock = false;
                }
            }

            // 添加剩余的内容
            if (currentBlock.length() > 0) {
                blocks.add(currentBlock.toString());
            }

            // 如果没有识别出语义块，则按行数进行简单切分
            if (blocks.size() == 1) {
                return splitByLines(body);
            }

            return blocks;
        }

        /**
         * 按行数进行简单切分
         */
        private List<String> splitByLines(String body) {
            List<String> blocks = new ArrayList<>();
            String[] lines = body.split("\n");
            StringBuilder currentBlock = new StringBuilder();
            int lineCount = 0;

            for (String line : lines) {
                currentBlock.append(line).append("\n");
                lineCount++;

                // 每20行作为一个分片
                if (lineCount >= 20) {
                    blocks.add(currentBlock.toString());
                    currentBlock = new StringBuilder();
                    lineCount = 0;
                }
            }

            // 添加剩余的内容
            if (currentBlock.length() > 0) {
                blocks.add(currentBlock.toString());
            }

            return blocks;
        }

        /**
         * 判断是否是语义块的开始
         */
        private boolean isStartOfSemanticBlock(String line) {
            return line.startsWith("if") || line.startsWith("else if") ||
                    line.startsWith("else") || line.startsWith("for") ||
                    line.startsWith("while") || line.startsWith("do") ||
                    line.startsWith("try") || line.startsWith("catch") ||
                    line.startsWith("finally") || line.startsWith("switch") ||
                    line.startsWith("case") || line.startsWith("default") ||
                    line.startsWith("synchronized");
        }

        /**
         * 计算一行中的大括号数量（开括号+1，闭括号-1）
         */
        private int countBraces(String line) {
            int count = 0;
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    count++;
                } else if (c == '}') {
                    count--;
                }
            }
            return count;
        }

        /**
         * 构建完整的接口定义内容（包含契约和方法签名）
         */
        private String buildInterfaceDefinitionContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            // 接口定义
            content.append("接口定义: ").append(n.getName()).append("\n");

            // 注解
            if (!n.getAnnotations().isEmpty()) {
                content.append("注解:\n");
                n.getAnnotations().forEach(annotation ->
                        content.append("  ").append(annotation).append("\n"));
            }

            // JavaDoc
            n.getJavadoc().ifPresent(javadoc -> {
                content.append("\n注释:\n");
                content.append(javadoc.getDescription().toText()).append("\n");
            });

            // 常量定义
            List<FieldDeclaration> constants = n.getFields().stream()
                    .filter(f -> f.isStatic() && f.isFinal())
                    .toList();
            if (!constants.isEmpty()) {
                content.append("\n常量定义:\n");
                constants.forEach(field -> {
                    field.getVariables().forEach(variable -> {
                        content.append("  ").append(field.getModifiers())
                                .append(" ").append(variable.getType())
                                .append(" ").append(variable.getName()).append("\n");
                    });
                });
            }

            // 抽象方法定义
            List<MethodDeclaration> methods = n.getMethods().stream()
                    .filter(m -> !m.isDefault() && !m.isStatic())
                    .toList();

            if (!methods.isEmpty()) {
                content.append("\n抽象方法定义:\n");
                methods.forEach(method -> {
                    content.append("  ").append(method.getDeclarationAsString()).append("\n");

                    // 添加方法的JavaDoc以增强语义
                    method.getJavadoc().ifPresent(javadoc -> {
                        String docText = javadoc.getDescription().toText();
                        if (!docText.trim().isEmpty()) {
                            content.append("    // ").append(docText.trim()).append("\n");
                        }
                    });
                });

            } else {
                content.append("\n// 该接口没有抽象方法（可能是函数式接口或标记接口）\n");
            }

            // 统计信息
            int defaultMethodCount = (int) n.getMethods().stream().filter(m -> m.isDefault()).count();
            int staticMethodCount = (int) n.getMethods().stream().filter(m -> m.isStatic()).count();

            return content.toString();
        }

        private String buildAbstractClassContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            content.append("抽象类定义: ").append(n.getName()).append("\n");

            // 注解和修饰符
            if (!n.getModifiers().isEmpty()) {
                content.append("修饰符: ").append(n.getModifiers()).append("\n");
            }

            if (!n.getAnnotations().isEmpty()) {
                content.append("注解:\n");
                n.getAnnotations().forEach(annotation ->
                        content.append("  @").append(annotation.getName()).append("\n"));
            }

            // 抽象方法声明列表
            List<MethodDeclaration> abstractMethods = n.getMethods().stream()
                    .filter(MethodDeclaration::isAbstract)
                    .toList();
            if (!abstractMethods.isEmpty()) {
                content.append("\n抽象方法声明列表:\n");
                abstractMethods.forEach(method ->
                        content.append("  ").append(method.getDeclarationAsString()).append("\n"));
            }

            return content.toString();
        }

        private String buildConcreteMembersContent(ClassOrInterfaceDeclaration n) {
            StringBuilder content = new StringBuilder();

            // 具体属性
            List<FieldDeclaration> concreteFields = n.getFields().stream()
                    .filter(this::isFieldSemanticallyImportant)
                    .toList();
            if (!concreteFields.isEmpty()) {
                content.append("具体属性实现:\n");
                concreteFields.forEach(field ->
                        content.append("  ").append(field.toString()).append("\n"));
            }

            // 具体方法
            List<MethodDeclaration> concreteMethods = n.getMethods().stream()
                    .filter(m -> !m.isAbstract() && isMethodSemanticallyImportant(m))
                    .toList();
            if (!concreteMethods.isEmpty()) {
                content.append("\n具体方法实现:\n");
                concreteMethods.forEach(method ->
                        content.append("  ").append(method.getName()).append("()\n"));
            }

            return content.toString();
        }

        private String buildAbstractMethodContent(MethodDeclaration method) {
            StringBuilder content = new StringBuilder();

            content.append("抽象方法详情: ").append(method.getName()).append("\n");
            content.append("签名: ").append(method.getDeclarationAsString()).append("\n");

            // 方法注释
            method.getJavadoc().ifPresent(javadoc -> {
                content.append("注释:\n");
                content.append("  ").append(javadoc.getDescription().toText()).append("\n");
            });

            return content.toString();
        }

        private String buildEnumDefinitionContent(EnumDeclaration n) {
            StringBuilder content = new StringBuilder();

            content.append("枚举名称: ").append(n.getName()).append("\n");

            // 实现接口
            if (!n.getImplementedTypes().isEmpty()) {
                content.append("实现接口:\n");
                n.getImplementedTypes().forEach(type ->
                        content.append("  ").append(type).append("\n"));
            }

            // 注解
            if (!n.getAnnotations().isEmpty()) {
                content.append("注解:\n");
                n.getAnnotations().forEach(annotation ->
                        content.append("  @").append(annotation.getName()).append("\n"));
            }

            // JavaDoc
            n.getJavadoc().ifPresent(javadoc -> {
                content.append("\n注释:\n");
                content.append(javadoc.getDescription().toText()).append("\n");
            });

            // 枚举常量列表
            content.append("\n枚举常量列表:\n");
            n.getEntries().forEach(entry ->
                    content.append("  ").append(entry.getName()).append("\n"));

            return content.toString();
        }

        private String buildEnumMembersContent(EnumDeclaration n) {
            StringBuilder content = new StringBuilder();

            List<BodyDeclaration<?>> members = n.getMembers();
            if (!members.isEmpty()) {
                content.append("其他成员:\n");
                members.forEach(member -> {
                    if (member instanceof FieldDeclaration) {
                        FieldDeclaration field = (FieldDeclaration) member;
                        content.append("  字段: ").append(field.toString()).append("\n");
                    } else if (member instanceof MethodDeclaration) {
                        MethodDeclaration method = (MethodDeclaration) member;
                        content.append("  方法: ").append(method.getDeclarationAsString()).append("\n");
                    }
                });
            }

            return content.toString();
        }

        private String buildEnumConstantContent(EnumConstantDeclaration constant) {
            StringBuilder content = new StringBuilder();

            content.append("常量详情: ").append(constant.getName()).append("\n");

            // 参数
            if (!constant.getArguments().isEmpty()) {
                content.append("参数: ").append(constant.getArguments()).append("\n");
            }

            // 注释
            constant.getJavadoc().ifPresent(javadoc -> {
                content.append("注释: ").append(javadoc.getDescription().toText()).append("\n");
            });

            return content.toString();
        }

        /**
         * 检查构造函数是否有复杂逻辑
         */
        private boolean hasComplexLogicInConstructor(ConstructorDeclaration constructor) {
            // 构造函数一定有body，不需要检查isPresent
            String body = constructor.getBody().toString();
            // 如果构造函数包含复杂逻辑，则认为重要
            return body.contains("if") || body.contains("for") ||
                    body.contains("while") || body.contains("throw") ||
                    body.contains("try") || body.contains("catch") ||
                    body.length() > 100;
        }

        /**
         * 构建成员详情内容（包含类上下文）
         * 这样每个分片都有完整的上下文信息，便于向量化后的检索
         */
        private String buildMemberDetailContent(ClassOrInterfaceDeclaration clazz, FieldDeclaration field, ConstructorDeclaration constructor, MethodDeclaration method) {
            StringBuilder content = new StringBuilder();

            // 首先添加类的基本上下文信息
            content.append("所属类: ").append(clazz.getName()).append("\n");
            if (!clazz.getModifiers().isEmpty()) {
                content.append("类修饰符: ").append(clazz.getModifiers()).append("\n");
            }

            // 添加相关的类属性信息（对于方法/构造函数，显示其访问的字段）
            if (method != null || constructor != null) {
                List<FieldDeclaration> relatedFields = clazz.getFields().stream()
                        .filter(this::isFieldSemanticallyImportant)
                        .limit(5) // 限制显示数量，避免分片过大
                        .toList();
                if (!relatedFields.isEmpty()) {
                    content.append("相关类属性:\n");
                    for (FieldDeclaration relatedField : relatedFields) {
                        relatedField.getVariables().forEach(variable -> {
                            content.append("  ").append(variable.getType())
                                    .append(" ").append(variable.getName()).append("\n");
                        });
                    }
                }
            }

            // 然后添加具体的成员详情
            if (field != null) {
                content.append("\n字段详情:\n");
                content.append(field.toString()).append("\n");

                // 字段注释
                field.getJavadoc().ifPresent(javadoc -> {
                    content.append("字段注释: ").append(javadoc.getDescription().toText()).append("\n");
                });
            } else if (constructor != null) {
                content.append("\n构造函数详情:\n");
                content.append(constructor.toString()).append("\n");

                // 构造函数注释
                constructor.getJavadoc().ifPresent(javadoc -> {
                    content.append("构造函数注释: ").append(javadoc.getDescription().toText()).append("\n");
                });
            } else if (method != null) {
                content.append("方法: ").append(method.getDeclarationAsString()).append("\n");

                // 方法注释
                method.getJavadoc().ifPresent(javadoc -> {
                    content.append("方法注释: ").append(javadoc.getDescription().toText()).append("\n");
                });

                // 方法体
                if (!method.isAbstract() && method.getBody().isPresent()) {
                    content.append("方法体:\n");
                    content.append(method.getBody().get().toString()).append("\n");
                } else if (method.isAbstract()) {
                    content.append("[抽象方法 - 无实现]\n");
                }
            }

            return content.toString();
        }

        /**
         * 构建成员详情内容的重载方法（兼容旧调用）
         */
        private String buildMemberDetailContent(ClassOrInterfaceDeclaration clazz, FieldDeclaration field, ConstructorDeclaration constructor) {
            return buildMemberDetailContent(clazz, field, constructor, null);
        }

        /**
         * 构建成员详情内容的重载方法（兼容旧调用）
         */
        private String buildMemberDetailContent(ClassOrInterfaceDeclaration clazz, ConstructorDeclaration constructor, MethodDeclaration method) {
            return buildMemberDetailContent(clazz, null, constructor, method);
        }

        /**
         * 压缩代码，去除多余空行换行等无效字符
         * @param code 原始代码
         * @return 压缩后的代码
         */
        private String compressCode(String code) {
            if (code == null || code.trim().isEmpty()) {
                return code;
            }

            // 去除行首行尾空白
            String[] lines = code.split("\n");
            StringBuilder compressed = new StringBuilder();
            boolean isEmptyLine = true;

            for (String line : lines) {
                String trimmedLine = line.trim();

                // 非空行，去除多余空格
                if (compressed.length() > 0 && !compressed.toString().endsWith("\n")) {
                    compressed.append(" ");
                }

                // 压缩行内的多个空格为单个空格
                trimmedLine = trimmedLine.replaceAll("\\s+", " ");
                compressed.append(trimmedLine);
                isEmptyLine = false;
            }

            return compressed.toString();
        }

        // 以下是辅助方法，与ParserTest中的类似

        private void setCurrentClass(ClassOrInterfaceDeclaration clazz) {
            this.fieldNames.clear();
            this.fieldTypes.clear();

            clazz.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    String fieldName = variable.getNameAsString();
                    String fieldType = variable.getTypeAsString();
                    fieldNames.add(fieldName);
                    fieldTypes.put(fieldName, fieldType);
                });
            });
        }

        private boolean isMethodSemanticallyImportant(MethodDeclaration method) {
            String methodName = String.valueOf(method.getName());
            int paramCount = method.getParameters().size();

            // 智能判断getter方法
            if (methodName.matches("^(get|is)[A-Z].*") && paramCount == 0) {
                String propertyName = extractPropertyNameFromGetter(methodName);
                if (fieldNames.contains(propertyName)) {
                    if (method.getJavadoc().isPresent() &&
                            method.getJavadoc().get().getDescription().toText().length() > 20) {
                        return true;
                    }
                    if (hasComplexLogic(method)) {
                        return true;
                    }
                    return false;
                }
            }

            // 智能判断setter方法
            if (methodName.startsWith("set") && methodName.length() > 3 && paramCount == 1) {
                String propertyName = extractPropertyNameFromSetter(methodName);
                if (fieldNames.contains(propertyName)) {
                    String paramType = method.getParameters().get(0).getTypeAsString();
                    String fieldType = fieldTypes.get(propertyName);
                    if (paramType.equals(fieldType) || isCompatibleType(paramType, fieldType)) {
                        if (hasComplexLogic(method)) {
                            return true;
                        }
                        return false;
                    }
                }
            }

            // 过滤Object类的常见方法
            if (("toString".equals(methodName) || "hashCode".equals(methodName) ||
                    "equals".equals(methodName) || "finalize".equals(methodName)) && paramCount <= 1) {
                if (method.getJavadoc().isPresent() &&
                        method.getJavadoc().get().getDescription().toText().length() > 50) {
                    return true;
                }
                if (method.getBody().isPresent()) {
                    String body = method.getBody().get().toString();
                    if (body.length() > 200) {
                        return true;
                    }
                }
                return false;
            }

            // 过滤简单的Builder模式方法
            if (methodName.equals("build") && paramCount == 0) {
                return false;
            }

            return true;
        }

        private boolean isFieldSemanticallyImportant(FieldDeclaration field) {
            String fieldName = field.getVariable(0).getNameAsString();
            String fieldType = field.getVariable(0).getTypeAsString();

            // 过滤序列化ID
            if ("serialVersionUID".equals(fieldName) && field.isStatic() && field.isFinal()) {
                return false;
            }

            // 过滤简单的日志记录器
            if (fieldType.contains("Logger") && field.isStatic() && field.isFinal()) {
                return false;
            }

            // 过滤常量（除非有重要注释）
            if (field.isStatic() && field.isFinal() && field.getJavadoc().isEmpty()) {
                return false;
            }

            return true;
        }

        private String extractPropertyNameFromGetter(String methodName) {
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        }

        private String extractPropertyNameFromSetter(String methodName) {
            if (methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            }
            return methodName;
        }

        private boolean hasComplexLogic(MethodDeclaration method) {
            if (!method.getBody().isPresent()) {
                return false;
            }

            String body = method.getBody().get().toString();
            return body.contains("if") || body.contains("for") ||
                    body.contains("while") || body.contains("throw") ||
                    body.contains("try") || body.contains("catch") ||
                    body.contains("switch") || body.contains("case") ||
                    body.length() > 150;
        }

        private boolean isCompatibleType(String paramType, String fieldType) {
            Map<String, String> primitiveToWrapper = Map.of(
                    "int", "Integer",
                    "long", "Long",
                    "double", "Double",
                    "float", "Float",
                    "boolean", "Boolean",
                    "char", "Character",
                    "byte", "Byte",
                    "short", "Short"
            );

            if (primitiveToWrapper.containsKey(fieldType) &&
                    primitiveToWrapper.get(fieldType).equals(paramType)) {
                return true;
            }
            if (primitiveToWrapper.containsKey(paramType) &&
                    primitiveToWrapper.get(paramType).equals(fieldType)) {
                return true;
            }

            if (paramType.contains("<") && fieldType.contains("<")) {
                String paramBase = paramType.split("<")[0];
                String fieldBase = fieldType.split("<")[0];
                return paramBase.equals(fieldBase);
            }

            return false;
        }
    }
}
