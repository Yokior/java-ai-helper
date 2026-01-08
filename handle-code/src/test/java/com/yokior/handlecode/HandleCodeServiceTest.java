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

}
