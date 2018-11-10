package wr1ttenyu.study.springboot.demo.curd.service;

import wr1ttenyu.study.springboot.demo.curd.bean.UUser;


public interface UserService {

    UUser getUserById(String id);

    UUser insertUser(UUser user);

    void deleteUser(String id);

    void updateUser(UUser user);

    void testSprintTran();
}
