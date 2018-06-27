package wr1ttenyu.study.spring.annotation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import wr1ttenyu.study.spring.annotation.dao.BookDao;

@Service
public class BookService {

    private static String staticField;

    /*@Qualifier("bookDao2")*/
    @Autowired
    private BookDao bookDao;

    public void testAutowried() {
        System.out.println(bookDao.getLabel());
    }

    public String getStaticField() {
        return staticField;
    }

    @Value("${test.static.field}")
    public void setStaticField(String staticField) {
        BookService.staticField = staticField;
    }
}
