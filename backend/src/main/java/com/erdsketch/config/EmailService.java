package com.erdsketch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@erdsketch.fly.dev}")
    private String fromAddress;

    public void sendWorkspaceInvite(String toEmail, String inviteeName, String workspaceName, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("[ErdSketch] " + workspaceName + " 워크스페이스에 초대되었습니다");
        message.setText(
            inviteeName + "님, 안녕하세요!\n\n" +
            inviterName + "님이 '" + workspaceName + "' 워크스페이스에 초대했습니다.\n\n" +
            "ErdSketch에 로그인하여 워크스페이스를 확인하세요.\n" +
            "https://erdsketch.fly.dev\n\n" +
            "감사합니다,\nErdSketch 팀"
        );
        mailSender.send(message);
    }
}
