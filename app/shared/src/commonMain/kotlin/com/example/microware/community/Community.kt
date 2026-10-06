package com.example.microware.community

import com.example.microware.play.Experience

data class Community(
    val code: DurableId,
    val icon: Avatar,
    val memberBanner: Banner,
    val experiences: List<Experience>,
)
