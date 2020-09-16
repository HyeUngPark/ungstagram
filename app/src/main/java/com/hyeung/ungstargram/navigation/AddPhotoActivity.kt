package com.hyeung.ungstargram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.hyeung.ungstargram.R
import com.hyeung.ungstargram.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage ? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // init
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // open album
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // upload event
        addphoto_btn_upload.setOnClickListener{
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                // image path
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            }else{
                // cancel
                finish()
            }
        }
    }
    fun contentUpload(){
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format((Date()))
        var imageFileName = "IMAGE"+timestamp+"_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //promise
        storageRef?.putFile(photoUri!!)?.continueWithTask { task: com.google.android.gms.tasks.Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri->
            var contentDTO = ContentDTO()

            // image
            contentDTO.imageUrl = uri.toString()
            // uid
            contentDTO.uid = auth?.currentUser?.uid
            // userId
            contentDTO.userId = auth?.currentUser?.email
            // explain content
            contentDTO.explain = addphoto_edit_explain.text.toString()
            // timestamp
            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)

            finish()
            }

        //callback
        /*
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
            storageRef.downloadUrl.addOnSuccessListener { uri->
                var contentDTO = ContentDTO()

                // image
                contentDTO.imageUrl = uri.toString()
                // uid
                contentDTO.uid = auth?.currentUser?.uid
                // userId
                contentDTO.userId = auth?.currentUser?.email
                // explain content
                contentDTO.explain = addphoto_edit_explain.text.toString()
                // timestamp
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)

                finish()
            }
        }

         */
    }
}
