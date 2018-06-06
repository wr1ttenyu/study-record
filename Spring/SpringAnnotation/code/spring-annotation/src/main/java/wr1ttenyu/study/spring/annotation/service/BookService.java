package wr1ttenyu.study.spring.annotation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import wr1ttenyu.study.spring.annotation.dao.BookDao;

@Service
public class BookService {

    /*@Qualifier("bookDao2")*/
    @Autowired
    private BookDao bookDao;
    
    public void testAutowried() {
        System.out.println(bookDao.getLabel());
    }
}
