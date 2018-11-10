package wr1ttenyu.study.springboot.demo.curd.dao;

import org.apache.ibatis.annotations.*;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;

public interface UserDao {

    @Select("select * from u_user where id=#{id}")
    public UUser getUserById(String id);

    @Delete("delete from u_user where id=#{id}")
    public int deleteUserById(String id);

    @Insert("insert into u_user(id, name, gmt_create, gmt_modified) values(#{id}, #{name}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    public int insertUser(UUser user);

    @Update("update u_user set name=#{name} where id=#{id}")
    public int updateUser(UUser user);

}
