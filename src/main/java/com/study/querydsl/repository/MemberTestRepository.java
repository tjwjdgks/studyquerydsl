package com.study.querydsl.repository;

import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.repository.support.Querydsl4RepositorySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.study.querydsl.entity.QMember.*;

public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository(Class<?> domainClass) {
        super(domainClass);
    }

    public List<Member> basicSelect(){
       return select(member).from(member).fetch();
    }
    public List<Member> basicSelectFrom(){
        return selectFrom(member).fetch();
    }
    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable){
        return applyPagination(pageable,query->(query.selectFrom(member)));
    }
    public Page<Member> applyPaginationWithCount(MemberSearchCondition condition, Pageable pageable){
        return applyPagination(pageable,
                query->(query.selectFrom(member)),
                countquery->(countquery.select(member.id).from(member))
        );
    }
}
