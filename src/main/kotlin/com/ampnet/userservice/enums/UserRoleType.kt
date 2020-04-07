package com.ampnet.userservice.enums

@Suppress("MagicNumber")
enum class UserRoleType(val id: Int) {

    ADMIN(1) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                PrivilegeType.MONITORING,
                PrivilegeType.PRA_PROFILE,
                PrivilegeType.PWA_PROFILE,
                PrivilegeType.PRO_PROFILE,
                PrivilegeType.PRA_ORG,
                PrivilegeType.PWA_ORG_APPROVE,
                PrivilegeType.PRO_ORG_INVITE,
                PrivilegeType.PWO_ORG_INVITE,
                PrivilegeType.PRA_WALLET,
                PrivilegeType.PWA_WALLET,
                PrivilegeType.PWA_WALLET_TRANSFER,
                PrivilegeType.PRA_WITHDRAW,
                PrivilegeType.PWA_WITHDRAW,
                PrivilegeType.PRA_DEPOSIT,
                PrivilegeType.PWA_DEPOSIT
            )
        }
    },

    USER(2) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                PrivilegeType.PRO_PROFILE,
                PrivilegeType.PRO_ORG_INVITE,
                PrivilegeType.PWO_ORG_INVITE
            )
        }
    },

    TOKEN_ISSUER(3) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                PrivilegeType.PRA_PROFILE,
                PrivilegeType.PRO_PROFILE,
                PrivilegeType.PRA_ORG,
                PrivilegeType.PRA_WITHDRAW,
                PrivilegeType.PWA_WITHDRAW,
                PrivilegeType.PRA_DEPOSIT,
                PrivilegeType.PWA_DEPOSIT,
                PrivilegeType.PRA_WALLET,
                PrivilegeType.PWA_WALLET_TRANSFER
            )
        }
    },

    PLATFORM_MANAGER(4) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                PrivilegeType.MONITORING,
                PrivilegeType.PRA_PROFILE,
                PrivilegeType.PWA_PROFILE,
                PrivilegeType.PRO_PROFILE,
                PrivilegeType.PRA_ORG,
                PrivilegeType.PWA_ORG_APPROVE,
                PrivilegeType.PRO_ORG_INVITE,
                PrivilegeType.PWO_ORG_INVITE,
                PrivilegeType.PRA_WALLET,
                PrivilegeType.PWA_WALLET,
                PrivilegeType.PWA_WALLET_TRANSFER,
                PrivilegeType.PRA_WITHDRAW,
                PrivilegeType.PRA_DEPOSIT
            )
        }
    };

    companion object {
        private val map = values().associateBy(UserRoleType::id)
        fun fromInt(type: Int) = map[type]
    }

    abstract fun getPrivileges(): List<PrivilegeType>
}
