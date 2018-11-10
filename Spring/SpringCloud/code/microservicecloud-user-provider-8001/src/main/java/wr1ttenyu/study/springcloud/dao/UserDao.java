package wr1ttenyu.study.springcloud.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import wr1ttenyu.study.springcloud.entity.UUser;

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
