package com.cafe.domain.member.entity;

import com.cafe.common.entity.BaseEntity;
import com.cafe.domain.member.enums.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "point_wallet_id", unique = true)
    private Long pointWalletId;

    @Builder
    private Member(String email, String password, String name, String phoneNumber, MemberRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role == null ? MemberRole.USER : role;
    }

    public void linkPointWallet(Long pointWalletId) {
        this.pointWalletId = pointWalletId;
    }

    public void changeRole(MemberRole role) {
        this.role = role;
    }
}
