package com.arqaam.logframelab.service;

import com.arqaam.logframelab.model.persistence.auth.User;

import java.util.Optional;

public interface UserService {

  Optional<User> getFirstUserByGroupName(String groupName);

  User save(User user);

  Optional<User> findByUsername(String username);
}
