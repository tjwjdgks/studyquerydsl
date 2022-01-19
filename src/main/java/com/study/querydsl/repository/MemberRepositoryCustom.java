package com.study.querydsl.repository;

import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);
    // 현재는 fetchCount() 나 fetchResults 등이 없으므로 count 쿼리는 직접 만들어서 넣어야 한다
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    // countquery 최적화 PageableExecutionUtils가 제공
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
