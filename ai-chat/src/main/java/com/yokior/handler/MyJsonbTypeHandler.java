package com.yokior.handler;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.io.*;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.OTHER})
@Slf4j
public class MyJsonbTypeHandler extends BaseTypeHandler<LinkedList<Checkpoint>> {

    private static final String JSONB = "jsonb";
    private static final String JSON = "json";

    private final Serializer<Checkpoint> checkpointSerializer = new CheckPointSerializer(new SpringAIJacksonStateSerializer(OverAllState::new, new ObjectMapper()));


    /**
     * 写数据库时，把java对象转成JSONB类型
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LinkedList<Checkpoint> parameter, JdbcType jdbcType) throws SQLException {
        if (ps != null) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType(JSONB);
            LinkedList<Checkpoint> list = parameter;
            try {
                jsonObject.setValue(serializeCheckpoints(list));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ps.setObject(i, jsonObject);
        }
    }

    /**
     * 读数据时，把JSONB类型的字段转成java对象
     */
    @Override
    public LinkedList<Checkpoint> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String v = rs.getString(columnName);
        try {
            return deserializeCheckpoints(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读数据时，把JSONB类型的字段转成java对象
     */
    @Override
    public LinkedList<Checkpoint> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String v = rs.getString(columnIndex);
        try {
            return deserializeCheckpoints(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读数据时，把JSONB类型的字段转成java对象
     */
    @Override
    public LinkedList<Checkpoint> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String v = cs.getString(columnIndex);
        try {
            return deserializeCheckpoints(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private String serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeInt(checkpoints.size());
            for (Checkpoint checkpoint : checkpoints) {
                checkpointSerializer.write(checkpoint, oos);
            }
            oos.flush();
            byte[] bytes = baos.toByteArray();
            return String.format("""
                    { "data": "%s" }
                    """, Base64.getEncoder().encodeToString(bytes));
        }
    }

    private LinkedList<Checkpoint> deserializeCheckpoints(String content) throws IOException, ClassNotFoundException {
        if (content == null || content.isEmpty()) {
            return new LinkedList<>();
        }

        // 解析JSON 获取data数据
        JSONObject jsonObject = JSONObject.parseObject(content);
        String data = jsonObject.getString("data");

        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            int size = ois.readInt();
            LinkedList<Checkpoint> checkpoints = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                checkpoints.add(checkpointSerializer.read(ois));
            }
            return checkpoints;
        }
    }

}