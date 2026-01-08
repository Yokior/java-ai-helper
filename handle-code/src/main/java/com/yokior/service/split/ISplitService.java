package com.yokior.service.split;

import com.yokior.common.SplitChunk;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/7 16:47
 */
public interface ISplitService {

    /**
     * 加载文件并分块
     *
     * @param filePath 文件路径
     * @param projectName 项目名
     * @return
     */
    List<SplitChunk> loadAndSplit(String filePath, String projectName) throws Exception;


    List<SplitChunk> loadAndSplit(Path filePath, String projectName) throws Exception;

}
