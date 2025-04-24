package com.randy.chin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.randy.chin.entity.User;
import com.randy.chin.mapper.UserMapper;
import com.randy.chin.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}