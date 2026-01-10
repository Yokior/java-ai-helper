package com.yokior.service.embedding;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:07
 */
@Service
public class EmbeddingServiceImpl implements IEmbeddingService {

    private EmbeddingModel embeddingModel;

    public EmbeddingServiceImpl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Float> embedding(String text) {
        float[] floats = embeddingModel.embed(text);
        return Arrays.asList(ArrayUtils.toObject(floats));
    }
}
