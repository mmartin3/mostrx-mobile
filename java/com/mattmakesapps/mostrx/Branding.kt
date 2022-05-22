package com.mattmakesapps.mostrx

enum class Branding {
    GENERIC {
        override fun toString() = "G"
    },
    BRAND {
        override fun toString() = "B"
    }
}
