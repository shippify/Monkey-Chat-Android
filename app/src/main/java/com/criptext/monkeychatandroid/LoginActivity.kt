package com.criptext.monkeychatandroid

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.criptext.ClientData
import com.criptext.lib.MonkeyInit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LoginActivity :  AppCompatActivity(), View.OnClickListener {

    private var editTextEmail: EditText? = null
    private var editTextPassword:EditText? = null
    private var btnLogin: Button? = null
    private var progressBar: ProgressBar? = null
    private var txtRespose: TextView? = null
    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextEmail= findViewById<EditText>(R.id.editTextUsername)
        editTextEmail?.setText("kevincamp_90@me.com")
        editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        editTextPassword?.setText("kevin")
        btnLogin = findViewById<Button>(R.id.loginBtn)
        progressBar = findViewById<ProgressBar>(R.id.progressBarCargando)
        btnLogin!!.setOnClickListener(this)

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onClick(v: View) {
//        v.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
        btnLogin?.isClickable = false

        val userCredentials = JSONObject()
        val url = "http://staging.shippify.co:8030/users/login"

        try {
            userCredentials.put("email", editTextEmail?.text)
            userCredentials.put("password", editTextPassword?.text)

            val que = Volley.newRequestQueue(this)
            val req = JsonObjectRequest(Request.Method.POST, url, userCredentials,
                    Response.Listener { response ->

                        var user = response.getJSONObject("payload")
                                .getJSONObject("data")
                                .getString("user")
                        val parser: Parser = Parser()
                        val stringBuilder: StringBuilder = StringBuilder(user)
                        val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                        var name = json.string("name")
                        var monkeyId = json.string("monkeyId")
                        if (monkeyId!!.isEmpty())
                            monkeyId = null

                        val userInfo = JSONObject()
                        val ignore_params = JSONArray()

                        try {
                            userInfo.put("name", name)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                        val mStart = object : MonkeyInit(this@LoginActivity, monkeyId,
                                SensitiveData.APP_ID, SensitiveData.APP_KEY, userInfo, ignore_params) {
                            override fun onSessionOK(sessionID: String, sdomain: String, sport: Int) {
                                val editor = prefs!!.edit()
                                editor.putBoolean(MonkeyChat.IS_REGISTERED, true)
                                editor.putString(MonkeyChat.MONKEY_ID, sessionID)
                                editor.putString(MonkeyChat.FULLNAME, name)
                                //                editor.putString(MonkeyChat.FULLNAME, editTextName.getText().toString());
                                editor.putString(MonkeyChat.SOCKET_DOMAIN, sdomain)
                                editor.putInt(MonkeyChat.SOCKET_PORT, sport)
                                editor.apply()
                                val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)
                                val data = ClientData(name!!, SensitiveData.APP_ID, SensitiveData.APP_KEY,
                                        sessionID, sdomain, sport)
                                data.fillIntent(mainIntent)

                                btnLogin?.isClickable = true
                                progressBar?.visibility = View.GONE

                                startActivity(mainIntent)
                                finish()
                            }

                            override fun onSessionError(exceptionName: String) {

//                                v.setVisibility(View.VISIBLE)
                                btnLogin?.isClickable = true
                                progressBar?.setVisibility(View.GONE)

                                val alert = AlertDialog.Builder(this@LoginActivity)
                                alert.setMessage(exceptionName)
                                alert.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }
                                alert.show()
                            }
                        }
                        mStart.register()
                    }, Response.ErrorListener {
                error ->
                //                            v.setVisibility(View.VISIBLE)
                btnLogin?.isClickable = true
                progressBar?.setVisibility(View.GONE)
                onSessionError(error)
            })


            que.add(req)
        } catch(e: JSONException) {
            e.printStackTrace()
        }

    }

    fun onSessionError (e : VolleyError) {
        e.printStackTrace()
        Toast.makeText(this, "There was an error. Please try again", Toast.LENGTH_LONG).show()
    }

}