package com.study.querydsl.repository;

import com.study.querydsl.dto.MemberSearchCondition;
import com.study.querydsl.dto.MemberTeamDto;
import com.study.querydsl.entity.Member;
import com.study.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Optional<Member> findMember = memberJpaRepository.findById(member.getId());

        assertEquals(findMember.get(),member);
    }
    @Test
    public void basicQueryDslTest(){
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        List<Member> findMember = memberJpaRepository.findByUsername("member1");
        assertEquals(findMember.size(),1);

    }

    @Test
    public void searchTest(){
        MemberSearchCondition condition = conditionSetting();

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertEquals(result.size(),1);
        assertEquals(result.get(0).getUsername(),"member4");
    }
    @Test
    public void searchTest2(){
        MemberSearchCondition condition = conditionSetting();


        List<MemberTeamDto> result = memberJpaRepository.search(condition);
        assertEquals(result.size(),1);
        assertEquals(result.get(0).getUsername(),"member4");

    }

    private MemberSearchCondition conditionSetting() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        return condition;
    }
}