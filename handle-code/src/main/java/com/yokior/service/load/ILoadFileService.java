package com.yokior.service.load;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:10
 */
public interface ILoadFileService {

    /**
     * 加载文件
     *
     * @param filePath 文件路径
     * @return 加载结果
     */
    String loadFile(String filePath);

}
