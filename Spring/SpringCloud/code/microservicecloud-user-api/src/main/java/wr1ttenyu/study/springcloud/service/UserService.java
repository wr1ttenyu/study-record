package wr1ttenyu.study.springcloud.service;

import wr1ttenyu.study.springcloud.entity.UUser;

public interface UserService {

    UUser getUserById(String id);

    UUser insertUser(UUser user);

    void deleteUser(String id);

    void updateUser(UUser user);

}
