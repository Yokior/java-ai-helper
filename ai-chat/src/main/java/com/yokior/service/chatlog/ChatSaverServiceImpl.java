package com.yokior.service.chatlog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yokior.entity.ChatLog;
import com.yokior.mapper.ChatlogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Yokior
 * @description
 * @date 2026/1/13 17:16
 */
@Service
@Slf4j
public class ChatSaverServiceImpl extends ServiceImpl<ChatlogMapper, ChatLog> implements IChatSaverService{
}
