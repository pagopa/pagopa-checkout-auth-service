package test2.authservice.repositories.redis.bean.auth

/** Authenticated user session data domain object */
data class UserInfo(val name: Name, val surname: Name, val fiscalCode: UserFiscalCode)
