package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "app_user")
data class User(
    @Id
    @Column
    var uuid: UUID,

    @Column
    var email: String,

    @Column
    var password: String?,

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    var authMethod: AuthMethod,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_info_id")
    var userInfo: UserInfo,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var enabled: Boolean

) {
    fun getAuthorities(): Set<SimpleGrantedAuthority> {
        val roleAuthority = SimpleGrantedAuthority("ROLE_" + role.name)
        val privileges = UserRoleType.fromInt(role.id)
                ?.getPrivileges()
                ?.map { SimpleGrantedAuthority(it.name) }.orEmpty()
        return (privileges + roleAuthority).toSet()
    }

    fun getFullName(): String = "${userInfo.firstName} ${userInfo.lastName}"
}
