package com.yokior.split;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Java代码解析测试类
 * 根据切分方案文档展示如何解析Java类并提取元数据
 *
 * @author Yokior
 * @description 测试JavaParser解析功能和代码切分方案
 * @date 2026/1/5
 */
public class ParserTest {

    private static final String RESOURCES_PATH = "src/test/resources";
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaParser();
    }

    @Test
    @DisplayName("测试解析普通类 - TestService")
    public void testParseNormalClass() throws Exception {
        System.out.println("\n========== 测试解析普通类: TestService ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "TestService.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("测试解析接口 - IUserService")
    public void testParseInterface() throws Exception {
        System.out.println("\n========== 测试解析接口: IUserService ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "IUserService.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("测试解析抽象类 - AbstractBaseService")
    public void testParseAbstractClass() throws Exception {
        System.out.println("\n========== 测试解析抽象类: AbstractBaseService ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "AbstractBaseService.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("测试解析枚举类 - UserStatus")
    public void testParseEnum() throws Exception {
        System.out.println("\n========== 测试解析枚举类: UserStatus ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "UserStatus.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("测试解析普通实体类 - User")
    public void testParseEntityClass() throws Exception {
        System.out.println("\n========== 测试解析普通实体类: User ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "User.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("测试解析仓储接口 - UserRepository")
    public void testParseRepositoryInterface() throws Exception {
        System.out.println("\n========== 测试解析仓储接口: UserRepository ==========");

        Path filePath = Paths.get(RESOURCES_PATH, "UserRepository.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("优化效果对比测试 - 展示过滤效果")
    public void testOptimizationEffect() throws Exception {
        System.out.println("\n========== 优化效果对比测试: User实体类 ==========");
        System.out.println("注意观察getter/setter方法被过滤的效果\n");

        Path filePath = Paths.get(RESOURCES_PATH, "User.java");
        parseAndDisplay(filePath);
    }

    @Test
    @DisplayName("智能过滤测试 - 验证get开头方法的智能判断")
    public void testSmartFiltering() throws Exception {
        System.out.println("\n========== 智能过滤测试: UserServiceWithGetMethod ==========");
        System.out.println("注意观察：");
        System.out.println("- getName()、getAge() 等真正的getter方法被过滤");
        System.out.println("- getUserData()、getUserStatistics() 等业务方法被保留");
        System.out.println("- setUserConfig() 有复杂逻辑被保留\n");

        Path filePath = Paths.get(RESOURCES_PATH, "UserServiceWithGetMethod.java");
        parseAndDisplay(filePath);
    }

    /**
     * 解析并展示Java文件的元数据
     */
    private void parseAndDisplay(Path filePath) throws Exception {
        File file = filePath.toFile();
        if (!file.exists()) {
            System.err.println("文件不存在: " + filePath);
            return;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            ParseResult<CompilationUnit> parseResult = parser.parse(in);

            if (!parseResult.isSuccessful()) {
                System.err.println("解析失败: " + parseResult.getProblems());
                return;
            }

            CompilationUnit cu = parseResult.getResult().orElse(null);
            if (cu != null) {
                // 创建代码分析器
                CodeAnalyzer analyzer = new CodeAnalyzer();

                // 提取并展示元信息
                analyzer.displayClassMetaInfo(cu);

                // 访问所有类型声明
                cu.accept(analyzer, null);
            }
        }
    }

    /**
     * 代码分析器类
     * 实现基于切分方案的元数据提取
     */
    private static class CodeAnalyzer extends VoidVisitorAdapter<Void> {

        // 缓存当前类的属性信息
        private Set<String> fieldNames = new HashSet<>();
        private Map<String, String> fieldTypes = new HashMap<>();
        private ClassOrInterfaceDeclaration currentClass;

        /**
         * 设置当前分析的类，提取属性信息
         */
        private void setCurrentClass(ClassOrInterfaceDeclaration clazz) {
            this.currentClass = clazz;
            this.fieldNames.clear();
            this.fieldTypes.clear();

            // 提取所有字段名称和类型
            clazz.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    String fieldName = variable.getNameAsString();
                    String fieldType = variable.getTypeAsString();
                    fieldNames.add(fieldName);
                    fieldTypes.put(fieldName, fieldType);
                });
            });
        }

        /**
         * 判断方法是否具有语义重要性
         * 基于类属性智能判断getter/setter方法
         */
        private boolean isMethodSemanticallyImportant(MethodDeclaration method) {
            String methodName = String.valueOf(method.getName());
            int paramCount = method.getParameters().size();

            // 智能判断getter方法
            if (methodName.matches("^(get|is)[A-Z].*") && paramCount == 0) {
                String propertyName = extractPropertyNameFromGetter(methodName);

                // 只有当对应的属性存在时，才认为是getter方法
                if (fieldNames.contains(propertyName)) {
                    // 如果方法有重要的JavaDoc注释，则保留
                    if (method.getJavadoc().isPresent() &&
                        method.getJavadoc().get().getDescription().toText().length() > 20) {
                        return true;
                    }
                    // 检查是否有复杂逻辑
                    if (hasComplexLogic(method)) {
                        return true;
                    }
                    return false; // 简单的getter，过滤
                }
                // 如果不是对应属性的getter，保留（可能是真正的业务方法）
            }

            // 智能判断setter方法
            if (methodName.startsWith("set") && methodName.length() > 3 &&
                paramCount == 1) {
                String propertyName = extractPropertyNameFromSetter(methodName);

                // 只有当对应的属性存在时，才认为是setter方法
                if (fieldNames.contains(propertyName)) {
                    // 检查参数类型是否匹配
                    String paramType = method.getParameters().get(0).getTypeAsString();
                    String fieldType = fieldTypes.get(propertyName);

                    if (paramType.equals(fieldType) || isCompatibleType(paramType, fieldType)) {
                        // 检查是否有复杂的逻辑（不只是简单赋值）
                        if (hasComplexLogic(method)) {
                            return true;
                        }
                        return false; // 简单的setter，过滤
                    }
                }
                // 如果不是对应属性的setter，保留
            }

            // 过滤Object类的常见方法（除非有重要实现）
            if (("toString".equals(methodName) || "hashCode".equals(methodName) ||
                 "equals".equals(methodName) || "finalize".equals(methodName))
                && paramCount <= 1) {
                // 检查是否有重要的JavaDoc或复杂实现
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

            // 其他情况认为有语义重要性
            return true;
        }

        /**
         * 从getter方法名提取属性名
         */
        private String extractPropertyNameFromGetter(String methodName) {
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return methodName;
        }

        /**
         * 从setter方法名提取属性名
         */
        private String extractPropertyNameFromSetter(String methodName) {
            if (methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            }
            return methodName;
        }

        /**
         * 检查方法是否有复杂逻辑
         */
        private boolean hasComplexLogic(MethodDeclaration method) {
            if (!method.getBody().isPresent()) {
                return false; // 抽象方法
            }

            String body = method.getBody().get().toString();
            // 如果方法体包含逻辑判断、循环、异常处理等，则认为有复杂逻辑
            return body.contains("if") || body.contains("for") ||
                   body.contains("while") || body.contains("throw") ||
                   body.contains("try") || body.contains("catch") ||
                   body.contains("switch") || body.contains("case") ||
                   body.length() > 150; // 代码行数较多
        }

        /**
         * 检查类型是否兼容（处理装箱类型等）
         */
        private boolean isCompatibleType(String paramType, String fieldType) {
            // 处理基本类型和包装类型的兼容性
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

            // 如果是基本类型与包装类型的关系
            if (primitiveToWrapper.containsKey(fieldType) &&
                primitiveToWrapper.get(fieldType).equals(paramType)) {
                return true;
            }
            if (primitiveToWrapper.containsKey(paramType) &&
                primitiveToWrapper.get(paramType).equals(fieldType)) {
                return true;
            }

            // 处理泛型类型擦除
            if (paramType.contains("<") && fieldType.contains("<")) {
                String paramBase = paramType.split("<")[0];
                String fieldBase = fieldType.split("<")[0];
                return paramBase.equals(fieldBase);
            }

            return false;
        }

        /**
         * 判断字段是否具有语义重要性
         * 过滤掉简单的序列化ID、日志记录器等
         */
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
            if (field.isStatic() && field.isFinal() &&
                field.getJavadoc().isEmpty()) {
                return false;
            }

            return true;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            System.out.println("\n----- " + (n.isInterface() ? "接口" : "类") + "信息 -----");

            // 设置当前类信息，用于智能判断getter/setter
            setCurrentClass(n);

            // [CLASS_META] 类元信息
            displayClassMeta(n);

            // [CLASS_STRUCTURE] 类结构概览
            displayClassStructure(n);

            // [MEMBER_DETAIL] 具体实现
            displayMemberDetails(n);

            super.visit(n, arg);
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            System.out.println("\n----- 枚举信息 -----");

            displayEnumMeta(n);
            displayEnumStructure(n);
            displayEnumDetails(n);

            super.visit(n, arg);
        }

        /**
         * 显示编译单元元信息
         */
        public void displayClassMetaInfo(CompilationUnit cu) {
            System.out.println("\n[CLASS_META - 编译单元]");

            // 包声明
            cu.getPackageDeclaration().ifPresent(pkg ->
                System.out.println("包声明: " + pkg.getName()));

            // 导入语句
            if (cu.getImports().size() > 0) {
                System.out.println("\n导入语句:");
                cu.getImports().forEach(import_ ->
                    System.out.println("  " + import_.getName()));
            }
        }

        /**
         * [CLASS_META] 显示类或接口的元信息
         */
        private void displayClassMeta(ClassOrInterfaceDeclaration n) {
            System.out.println("\n[CLASS_META]");

            // 类定义
            System.out.println("类型: " + (n.isInterface() ? "接口" :
                                  n.isAbstract() ? "抽象类" : "普通类"));
            System.out.println("名称: " + n.getName());

            // 修饰符
            if (!n.getModifiers().isEmpty()) {
                System.out.println("修饰符: " + n.getModifiers());
            }

            // 类型参数（泛型）
            if (!n.getTypeParameters().isEmpty()) {
                System.out.println("类型参数: " + n.getTypeParameters());
            }

            // 扩展类
            n.getExtendedTypes().forEach(extType ->
                System.out.println("扩展: " + extType));

            // 实现接口
            if (!n.getImplementedTypes().isEmpty()) {
                System.out.println("实现接口:");
                n.getImplementedTypes().forEach(implType ->
                    System.out.println("  " + implType));
            }

            // 注解
            if (!n.getAnnotations().isEmpty()) {
                System.out.println("注解:");
                n.getAnnotations().forEach(annotation ->
                    System.out.println("  @" + annotation.getName()));
            }

            // JavaDoc注释
            n.getJavadoc().ifPresent(javadoc -> {
                System.out.println("\nJavaDoc:");
                System.out.println(javadoc.getDescription().toText());
            });
        }

        /**
         * [CLASS_STRUCTURE] 显示类结构概览
         */
        private void displayClassStructure(ClassOrInterfaceDeclaration n) {
            System.out.println("\n[CLASS_STRUCTURE]");

            // 字段列表（过滤语义不重要的字段）
            List<FieldDeclaration> fields = n.getFields().stream()
                    .filter(this::isFieldSemanticallyImportant)
                    .toList();
            if (!fields.isEmpty()) {
                System.out.println("\n重要字段列表:");
                fields.forEach(field -> {
                    field.getVariables().forEach(variable -> {
                        System.out.println("  " + field.getModifiers() + " " +
                                         variable.getType() + " " + variable.getName());
                        // 字段注解
                        field.getAnnotations().forEach(annotation ->
                            System.out.println("    @" + annotation.getName()));
                    });
                });
            } else {
                // 如果没有重要字段，显示所有字段但标记为辅助字段
                List<FieldDeclaration> allFields = n.getFields();
                if (!allFields.isEmpty()) {
                    System.out.println("\n辅助字段列表（已过滤）:");
                    allFields.forEach(field -> {
                        field.getVariables().forEach(variable -> {
                            System.out.println("  " + field.getModifiers() + " " +
                                             variable.getType() + " " + variable.getName() + " [辅助]");
                        });
                    });
                }
            }

            // 构造函数列表
            List<ConstructorDeclaration> constructors = n.getConstructors();
            if (!constructors.isEmpty()) {
                System.out.println("\n构造函数列表:");
                constructors.forEach(constructor -> {
                    System.out.println("  " + constructor.getName() +
                                     constructor.getParameters());
                    constructor.getAnnotations().forEach(annotation ->
                        System.out.println("    @" + annotation.getName()));
                });
            }

            // 方法签名列表（只显示语义重要的方法）
            List<MethodDeclaration> methods = n.getMethods().stream()
                    .filter(this::isMethodSemanticallyImportant)
                    .toList();
            if (!methods.isEmpty()) {
                System.out.println("\n重要方法签名列表:");
                methods.forEach(method -> {
                    System.out.println("  " + method.getModifiers() + " " +
                                     method.getType() + " " + method.getName() +
                                     method.getParameters());
                    // 如果方法有注释，显示简要信息
                    method.getJavadoc().ifPresent(javadoc -> {
                        String desc = javadoc.getDescription().toText();
                        if (desc.length() > 50) {
                            System.out.println("    注释: " + desc.substring(0, 50) + "...");
                        }
                    });
                });
            }

            // 显示被过滤的方法数量
            long filteredMethodCount = n.getMethods().stream()
                    .filter(m -> !isMethodSemanticallyImportant(m))
                    .count();
            if (filteredMethodCount > 0) {
                System.out.println("\n已过滤 " + filteredMethodCount + " 个辅助方法（getter/setter等）");
            }

            // 内部类列表
            List<TypeDeclaration> innerTypes = n.getMembers().stream()
                    .filter(m -> m instanceof TypeDeclaration)
                    .map(m -> (TypeDeclaration) m)
                    .toList();
            if (!innerTypes.isEmpty()) {
                System.out.println("\n内部类/接口列表:");
                innerTypes.forEach(type ->
                    System.out.println("  " + type.getName()));
            }
        }

        /**
         * [MEMBER_DETAIL] 显示成员详细信息
         */
        private void displayMemberDetails(ClassOrInterfaceDeclaration n) {
            System.out.println("\n[MEMBER_DETAIL]");

            // 重要字段详情
            List<FieldDeclaration> importantFields = n.getFields().stream()
                    .filter(this::isFieldSemanticallyImportant)
                    .toList();
            if (!importantFields.isEmpty()) {
                System.out.println("\n重要字段详情:");
                importantFields.forEach(field -> {
                    System.out.println(field.toString());
                });
            }

            // 重要方法详情
            List<MethodDeclaration> importantMethods = n.getMethods().stream()
                    .filter(this::isMethodSemanticallyImportant)
                    .toList();
            if (!importantMethods.isEmpty()) {
                System.out.println("\n重要方法详情:");
                importantMethods.forEach(method -> {
                    System.out.println("\n方法详情: " + method.getName());
                    System.out.println("  签名: " + method.getDeclarationAsString());

                    // 方法注释
                    method.getJavadoc().ifPresent(javadoc -> {
                        System.out.println("  JavaDoc:");
                        System.out.println("    " + javadoc.getDescription().toText());
                    });

                    // 方法体（如果是抽象方法则显示abstract）
                    if (!method.isAbstract() && method.getBody().isPresent()) {
                        System.out.println("  方法体:");
                        System.out.println("    " + method.getBody().get().toString()
                                           .replace("\n", "\n    "));
                    } else if (method.isAbstract()) {
                        System.out.println("  [抽象方法 - 无实现]");
                    }
                });
            }

            // 构造函数详情
            n.getConstructors().forEach(constructor -> {
                System.out.println("\n构造函数详情:");
                System.out.println(constructor.toString());
            });

            // 显示过滤统计
            long totalFields = n.getFields().size();
            long totalMethods = n.getMethods().size();
            long importantFieldsCount = importantFields.size();
            long importantMethodsCount = importantMethods.size();

            System.out.println("\n[过滤统计]");
            System.out.println("字段: " + importantFieldsCount + "/" + totalFields + " (重要/总数)");
            System.out.println("方法: " + importantMethodsCount + "/" + totalMethods + " (重要/总数)");
        }

        /**
         * 显示枚举元信息
         */
        private void displayEnumMeta(EnumDeclaration n) {
            System.out.println("\n[ENUM_DEFINITION]");
            System.out.println("枚举名称: " + n.getName());

            // 实现接口
            if (!n.getImplementedTypes().isEmpty()) {
                System.out.println("实现接口:");
                n.getImplementedTypes().forEach(type ->
                    System.out.println("  " + type));
            }

            // 注解
            if (!n.getAnnotations().isEmpty()) {
                System.out.println("注解:");
                n.getAnnotations().forEach(annotation ->
                    System.out.println("  @" + annotation.getName()));
            }

            // JavaDoc
            n.getJavadoc().ifPresent(javadoc -> {
                System.out.println("\nJavaDoc:");
                System.out.println(javadoc.getDescription().toText());
            });
        }

        /**
         * 显示枚举结构
         */
        private void displayEnumStructure(EnumDeclaration n) {
            System.out.println("\n[ENUM_MEMBERS]");

            // 枚举常量
            System.out.println("\n枚举常量:");
            n.getEntries().forEach(entry -> {
                System.out.println("  " + entry.getName());
                if (!entry.getArguments().isEmpty()) {
                    System.out.println("    参数: " + entry.getArguments());
                }
            });

            // 其他成员
            List<BodyDeclaration<?>> members = n.getMembers();
            if (!members.isEmpty()) {
                System.out.println("\n其他成员:");
                members.forEach(member -> {
                    if (member instanceof FieldDeclaration) {
                        FieldDeclaration field = (FieldDeclaration) member;
                        System.out.println("  字段: " + field);
                    } else if (member instanceof MethodDeclaration) {
                        MethodDeclaration method = (MethodDeclaration) member;
                        System.out.println("  方法: " + method.getDeclarationAsString());
                    }
                });
            }
        }

        /**
         * 显示枚举详情
         */
        private void displayEnumDetails(EnumDeclaration n) {
            System.out.println("\n[ENUM_CONSTANT_DETAIL]");

            n.getEntries().forEach(entry -> {
                System.out.println("\n常量详情: " + entry.getName());
                entry.getJavadoc().ifPresent(javadoc -> {
                    System.out.println("  注释: " + javadoc.getDescription().toText());
                });
            });
        }
    }
}
