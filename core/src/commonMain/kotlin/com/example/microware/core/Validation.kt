package com.example.microware.core

const val BannerWidth = 128
const val BannerHeight = 32

const val AvatarWidth = 32
const val AvatarHeight = 32

fun validateBanner(width: Int, height: Int) {
    require(width == BannerWidth && height == BannerHeight)
}

fun validateAvatar(width: Int, height: Int) {
    require(width == AvatarWidth && height == AvatarHeight)
}
