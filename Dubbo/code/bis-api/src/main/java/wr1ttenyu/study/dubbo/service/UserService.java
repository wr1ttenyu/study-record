package wr1ttenyu.study.dubbo.service;


import wr1ttenyu.study.dubbo.entity.UUser;

public interface UserService {

    UUser getUserById(String id);

    UUser insertUser(UUser user);

    void deleteUser(String id);

    void updateUser(UUser user);

}
