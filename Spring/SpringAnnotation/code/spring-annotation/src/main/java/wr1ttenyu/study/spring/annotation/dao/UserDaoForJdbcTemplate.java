package wr1ttenyu.study.spring.annotation.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import wr1ttenyu.study.spring.annotation.entity.UserDo;

import java.util.Random;
import java.util.UUID;

@Repository
public class UserDaoForJdbcTemplate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insert() {
        String sql = "INSERT INTO user (id, name, age) VALUES (?, ?, ?)";
        String userId = UUID.randomUUID().toString();
        String userName = userId.substring(0, 10);
        Integer age = (int) (Math.random() * 100);
        jdbcTemplate.update(sql, userId, userName, age);
    }
}