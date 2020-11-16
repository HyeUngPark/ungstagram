package com.hyeung.ungstagram.navigation.model

data class AlarmDTO(
    var destinationUid : String ? = null,
    var userId : String ? = null,
    var uid : String ?  = null,
    var kind : Int ? =null, // 좋아요:0 댓글:1 팔로우:2
    var message : String ? = null,
    var timestamp : Long ? = null
)