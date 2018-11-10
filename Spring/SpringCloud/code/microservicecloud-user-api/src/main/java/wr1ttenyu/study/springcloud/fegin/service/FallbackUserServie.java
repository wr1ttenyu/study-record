package wr1ttenyu.study.springcloud.fegin.service;

import com.netflix.hystrix.Hystrix;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;
import wr1ttenyu.study.springcloud.entity.UUser;

import java.util.Date;

@Component
public class FallbackUserServie implements FallbackFactory<UserFeginService> {

    @Override
    public UserFeginService create(Throwable throwable) {
        return new UserFeginService() {
            @Override
            public UUser getUserById(String id) {
                return new UUser("999", "服务降级", new Date(), new Date());
            }

            @Override
            public UUser insertUser(UUser user) {
                return new UUser("999", "服务降级", new Date(), new Date());
            }

            @Override
            public void deleteUser(String id) {

            }

            @Override
            public void updateUser(UUser user) {
                Hystrix hystrix = new Hystrix()
                        ;

            }
        };
    }
}
