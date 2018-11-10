package wr1ttenyu.study.dubbo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UUser {

    private String id;

    private String name;

    private Date gmtCreate;

    private Date gmtModified;

}
