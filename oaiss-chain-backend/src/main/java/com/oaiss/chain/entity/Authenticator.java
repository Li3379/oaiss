package com.oaiss.chain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 认证机构表
 * 存储认证机构信息
 * 
 * @author OAISS Team
 */
@Entity
@Table(name = "authenticator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Authenticator extends BaseEntity {

    /**
     * 关联用户ID
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 机构名称
     */
    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    /**
     * 机构编码
     */
    @Column(name = "org_code", nullable = false, unique = true, length = 50)
    private String orgCode;

    /**
     * 机构地址
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * 联系人
     */
    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    /**
     * 联系电话
     */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /**
     * 认证范围（JSON数组）
     */
    @Column(name = "cert_scope", columnDefinition = "TEXT")
    private String certScope;

    /**
     * 机构状态（0-禁用, 1-启用）
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 1;
}
