package com.yokior.handlecode;

import com.yokior.service.handlecode.IHandleCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 17:29
 */
@SpringBootTest
public class HandleCodeServiceTest {

    @Autowired
    private IHandleCodeService handleCodeService;


    @Test
    void test() throws Exception {
        handleCodeService.splitAndEmbedAndSave("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj");
    }

    @Test
    void testAsync() throws Exception {
        handleCodeService.splitAndEmbedAndSaveAsync("C:\\Users\\M1891\\AppData\\Local\\Temp\\jjj");
    }


    /**
     * 测试springboot-ai-qa-system
     */
    @Test
    void springboot_ai_qa_systeam() throws Exception {
        handleCodeService.splitAndEmbedAndSaveAsync("C:\\Users\\M1891\\AppData\\Local\\Temp\\springboot-ai-qa-system");
    }

}
