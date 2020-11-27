package com.arqaam.logframelab.configuration.security;

import com.arqaam.logframelab.model.persistence.auth.User;
import com.arqaam.logframelab.service.auth.GroupService;
import com.arqaam.logframelab.service.auth.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextRefreshEventListener {

  // TODO investigate more secure way of storing such credentials
  private static final String SEC_ADMIN_GROUP_NAME = "SEC_ADMIN";
  @Value("${logframelab.secadmin.password}")
  private String SEC_ADMIN_PASSWORD;
  private static final String DEFAULT_SEC_ADMIN_USERNAME = "secadmin";
  private static final String SYSTEM = "SYSTEM";

  private final UserService userService;
  private final GroupService groupService;
  private final PasswordEncoder passwordEncoder;

  public ContextRefreshEventListener(
      UserService userService, GroupService groupService, PasswordEncoder passwordEncoder) {
    this.userService = userService;
    this.groupService = groupService;
    this.passwordEncoder = passwordEncoder;
  }

  @EventListener
  public void initSecurityAdminUser(ContextRefreshedEvent event) {
    List<User> usersByGroupName = userService.getUserByGroupName(SEC_ADMIN_GROUP_NAME);
    if (usersByGroupName.isEmpty()) {
      User secAdminUser =
          User.builder()
              .username(DEFAULT_SEC_ADMIN_USERNAME)
              .password(passwordEncoder.encode(SEC_ADMIN_PASSWORD))
              .enabled(true)
              .createdBy(SYSTEM)
              .build();
      secAdminUser.addGroup(groupService.findByGroupName(SEC_ADMIN_GROUP_NAME));

      userService.createOrUpdateUser(secAdminUser);
    }
  }

}
