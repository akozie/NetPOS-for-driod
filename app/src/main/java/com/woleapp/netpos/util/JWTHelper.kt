package com.woleapp.netpos.util

import com.auth0.android.jwt.JWT
import java.util.*

object JWTHelper {
    @JvmStatic
    fun getStormId(token: String?): String? {
        val jwt = JWT(token!!)
        return if (!jwt.claims.containsKey("stormId")) null else JWT(token).getClaim("stormId")
            .asString()
    }

    @JvmStatic
    fun isAgent(token: String?): Boolean? {
        val jwt = JWT(token!!)
        return if (!jwt.claims.containsKey("isAgent")) false else JWT(token).getClaim("isAgent")
            .asBoolean()
    }

    @JvmStatic
    fun isExpired(token: String?): Boolean {
        val jwt = JWT(token!!)
        return jwt.expiresAt!!.time < Date().time
    }

    @JvmStatic
    fun isAdmin(token: String?): Boolean {
        return JWT(token!!).getClaim("permissions")
            .asArray(String::class.java)[0].split(":")[1].equals("admin", true)
    }
}