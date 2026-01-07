package com.yokior.split;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.yokior.common.SplitChunk;
import com.yokior.service.split.ISplitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/7 16:49
 */
@SpringBootTest
public class SplitServiceTest {

    @Autowired
    private ISplitService splitService;

    @Test
    void testSplitClass() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\User.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            System.out.println(chunk);
        }
    }

    @Test
    void testSplitInterface() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\IUserService.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }

    @Test
    void testSplitAbstractClass() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("D:\\Java_Project\\java-ai-helper\\handle-code\\src\\test\\resources\\AbstractBaseService.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }
    @Test
    void testSplitClassWithGetMethod() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\UserServiceWithGetMethod.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }

    @Test
    void testSplitEnum() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\UserStatus.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }


    @Test
    void testSplitRepository() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\UserRepository.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }

    @Test
    void testSplitService() throws Exception {
        List<SplitChunk> chunkList = splitService.loadAndSplit("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj\\a\\TestService.java", "jjj");
        for (SplitChunk chunk : chunkList) {
            // 规整JSON格式输出
            System.out.println(JSON.toJSONString(chunk, JSONWriter.Feature.PrettyFormat));
        }
    }
}
