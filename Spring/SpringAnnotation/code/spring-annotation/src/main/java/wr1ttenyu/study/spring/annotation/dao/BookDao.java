package wr1ttenyu.study.spring.annotation.dao;

import org.springframework.stereotype.Repository;

@Repository
public class BookDao {

    private String label;

    public BookDao() {
        super();
    }

    public BookDao(String label) {
        super();
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
