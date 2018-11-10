package wr1ttenyu.study.cloud.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import wr1ttenyu.study.cloud.dao.UserDao;
import wr1ttenyu.study.springcloud.entity.UUser;
import wr1ttenyu.study.springcloud.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private Logger Log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDao userDao;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UUser getUserById(String id) {
        UUser user = userDao.getUserById(id);
        return user;
    }

    @Override
    public UUser insertUser(UUser user) {
        userDao.insertUser(user);
        return user;
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        userDao.deleteUserById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateUser(UUser user) {
        userDao.updateUser(user);
    }
}
