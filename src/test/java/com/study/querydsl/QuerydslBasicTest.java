package com.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.study.querydsl.dto.MemberDto;
import com.study.querydsl.dto.QMemberDto;
import com.study.querydsl.dto.UserDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.QMember;
import com.study.querydsl.entity.QTeam;
import com.study.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.criteria.CriteriaBuilder;

import java.util.List;
import java.util.function.Supplier;

import static com.study.querydsl.entity.QMember.*;
import static com.study.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;
    @BeforeEach
    public void before(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        queryFactory = new JPAQueryFactory(em);
    }

    @Test
    public void startJPQL(){

        Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1").getSingleResult();

        assertEquals(findByJPQL.getUsername(),"member1");
    }

    @Test
    public void startQuerydsl(){


        //QMember m = new QMember("m");

        // ?????? Q-type ?????????
        QMember m = member;

        // ?????? ??????
        Member member = queryFactory
                .select(m)
                .from(m).
                where(
                        m.username.eq("member1"),
                        m.age.eq(10)
                )
                .fetchOne();

        assertEquals(member.getUsername(),"member1");

    }

    @Test
    public void resultFetch(){


        List<Member> fetch = queryFactory.selectFrom(member).fetch();

        Member member = queryFactory.selectFrom(QMember.member).where(QMember.member.username.eq("member1")).fetchOne();

        Member fetchFirst = queryFactory.selectFrom(QMember.member).fetchFirst();
    }

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member).where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertEquals(result.size(),3);

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member membernull = result.get(2);

        assertEquals(member5.getUsername(),"member5");
        assertEquals(member6.getUsername(),"member6");
        assertNull(membernull.getUsername());
    }

    @Test
    public void paging1(){
        List<Member> fetch = queryFactory.
                selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertEquals(fetch.size(), 2);
    }
    @Test
    public void aggregation(){
        // query dsl ????????? ????????? // ???????????? type??? ?????? ??? ????????????
        List<Tuple> fetch = queryFactory.select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
        ).from(member).fetch();

        Tuple tuple = fetch.get(0);
        assertEquals(tuple.get(member.count()),4);
        assertEquals(tuple.get(member.age.sum()),100);

    }

    @Test
    public void group(){
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // query??? ????????????
        assertEquals(teamA.get(team.name),"teamA");
        assertEquals(teamA.get(member.age.avg()),15);

        assertEquals(teamB.get(team.name),"teamB");
        assertEquals(teamB.get(member.age.avg()),35);

    }

    @Test
    public void join(){
        List<Member> teamA = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(teamA)
                .extracting("username")
                .containsExactly("member1","member2");
    }



    /*
      ?????? ??????
      ????????? ????????? ??? ????????? ?????? ?????? ??????
     */

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // ?????? member??? team??? ?????? ?????? ??? where ???????????? ?????????. (theta ??????)
        // ?????? ?????? ????????? left, right ?????? -> ?????? on??? ???????????? ?????? ??????
        List<Member> fetch = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /*
    on ?????? ????????? ?????? ??????
        1. ?????? ?????? ?????????
        2. ???????????? ?????? ????????? ?????? ??????
     */

    @Test
    public void join_on_filtering(){

        List<Tuple> teamA = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")).fetch();

        teamA.forEach(t->System.out.println(t));
    }
    // ???????????? ?????? ???????????? ?????? ??????
    // ?????? ????????? ????????? ????????? ????????? ????????????
    @Test
    public void join_on_no_relation(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // ??? ??????
        List<Tuple> fetch = queryFactory.select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        fetch.forEach(t->System.out.println(t));
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // ????????? ??????????????? ????????? ?????? ??????????????? ?????? ?????????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertFalse(loaded,"???????????? ????????? ");
    }

    // fetch ????????? ????????? ????????????..
    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();


        Member findMember = queryFactory.selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertTrue(loaded,"???????????? ????????? ");
    }

    // ?????? ?????? com.querydsl.jpa.JPAExpressions ??????
    /*
    // from ?????? ?????? ????????? ???????????? ?????????
            from ?????? ???????????? ????????????
                1. ??????????????? join?????? ????????????. (????????? ????????? ??????, ???????????? ????????? ??????.)
                2. ???????????????????????? ????????? 2??? ???????????? ????????????.
                3. nativeSQL??? ????????????.
     */
    @Test
    public void subQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);

    }
    // ????????? ?????? ?????? ??????
    // goe  : greater or equal
    @Test
    public void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg()).from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30,40);

    }
    // gt greater
    @Test
    public void subQueryIn(){

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.select(memberSub.age).from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20,30,40);

    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = queryFactory.
                select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        fetch.forEach(i->System.out.println(i));
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0~20???")
                        .when(member.age.between(21,30)).then("21~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    // ????????? ????????? ???
    // ?????? jpql ?????? ?????? ??? ?????????, ??????????????? ?????? ?????????
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        result.forEach(i->System.out.println(i));
    }

    @Test
    public void concat(){
        // username_age
        List<Tuple> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()),member.age)
                .from(member)
                .where(member.username.eq("member2"))
                .fetch();

        fetch.forEach(System.out::println);
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();
        result.forEach(System.out::println);
    }

    // ??????????????? dto??? ???????????? ?????? ?????? ??????
    // Tuple??? ?????? dsl??? ?????? ????????? ???????????? ????????? ????????? ????????? ????????? ?????? ?????? ?????????
   @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory.select(member.username,member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println(username);
            System.out.println(age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new com.study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class).getResultList();

        resultList.forEach(System.out::println);
    }

    @Test
    public void findDtoByQDSLSetter(){
        // getter setter ??? ??????
        List<MemberDto> result = queryFactory.
                select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    @Test
    public void findDtoByQDSLField(){
        // ????????? ??????
        List<MemberDto> result = queryFactory.
                select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    @Test
    public void findDtoByQDSLConstructor(){
        // ????????? ??????
        List<MemberDto> result = queryFactory.
                select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    // field ??? ????????? ?????????, bean ??????, field ??????
    @Test
    public void findUserDtoByQDSLConstructor(){
        // ????????? ??????
        List<UserDto> result = queryFactory.
                select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    // ExpressionUtils ??????
    @Test
    public void findUserDtoExpressionByQDSLConstructor(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory.
                select(Projections.fields(
                        UserDto.class, member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max()).from(memberSub),"age")
                ))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    //  @QueryProjection ??????
    // ???????????? ????????? ?????? ??? ??????
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        fetch.forEach(System.out::println);
    }

   // ?????? ??????1
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);
        assertEquals(result.size(),1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

         return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    // ?????? ?????? 2
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);
        assertEquals(result.size(),1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        // where??? null??? ???????????? ????????????
        return queryFactory.selectFrom(member)
               .where(usernameEq(usernameCond),ageEq(ageCond))
               .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    // ?????? ??????
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // ????????? BooleanExpression null??? ????????? ????????? ????????? ????????? ?????? return BooleanBuilder
    private BooleanBuilder ageBuilderEq(Integer age) {
        return nullSafeBuilder(() -> member.age.eq(age));
    }
    private BooleanBuilder usernameBuilderEq(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }
    private BooleanBuilder allBuilderEq(Integer age, String username){
        return ageBuilderEq(age).and(usernameBuilderEq(username));
    }
    private BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f){
        try {
            return new BooleanBuilder(f.get());
        }catch (IllegalArgumentException e){
            return new BooleanBuilder();
        }
    }


    // ????????? update delete ?????? ??? bulk ??????
    @Test
    public void bulkUpdate(){
        // ????????? ?????? row ???
        long count = queryFactory.update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();
    }
    // add multiply
    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();
    }

    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(31))
                .execute();
    }

    @Test
    public void sqlFuntion(){
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        result.forEach(System.out::println);
    }
    @Test
    public void sqlFuntion2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",member.username))) // sql function ?????? ?????? ??????
                .where(member.username.eq(member.username.lower())) // querydsl?????? ???????????????????????? ??????????????? ???????????? sql ????????? ??????
                .fetch();
        result.forEach(System.out::println);
    }

}
