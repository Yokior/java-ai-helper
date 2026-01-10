package com.yokior.common;


/**
 * @author Yokior
 * @description 过滤表达式
 * @date 2026/1/10 21:18
 */
public class FilterExpression {

    private StringBuilder expression;

    private FilterExpression(StringBuilder expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private StringBuilder expression;

        private Builder() {
            this.expression = new StringBuilder();
        }

        public FilterExpression build() {
            return new FilterExpression(expression);
        }

        private void handle(String field, String type ,Object value) {
            expression.append(field).append(" ").append(type);
            if (value instanceof String) {
                expression.append(" \"").append(value).append("\"");
            } else {
                expression.append(" ").append(value);
            }
        }

        public Builder and() {
            expression.append(" && ");
            return this;
        }

        public Builder or() {
            expression.append(" || ");
            return this;
        }

        public Builder gt(String field, Object value) {
            handle(field, ">", value);
            return this;
        }

        public Builder gte(String field, Object value) {
            handle(field, ">=", value);
            return this;
        }

        public Builder lt(String field, Object value) {
            handle(field, "<", value);
            return this;
        }

        public Builder lte(String field, Object value) {
            handle(field, "<=", value);
            return this;
        }

        public Builder eq(String field, Object value) {
            handle(field, "==", value);
            return this;
        }

        public Builder ne(String field, Object value) {
            handle(field, "!=", value);
            return this;
        }

        public Builder like(String field, Object value) {
            handle(field, "like", value);
            return this;
        }

        public Builder in(String field, Object value) {
            handle(field, "in", value);
            return this;
        }

        public Builder notIn(String field, Object value) {
            handle(field, "not in", value);
            return this;
        }


    }
}
