package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.idle.config.WeChatProperties;
import com.community.idle.dto.LoginDTO;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.JwtUtil;
import com.community.idle.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private final RestTemplate restTemplate;
    private final WeChatProperties weChatProperties;
    private final UserMapper userMapper;

    public AuthService(RestTemplate restTemplate, WeChatProperties weChatProperties, UserMapper userMapper) {
        this.restTemplate = restTemplate;
        this.weChatProperties = weChatProperties;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        Map<String, Object> wechatResult = code2Session(dto.getCode());

        String openid = (String) wechatResult.get("openid");
        String sessionKey = (String) wechatResult.get("session_key");

        if (openid == null) {
            throw new BusinessException("微信登录失败");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            user.setNickname(dto.getNickname());
            user.setAvatar(dto.getAvatar());
            user.setGender(dto.getGender());
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            user.setSessionKey(sessionKey);
            if (dto.getNickname() != null) {
                user.setNickname(dto.getNickname());
            }
            if (dto.getAvatar() != null) {
                user.setAvatar(dto.getAvatar());
            }
            if (dto.getGender() != null) {
                user.setGender(dto.getGender());
            }
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }

        String token = JwtUtil.generateToken(openid, user.getId());

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .openid(openid)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> code2Session(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(weChatProperties.getCode2sessionUrl())
                .queryParam("appid", weChatProperties.getAppid())
                .queryParam("secret", weChatProperties.getSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        log.info("调用微信code2Session接口: {}", url);

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result != null && result.get("errcode") != null) {
            Integer errcode = (Integer) result.get("errcode");
            String errmsg = (String) result.get("errmsg");
            log.error("微信code2Session接口调用失败, errcode: {}, errmsg: {}", errcode, errmsg);
            throw new BusinessException("微信登录失败: " + errmsg);
        }

        return result;
    }
}
