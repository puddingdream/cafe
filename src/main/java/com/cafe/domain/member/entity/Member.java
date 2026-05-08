package com.cafe.domain.member.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.domain.member.enums.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Table(name = "members")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@SQLDelete(sql = "UPDATE members SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(nullable = false, unique = true)
    private Long pointWalletId;
}
