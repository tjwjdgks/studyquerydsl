package com.study.querydsl.repository.support;

import com.querydsl.jpa.JPQLQuery;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.List;

import static com.study.querydsl.entity.QMember.*;

// QuerydslSupport 으로 만든 repository
public class MemberQuerydslSupportRepository extends QuerydslRepositorySupport {

    public MemberQuerydslSupportRepository(Class<?> domainClass) {
        super(domainClass);
    }

    // querydsl 3 버전
    public List<Member> selectAll(){
        List<Member> fetch = from(member)
                .select(member)
                .fetch();
        return fetch;
    }
    public Page<Member> pageAll(Pageable pageable){
        JPQLQuery<Member> select = from(member)
                .select(member);
        JPQLQuery<Long> count = from(member)
                .select(member.count());
        List<Member> content = getQuerydsl().applyPagination(pageable, select).fetch();
        return PageableExecutionUtils.getPage(content,pageable,()->count.fetchOne());
    }
}
