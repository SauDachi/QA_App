package sato.daichi.techacademy.jp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference
    private var mFavFlag = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { }
        override fun onChildRemoved(dataSnapshot: DataSnapshot) { }
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) { }
        override fun onCancelled(databaseError: DatabaseError) { }
    }

    // ---------------追加---------------
    private val mFavoriteListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val questionUid = dataSnapshot.getValue()
            if (questionUid != null) {
                //ボタンの表示を変更
                favorite_button.text = "お気に入り解除"

                //mFavFlagをtrue
                mFavFlag = true
            }

        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { }
        override fun onChildRemoved(dataSnapshot: DataSnapshot) { }
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) { }
        override fun onCancelled(databaseError: DatabaseError) { }
    }
    // ---------------追加---------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras?.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // ---------------追加---------------

        //ログインユーザの情報を取得
        val user = FirebaseAuth.getInstance().currentUser

        //ログイン時のみお気に入りボタン表示
        if (user == null) {
            favorite_button.visibility = View.INVISIBLE
        } else {
            //お気に入りデータ確認
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val favoriteRef = dataBaseReference.child(FavoritePATH).child(user.uid).child(mQuestion.questionUid)
            favoriteRef.addChildEventListener(mFavoriteListener)
        }

        favorite_button.setOnClickListener() {
            // ログイン済みのユーザーを取得する
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            mFavoriteRef = dataBaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)

            if (mFavFlag) {
                //お気に入りを解除
                favorite_button.text = "お気に入り"
                favorite_button.setBackgroundColor(Color.LTGRAY)

                //favoritesから削除
                dataBaseReference.child(FavoritePATH).child(user.uid).child(mQuestion.questionUid).removeValue()

                //mFavFlagをfalse
                mFavFlag = false

            } else {
                //お気に入りに追加
                favorite_button.text = "お気に入り解除"

                val data = HashMap<String, String>()
                data["genre"] = mQuestion.genre.toString()
                mFavoriteRef.setValue(data)

                //mFavFlagをtrue
                mFavFlag = true
            }
        }

        // ---------------追加---------------

        fab.setOnClickListener {
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

    }
}
