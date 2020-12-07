package com.hyeung.ungstagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.hyeung.ungstagram.LoginActivity
import com.hyeung.ungstagram.MainActivity
import com.hyeung.ungstagram.R
import com.hyeung.ungstagram.navigation.model.AlarmDTO
import com.hyeung.ungstagram.navigation.model.ContentDTO
import com.hyeung.ungstagram.navigation.model.FollowDTO
import com.hyeung.ungstagram.navigation.util.FcmPush
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*
class  UserFragment : Fragment(){
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? =null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    var followListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            // my page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            // other user
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity.toolbar_btn_back.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainactivity?.toolbar_title_image?.visibility = View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener{
                requestFollow()
            }

        }

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!,3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)

        }
        getProfileImage()
        getFollower()
        getFollowing()
        return fragmentView
    }
    fun getFollowing() {
        followingListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView!!.account_tv_following_count.text = followDTO?.followingCount.toString()
        }
    }

    fun getFollower() {
        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()
            if (followDTO?.followers?.containsKey(currentUserUid)!!) {

                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                fragmentView?.account_btn_follow_signout
                    ?.background
                    ?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
            } else {

                if (uid != currentUserUid) {

                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                }
            }

        }

    }

//    fun getFollowerAndFollowing(){
//        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener{documentSnapshot, firebaseFirestoreException ->
//            if(documentSnapshot == null) return@addSnapshotListener
//            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
//            if(followDTO?.followingCount != null){
//                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
//            }
//            if(followDTO?.followerCount != null){
//                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
//
//                if(followDTO?.followers?.containsKey(currentUserUid!!)){
//                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
//                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
//                }else{
//                    if(uid != currentUserUid){
//                        fragmentView?.account_btn_follow_signout!!.text = getString(R.string.follow)
//                        fragmentView?.account_btn_follow_signout!!.background?.colorFilter = null
//                    }
//                }
//            }
//        }
//    }

    fun requestFollow() {
        // 상대 방 팔로우
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            if (followDTO?.followings.containsKey(uid)!!) {
                // 팔로잉 취소
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings?.remove(uid)
            } else {
                // 팔로우
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) { // 최초 팔로우
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (followDTO!!.followers.containsKey(currentUserUid!!)!!) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                // 팔로우
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)

            }
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
        }
        fun getProfileImage() {
            firestore?.collection("profileImages")?.document(uid!!)
                ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    if (documentSnapshot == null) return@addSnapshotListener
                    if (documentSnapshot.data != null) {
                        var url = documentSnapshot?.data!!["image"]
                        Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop())
                            .into(fragmentView?.account_iv_profile!!)
                    }
                }
        }

        fun followerAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = auth?.currentUser?.email
            alarmDTO.uid = auth?.currentUser?.uid
            alarmDTO.kind = 2
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_follow)
            FcmPush.instance.sendMessage(destinationUid,"Ungstagram",message)
        }

        inner class UserFragmentRecyclerViewAdapter :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

            init {
                firestore?.collection("images")?.whereEqualTo("uid", uid)?.whereEqualTo("delYn", false)?.orderBy("timestamp", Query.Direction.DESCENDING)
                    ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                        if (querySnapshot == null) return@addSnapshotListener

                        for (snapshot in querySnapshot.documents) {
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                        }
                        fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                var width = resources.displayMetrics.widthPixels / 3
                var imageView = ImageView(parent.context)
                imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
                return CustomViewHolder(imageView)

            }

            inner class CustomViewHolder(var imageView: ImageView) :
                RecyclerView.ViewHolder(imageView) {

            }

            override fun getItemCount(): Int {
                return contentDTOs.size
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                var imageView = (holder as CustomViewHolder).imageView
                Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                    .apply(RequestOptions().centerCrop()).into(imageView)
            }


        }
    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followingListenerRegistration?.remove()
    }
}
