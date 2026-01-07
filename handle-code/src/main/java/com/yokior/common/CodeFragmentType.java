package com.yokior.common;

/**
 * @author Yokior
 * @description 代码分片类型枚举
 * @date 2026/1/8
 */
public enum CodeFragmentType {

    /**
     * 类概览信息
     */
    CLASS_OVERVIEW("CLASS_OVERVIEW", "类概览"),

    /**
     * 方法详情
     */
    METHOD_DETAIL("METHOD_DETAIL", "方法详情"),

    /**
     * 接口定义
     */
    INTERFACE_DEFINITION("INTERFACE_DEFINITION", "接口定义"),

    /**
     * 默认方法
     */
    DEFAULT_METHOD("DEFAULT_METHOD", "默认方法"),

    /**
     * 静态方法
     */
    STATIC_METHOD("STATIC_METHOD", "静态方法"),

    /**
     * 抽象类元信息
     */
    ABSTRACT_CLASS_META("ABSTRACT_CLASS_META", "抽象类元信息"),

    /**
     * 具体实现成员
     */
    CONCRETE_MEMBERS("CONCRETE_MEMBERS", "具体实现成员"),

    /**
     * 抽象方法详情
     */
    ABSTRACT_METHOD_DETAIL("ABSTRACT_METHOD_DETAIL", "抽象方法详情"),

    /**
     * 枚举定义
     */
    ENUM_DEFINITION("ENUM_DEFINITION", "枚举定义"),

    /**
     * 枚举成员
     */
    ENUM_MEMBERS("ENUM_MEMBERS", "枚举成员"),

    /**
     * 枚举常量详情
     */
    ENUM_CONSTANT_DETAIL("ENUM_CONSTANT_DETAIL", "枚举常量详情");

    private final String code;
    private final String description;

    CodeFragmentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举类型
     */
    public static CodeFragmentType fromCode(String code) {
        for (CodeFragmentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}