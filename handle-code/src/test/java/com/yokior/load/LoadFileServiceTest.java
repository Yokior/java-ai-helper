package com.yokior.load;

import com.yokior.service.load.ILoadFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:19
 */
@SpringBootTest
public class LoadFileServiceTest {

    @Autowired
    private ILoadFileService loadFileService;

    @Test
    void loadFile() {
        String path = loadFileService.unzipAndSaveFile("D:\\Java_Project\\java-ai-helper\\handle-code\\src\\test\\resources\\test_project.zip");
        System.out.println(path);
    }
}
