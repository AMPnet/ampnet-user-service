package com.ampnet.userservice.enums

enum class PrivilegeType {

    /*
    Def: <type>_privilege

    type:
        - PR - PERM_READ
        - PW - PERM_WRITE
        - PRO - PERM_READ_OWN
        - PWO - PER_WRITE_OWN
        - PRA - PERM_READ_ADMIN
        - PWA - PERM_WRITE_ADMIN
     */

    // Administration
    MONITORING,

    // Coop
    PWA_COOP,
    PRA_COOP,

    // Profile
    PRO_PROFILE,
    PRA_PROFILE,
    PWA_PROFILE,

    // Organization
    PRA_ORG,
    PWA_ORG_APPROVE,
    PRO_ORG_INVITE,
    PWO_ORG_INVITE,

    // Wallet
    PRA_WALLET,
    PWA_WALLET,
    PWA_WALLET_TRANSFER,

    // Withdraw
    PRA_WITHDRAW,
    PWA_WITHDRAW,

    // Deposit
    PRA_DEPOSIT,
    PWA_DEPOSIT
}
