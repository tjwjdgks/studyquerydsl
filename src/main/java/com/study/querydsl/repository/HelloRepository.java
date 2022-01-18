package com.study.querydsl.repository;

import com.study.querydsl.entity.Hello;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;

@Repository
@RequiredArgsConstructor
public class HelloRepository {

    private final EntityManager em;

    public void save(Hello hello){
        em.persist(hello);
    }
}
