package wr1ttenyu.study.spring.annotation.dao;

import java.util.List;
import wr1ttenyu.study.spring.annotation.entity.UserDo;

public interface UserDao {

    int deleteByPrimaryKey(String id);

    int insert(UserDo record);

    int insertSelective(UserDo record);

    UserDo selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(UserDo record);

    int updateByPrimaryKey(UserDo record);
}