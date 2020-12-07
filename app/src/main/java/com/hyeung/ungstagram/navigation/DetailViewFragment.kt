package com.hyeung.ungstagram.navigation

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hyeung.ungstagram.R
import com.hyeung.ungstagram.navigation.model.AlarmDTO
import com.hyeung.ungstagram.navigation.model.ContentDTO
import com.hyeung.ungstagram.navigation.util.FcmPush
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class  DetailViewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()
        init{
            firestore?.collection("images")?.whereEqualTo("delYn",false)?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    for(snapshot in querySnapshot!!.documents){
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }

        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        // 메모리 절약
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            // userId
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId
            // Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)
            // explain
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain
            // like count
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes "+contentDTOs!![position].favoriteCount
            // profile Image
            firestore?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task?.result!!["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image)
                    }
                }
            // 삭제버튼
            if(contentDTOs!![position].uid != uid){
                viewholder.iv_delete.visibility = View.INVISIBLE
            }
            viewholder.detailviewitem_favorite_imageview.setOnClickListener{
                favoriteEvent(position)
            }
            if(contentDTOs!![position].favorites.containsKey(uid)){
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }

            viewholder.detailviewitem_content_imageview.setOnClickListener { v->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)
            }

            viewholder.iv_delete.setOnClickListener{v->
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("삭제확인")
                    .setMessage("정말 삭제하시겠습니까?")
                    .setPositiveButton("삭제"){dialog: DialogInterface?, which: Int ->
//                        Toast.makeText(requireActivity(), "삭제", Toast.LENGTH_LONG).show()
                        deleteContent(contentUidList[position])
                    }
                    .setNegativeButton("취소") {dialog: DialogInterface?, which: Int ->
//                        Toast.makeText(requireActivity(), "취소", Toast.LENGTH_LONG).show()
                    }
                    .show()
            }
        }
        //좋아요 이벤트 기능
        fun favoriteEvent(position: Int){
//        Log.d("TAG","사이즈:  " +contentUidList.size);
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                Log.d("TAG","uid > "+uid)
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! -1
                    contentDTO?.favorites.remove(uid)
                }else{
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! +1
                    contentDTO?.favorites[uid!!] = true
                    favortieAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }?.addOnSuccessListener { result ->
                Log.d("TAG", "Transaction success: "+result)
            }?.addOnFailureListener { e ->
                Log.w("TAG", "Transaction failure. "+ e)
            }
        }
        fun favortieAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid,"Ungstagram",message)
        }
        // 삭제버튼
        fun deleteContent(delUid : String){
            var tsDoc = firestore?.collection("images")?.document(delUid)
            firestore?.runTransaction { transaction ->
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                    contentDTO?.delYn = true
                if (contentDTO != null) {
                    transaction.set(tsDoc, contentDTO)
//                    notifyDataSetChanged()
                }
            }?.addOnSuccessListener { result ->
                Toast.makeText(requireActivity(), "삭제 성공", Toast.LENGTH_LONG).show()
                Log.d("TAG", "Transaction success: "+result)
            }?.addOnFailureListener { e ->
                Toast.makeText(requireActivity(), "삭제 실패", Toast.LENGTH_LONG).show()
                Log.w("TAG", "Transaction failure. "+ e)
            }


        }

    }
}
