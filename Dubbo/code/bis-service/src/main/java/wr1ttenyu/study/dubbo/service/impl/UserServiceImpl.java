package wr1ttenyu.study.dubbo.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import wr1ttenyu.study.dubbo.dao.UserDao;
import wr1ttenyu.study.dubbo.entity.UUser;
import wr1ttenyu.study.dubbo.service.UserService;

@Service(
        version = "1.0.0",
        application = "${dubbo.application.id}",
        protocol = "${dubbo.protocol.id}",
        registry = "${dubbo.registry.id}"
)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public UUser getUserById(String id) {
        return userDao.getUserById(id);
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
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateUser(UUser user) {
        userDao.updateUser(user);
    }
}
