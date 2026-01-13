package com.yokior.service.userconversation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yokior.entity.UserConversation;
import com.yokior.mapper.UserConversationMapper;
import org.springframework.stereotype.Service;

/**
 * @author Yokior
 * @description
 * @date 2026/1/14 0:17
 */
@Service
public class UserConversationServiceImpl extends ServiceImpl<UserConversationMapper, UserConversation> implements IUserConversationService {
}
