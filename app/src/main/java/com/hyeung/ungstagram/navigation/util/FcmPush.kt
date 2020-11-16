package com.hyeung.ungstagram.navigation.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.hyeung.ungstagram.navigation.model.PushDTO
import okhttp3.*
import java.io.IOException

class FcmPush (){
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAGGEfnvc:APA91bHGl1ByL2YHxx8JhmsaGU3TKnLJ6NuduB4KI_1_FxRPuyzXBPwerMHKVrgtZTlW4fhDuEPwTonG4xTLIf8HDjwGovNGKCeE3eekCGBvt--oUOyUxtalxQcdNXU0issoQsEW5Wic"
    var gson : Gson ? = null
    var okHttpClient : OkHttpClient ? = null
    companion object{
        var instance = FcmPush()
    }
    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(detinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(detinationUid).get().addOnCompleteListener { task->
            if(task.isSuccessful){
                var token = task?.result?.get("pushtoken").toString()

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message
                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization","key="+serverKey)
                    .url(url)
                    .post(body)
                    .build()
                okHttpClient?.newCall(request)?.enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {

                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }
}