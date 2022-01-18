package com.study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import com.study.querydsl.dto.QMemberTeamDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.study.querydsl.entity.QMember.*;
import static com.study.querydsl.entity.QTeam.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member){
        em.persist(member);
    }
    public Optional<Member> findById(Long id){
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll_Querydsl(){
        return  queryFactory.selectFrom(member).fetch();
    }
    public List<Member> findByUsername(String username){
        return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if(StringUtils.hasText(condition.getTeamName())){
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe()!=null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }
    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername())
                                .and(teamNameEq(condition.getTeamName()))
                                .and(ageGOE(condition.getAgeGoe()))
                                .and(ageLOE(condition.getAgeLoe()))
                )
                .fetch();
    }

    private BooleanBuilder ageLOE(Integer ageLoe) {
        return nullSafeBuilder(()->member.age.loe(ageLoe));
    }

    private BooleanBuilder ageGOE(Integer ageGoe) {
        return nullSafeBuilder(()->member.age.goe(ageGoe));
    }

    private BooleanBuilder teamNameEq(String teamName) {
        if(!StringUtils.hasText(teamName))
            return new BooleanBuilder();
        return nullSafeBuilder(()->team.name.eq(teamName));
    }


    private BooleanBuilder usernameEq(String username) {
        if(!StringUtils.hasText(username))
            return new BooleanBuilder();
        return nullSafeBuilder(()->member.username.eq(username));
    }

    public static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> supplier){
        try{
           return new BooleanBuilder(supplier.get());
        }catch(IllegalArgumentException e){
            return new BooleanBuilder();
        }catch (NullPointerException e){
            return new BooleanBuilder();
        }
    }
}
