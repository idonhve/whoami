package com.whoami.module.oplog.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志参数脱敏：递归遍历 JSON，把敏感字段的值替换为 ***。
 * 敏感字段按「小写后包含关键词」判定（password / secret / token / authorization）。
 */
public final class SensitiveMasker {

    public static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "secret", "token", "authorization", "credential");

    private SensitiveMasker() {
    }

    public static JsonNode mask(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    field.setValue(TextNode.valueOf(MASK));
                } else {
                    mask(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                mask(child);
            }
        }
        return node;
    }

    static boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
