package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.Column
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "app_user")
@Suppress("LongParameterList")
class User(
    @Id
    @Column
    val uuid: UUID,

    @Column
    var firstName: String,

    @Column
    var lastName: String,

    @Column
    var email: String,

    @Column
    var password: String?,

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    var authMethod: AuthMethod,

    var userInfoId: Int?,

    @Column(name = "role_id", nullable = false)
    var role: UserRole,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    var enabled: Boolean

) {
    fun getAuthorities(): Set<SimpleGrantedAuthority> {
        val roleAuthority = SimpleGrantedAuthority("ROLE_" + role.name)
        val privileges = role.getPrivileges()
            .map { SimpleGrantedAuthority(it.name) }
        return (privileges + roleAuthority).toSet()
    }

    fun getFullName(): String = "$firstName $lastName"
}

@Converter(autoApply = true)
class UserRoleConverter : AttributeConverter<UserRole, Int> {

    override fun convertToDatabaseColumn(attribute: UserRole): Int =
        attribute.id

    override fun convertToEntityAttribute(dbData: Int): UserRole? =
        UserRole.fromInt(dbData)
}
