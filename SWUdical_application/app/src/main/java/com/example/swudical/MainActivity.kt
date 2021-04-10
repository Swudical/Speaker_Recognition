package com.example.swudical

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.swudical.DTO.UserInfoDTO
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    //facebook auth
    private var callbackManager: CallbackManager? = null
    //firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth
    //google client
    private lateinit var googleSignInClient: GoogleSignInClient

    //private const val TAG = "GoogleActivity"
    private val RC_SIGN_IN = 99
    private val TAG = "facebooklogin"

    val user = FirebaseAuth.getInstance()
    val uid = user.currentUser?.uid.toString()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 페이스북 로그인 버튼 설정
        // var -> val로 수정함 0306 20:48
        val facebookButton = findViewById<Button>(R.id.login_button)
        facebookButton.setOnClickListener { facebookLogin() }
        callbackManager = CallbackManager.Factory.create()

        // google_sign_btn.setOnClickListener (this) // 구글 로그인 버튼
        google_sign_btn.setOnClickListener {signIn()}
        //Google 로그인 옵션 구성. requestIdToken 및 Email 요청
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //firebase auth 객체
        firebaseAuth = FirebaseAuth.getInstance()
    }

    // onStart. 유저가 앱에 이미 구글 로그인을 했는지 확인
    public override fun onStart() {
        super.onStart()
        if(firebaseAuth.currentUser !=null) {
            startActivity(Intent(this, RecordsValiActivity::class.java))
            finish()
        }
    } //onStart End

    // onActivityResult
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                Log.w("로그인", "성공", task.exception)
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("MainActivity", "Google sign in failed", e)
            }
        }
    } // onActivityResult End

    // firebaseAuthWithGoogle
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("MainActivity", "firebaseAuthWithGoogle:" + acct.id!!)

        //Google SignInAccount 객체에서 ID 토큰을 가져와서 Firebase Auth로 교환하고 Firebase에 인증
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.w("MainActivity", "firebaseAuthWithGoogle 성공", task.exception)
                if(firebaseAuth.currentUser !=null) { // 현재 firebaseAuth로 로그인 한 사용자
                    val basicNotice = "\'EDIT\'을 눌러주세요!"
                    db.collection("user_info").document(uid).get()
                        .addOnSuccessListener { res->
                            val userInfoDTO = res.toObject(UserInfoDTO::class.java)
                            if(userInfoDTO?.name==basicNotice) { // 이름 필드가 비었을 때 = 처음 로그인 -> 개인 정보 입력
                                if (res.exists()) {
                                    startActivity(Intent(this, UserInfoActivity::class.java))
                                }
                            }else{ // 처음 로그인이 아닐 때 -> 진료기록 확인
                                startActivity(Intent(this, RecordsValiActivity::class.java))
                            }
                        }
                    finish()
                }
            } else {
                Log.w("MainActivity", "firebaseAuthWithGoogle 실패", task.exception)
                Snackbar.make(findViewById(R.id.google_sign_btn), "로그인에 실패하였습니다.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }// firebaseAuthWithGoogle END

    // facebookLogin start
    private fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, listOf("public_profile","email"))
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    handleFBToken(result?.accessToken)
                }
                override fun onCancel() {}
                override fun onError(error: FacebookException?) {}
            })
    }

    // token
    private fun handleFBToken(token : AccessToken?){
        val credential = FacebookAuthProvider.getCredential(token?.token!!)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if(firebaseAuth.currentUser !=null) { // 현재 firebaseAuth로 로그인 한 사용자
                    val basicNotice = "\'EDIT\'을 눌러주세요!"
                    db.collection("user_info").document(uid).get()
                        .addOnSuccessListener { res->
                            val userInfoDTO = res.toObject(UserInfoDTO::class.java)
                            if(userInfoDTO?.name==basicNotice) { // 이름 필드가 비었을 때 = 처음 로그인 -> 개인 정보 입력
                                if (res.exists()) {
                                    startActivity(Intent(this, UserInfoActivity::class.java))
                                }
                            }else{ // 처음 로그인이 아닐 때 -> 진료기록 확인
                                startActivity(Intent(this, RecordsValiActivity::class.java))
                            }
                        }
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }


    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}
