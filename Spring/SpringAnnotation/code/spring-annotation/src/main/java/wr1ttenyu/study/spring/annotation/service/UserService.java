package wr1ttenyu.study.spring.annotation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wr1ttenyu.study.spring.annotation.dao.UserDaoForJdbcTemplate;

@Service
public class UserService {

    @Autowired
    private UserDaoForJdbcTemplate userDao;

    @Transactional
    public void addUser() {
        userDao.insert();
        int i = 1 / 0;
    }

}
