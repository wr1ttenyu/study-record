package wr1ttenyu.study.springboot.demo.curd.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.component.RedisdbNode;
import wr1ttenyu.study.springboot.demo.curd.config.MyCacheAnnotation;
import wr1ttenyu.study.springboot.demo.curd.dao.UserDao;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;
import wr1ttenyu.study.springboot.demo.curd.service.UserService2;
import wr1ttenyu.study.springboot.demo.curd.utils.MySpringContext;

@Service
public class UserServiceImpl implements UserService {

    private Logger Log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserService2 userService2;

    @Autowired
    private UserService userService;

    /**
     * 一个CacheManager 可以管理多个缓存组件
     * CacheManager
     * |----- cache
     * <p>
     * 使用@Cacheable 开启缓存
     * Cacheable 属性：
     * cacheNames：Names of the caches in which method invocation results are stored.
     * <p>
     * key: Spring Expression Language (SpEL) expression for computing the key dynamically.
     * Default is {@code ""}, meaning all method parameters are considered as a key,
     * unless a custom keyGenerator has been configured.
     * <p>
     * cacheManager: The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
     * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
     * is set already.
     * <p>
     * cacheResolver: The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
     * to use
     * <p>
     * condition: Spring Expression Language (SpEL) expression used for making the method
     * caching conditional.
     * <p>
     * unless: Spring Expression Language (SpEL) expression used to veto method caching
     * <p>
     * sync: Synchronize the invocation of the underlying method if several threads are
     * attempting to load a value for the same key. The synchronization leads to
     * a couple of limitations:
     *
     * @param id
     * @return
     */
    /*@MyCacheAnnotation(key = "args{0}", preFix = "my_user_", redisdb = "1", cacheLevel = "2")*/
    @Override
    /*@MyCacheAnnotation(redisDb = RedisdbNode.DB1)*/
    public UUser getUserById(String id) {
        Log.info("查询用户信息,id:{}", id);
        return userDao.getUserById(id);
    }

    @Override
    /*@CachePut(cacheResolver = "cacheResolver", key = "#user.id")*/
    public UUser insertUser(UUser user) {
        userDao.insertUser(user);
        return user;
    }

    @Override
    /*@CacheEvict(cacheResolver = "cacheResolver", cacheNames = {"user"}, key = "#user.id")*/
    @Transactional
    public void deleteUser(String id) {
        userDao.deleteUserById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateUser(UUser user) {
        userDao.updateUser(user);
        int i = 1/0;
    }

    @Override
    @Transactional
    public void testSprintTran() {
        /*try {
            UUser user = getUserById(6);
            user.setName("狗宇");
            deleteUser(5);
            ((UserService)(AopContext.currentProxy())).deleteUser(5);
            updateUser(user);
        } catch (Exception e) {
            UserService bean = MySpringContext.getBean(UserService.class);
            UUser userById = bean.getUserById(6);
            Log.info("getUser without Transactional, user name:{}", userById.getName());
            System.out.println(bean);
            UUser user = getUserById(6);
            Log.info("Before set Transactional, user name:{}", user.getName());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            user = getUserById(6);
            Log.info("After set Transactional, user name:{}", user.getName());
        }*/

        UUser user = getUserById("1");
        user.setName("狗宇");
        deleteUser("5");
        /*tranTest(user);*/
        /*UserService bean = MySpringContext.getBean(UserService.class);
        bean.updateUser(user);*/
        try {
            userService.updateUser(user);
        } catch (Exception e) {
            System.out.println(123);
        }
    }

    private void tranTest(UUser user) {
        userDao.updateUser(user);
    }
}
