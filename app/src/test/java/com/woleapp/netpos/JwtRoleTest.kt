package com.woleapp.netpos

import com.woleapp.netpos.util.JWTHelper
import org.junit.Assert
import org.junit.Test

class JwtRoleTest {

    @Test
    fun testDecodeJwtToken(){
        val arr = JWTHelper.isAdmin("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImFkZWdib3lldGF5b0B5bWFpbC5jb20iLCJhcHBuYW1lIjoic3Rvcm1fYXBwIiwic3Rvcm1JZCI6ImI0MTZjNzdmLWE2OTctNDFmOS1iNmJkLWJhNDBiODA3NjdjNiIsInRlcm1pbmFsSWQiOiJUWTIzNDU2IiwiYnVzaW5lc3NOYW1lIjoiQ2FtZWxDYXNlIiwiaXNBZ2VudCI6dHJ1ZSwicGVybWlzc2lvbnMiOlsicm9sZTphZ2VudCJdLCJpYXQiOjE2MDEwMjAzNzUsImV4cCI6MTYwMTAzNDc3NSwiaXNzIjoic3Rvcm06YWNjb3VudHMiLCJzdWIiOiJhdXRoIn0.BjhWks10rR4oolSIet-ZCUTOPxSSfrDeP1idYRDsmgI")
        println(arr)
        Assert.assertEquals(false, arr)
    }
}