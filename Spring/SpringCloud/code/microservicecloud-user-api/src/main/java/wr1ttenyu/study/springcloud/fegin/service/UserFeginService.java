package wr1ttenyu.study.springcloud.fegin.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wr1ttenyu.study.springcloud.entity.UUser;

@FeignClient(value = "MICROSERVICECLOUD-USER-PROVIDER", fallbackFactory = FallbackUserServie.class)
public interface UserFeginService {

    @GetMapping("/user/get/{id}")
    UUser getUserById(@PathVariable("id") String id);

    @PostMapping("/user/add")
    UUser insertUser(@RequestBody UUser user);

    @GetMapping("/user/del/{id}")
    void deleteUser(String id);

    @PostMapping("/user/add")
    void updateUser(UUser user);

}
