package com.clouddrive.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private Long storageSize;
    private Long usedSize;
    private Date createTime;
    private Date updateTime;
}
