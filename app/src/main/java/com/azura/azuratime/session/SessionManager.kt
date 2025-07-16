package com.azura.azuratime.session

import android.content.Context
import android.content.SharedPreferences
import com.azura.azuratime.db.UserEntity

class SessionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_ROLE = "role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val PREFS_NAME = "AuthPrefs"
        private const val KEY_UID = "USER_UID"
    }
    
    fun saveUserSession(user: UserEntity) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.username)
            putString(KEY_NAME, user.name)
            putString(KEY_ROLE, user.role)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }.apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    fun getRole(): String? {
        return sharedPreferences.getString(KEY_ROLE, null)
    }
    
    fun getName(): String? {
        return sharedPreferences.getString(KEY_NAME, null)
    }
    
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
    
    fun saveUid(uid: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UID, uid).apply()
    }

    fun getUid(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UID, null)
    }

    fun clearUid() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_UID).apply()
    }
}
