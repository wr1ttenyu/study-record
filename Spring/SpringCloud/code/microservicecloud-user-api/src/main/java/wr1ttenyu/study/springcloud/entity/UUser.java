package wr1ttenyu.study.springcloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class UUser implements Serializable {

    private String id;

    private String name;

    private Date gmtCreate;

    private Date gmtModified;

}

