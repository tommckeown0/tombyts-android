import android.content.Context
import android.content.SharedPreferences

class SimpleAuthPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString("authToken", token)
            .apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("authToken", null)
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit()
            .putString("username", username)
            .apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun clearAuthData() {
        sharedPreferences.edit()
            .remove("authToken")
            .remove("username")
            .apply()
    }
}