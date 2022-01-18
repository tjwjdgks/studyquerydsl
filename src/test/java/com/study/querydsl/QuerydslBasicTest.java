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

        // 기본 Q-type 활용용
        QMember m = member;

        // 동적 쿼리
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
        // query dsl 튜플이 나온다 // 여러개의 type이 있을 때 사용된다
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

        // query와 맞춰준다
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
      세타 조인
      회원의 이름과 팀 이름과 같은 회원 조인
     */

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // 모든 member와 team을 모두 조인 후 where 조건으로 찾는다. (theta 조인)
        // 외부 조인 불가능 left, right 조인 -> 조인 on을 사용하면 사용 가능
        List<Member> fetch = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /*
    on 절을 활용한 조인 기능
        1. 조인 대상 필터링
        2. 연관관계 없는 엔티티 외부 조인
     */

    @Test
    public void join_on_filtering(){

        List<Tuple> teamA = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")).fetch();

        teamA.forEach(t->System.out.println(t));
    }
    // 연관관계 없는 엔티티와 외부 조인
    // 일반 조인과 다르게 엔티티 하나만 들어간다
    @Test
    public void join_on_no_relation(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // 막 조인
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
        // 로딩된 엔티티인지 초기화 안된 엔티티인지 확인 해준다
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertFalse(loaded,"패치조인 미적용 ");
    }

    // fetch 조인은 엔티티 대상으로..
    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();


        Member findMember = queryFactory.selectFrom(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertTrue(loaded,"패치조인 미적용 ");
    }

    // 서브 쿼리 com.querydsl.jpa.JPAExpressions 사용
    /*
    // from 절의 서브 쿼리는 지원하지 않는다
            from 절의 서브쿼리 해결방안
                1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
                2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
                3. nativeSQL을 사용한다.
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
    // 나이가 평균 이상 회원
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
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0~20살")
                        .when(member.age.between(21,30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    // 상수가 필요할 때
    // 실제 jpql 상수 부분 안 나간다, 결과에서만 상수 받는다
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

    // 바깥계층에 dto로 변환해서 하는 것이 좋음
    // Tuple은 쿼리 dsl의 구현 기술중 하나이기 때문에 외부가 내부의 기술을 알면 좋지 않다다
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
        // getter setter 로 주입
        List<MemberDto> result = queryFactory.
                select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    @Test
    public void findDtoByQDSLField(){
        // 필드로 주입
        List<MemberDto> result = queryFactory.
                select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    @Test
    public void findDtoByQDSLConstructor(){
        // 필드로 주입
        List<MemberDto> result = queryFactory.
                select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }
    // field 의 별칭이 다를때, bean 방식, field 방식
    @Test
    public void findUserDtoByQDSLConstructor(){
        // 필드로 주입
        List<UserDto> result = queryFactory.
                select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    // ExpressionUtils 사용
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
    //  @QueryProjection 사용
    // 컴파일에 오류를 잡을 수 있음
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        fetch.forEach(System.out::println);
    }

   // 동적 쿼리1
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

    // 동적 쿼리 2
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);
        assertEquals(result.size(),1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        // where에 null이 들어가면 무시된다
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

    // 조합 가능
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // 기존의 BooleanExpression null로 반환시 생기는 문제를 개선한 코드 return BooleanBuilder
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


    // 한번에 update delete 하는 것 bulk 연산
    @Test
    public void bulkUpdate(){
        // 영향을 받은 row 수
        long count = queryFactory.update(member)
                .set(member.username, "비회원")
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
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",member.username))) // sql function 사용 기본 방법
                .where(member.username.eq(member.username.lower())) // querydsl에서 데이터베이스에서 공통적으로 제공하는 sql 함수를 제공
                .fetch();
        result.forEach(System.out::println);
    }

}
