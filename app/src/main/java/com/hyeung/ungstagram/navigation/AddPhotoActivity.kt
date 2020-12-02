package com.hyeung.ungstagram.navigation

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.hyeung.ungstagram.R
import com.hyeung.ungstagram.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage ? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    val REQUEST_IMAGE_CAPTURE = 1
    var currentPhotoPath : String ? =null
    var test : View ? = null
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // init
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 카메라 권한
        settingPermission()

        addphoto_btn_upload.setOnClickListener{
            contentUpload()
        }
        // 선택 팝업
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.activiy_dialog, null)

        val dialog = builder.setView(dialogView)
            .setNegativeButton("닫기") { dialogInterface, i ->
                finish()
            }
            .setOnCancelListener{dialog ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
        // 앨범열기
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"

        val camera_open = dialogView.findViewById<Button>(R.id.camera_open)
        val gallery_open = dialogView.findViewById<Button>(R.id.gallery_open)
        gallery_open.setOnClickListener{view->
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
            dialog.dismiss()
            add_photo_layout.visibility = View.VISIBLE
        }
        camera_open.setOnClickListener{
            startCapture()
            dialog.dismiss()
            add_photo_layout.visibility = View.VISIBLE
        }
    }

    fun startCapture(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try{
                    createImageFile()
                }catch(ex: IOException){
                    null
                }
                photoFile?.also{
                    val photoURI : Uri = FileProvider.getUriForFile(
                        this,
                        "org.techtown.capturepicture.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun createImageFile() : File {
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir : File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply{
            currentPhotoPath = absolutePath
        }
    }

    fun settingPermission(){
        var permis = object  : PermissionListener {
            //            어떠한 형식을 상속받는 익명 클래스의 객체를 생성하기 위해 다음과 같이 작성
            override fun onPermissionGranted() {
//                Toast.makeText(this@AddPhotoActivity, "권한 허가", Toast.LENGTH_SHORT)
//                    .show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@AddPhotoActivity, "권한 거부", Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.finishAffinity(this@AddPhotoActivity) // 권한 거부시 앱 종료
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permis)
            .setRationaleMessage("카메라 사진 권한 필요")
            .setDeniedMessage("카메라 권한 요청 거부")
            .setPermissions(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA)
            .check()
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
        }else if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            val file = File(currentPhotoPath)
            if (Build.VERSION.SDK_INT < 28) {
                val bitmap = MediaStore.Images.Media
                    .getBitmap(contentResolver, Uri.fromFile(file))
                addphoto_image.setImageBitmap(bitmap)
            }
            else{
                val decode = ImageDecoder.createSource(this.contentResolver,
                    Uri.fromFile(file))
                val bitmap = ImageDecoder.decodeBitmap(decode)
                photoUri = Uri.fromFile(file)
                addphoto_image.setImageBitmap(bitmap)
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
            contentDTO.explain = addphoto_edit_explain?.text.toString()
            // timestamp
            contentDTO.timestamp = System.currentTimeMillis()

             firestore?.collection("images")?.document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)

            finish()
        }
    }
}
