package com.example.starborn.core.platform

object AppLog {
    fun d(tag: String, message: String) {
        println("DEBUG [$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        System.err.println("ERROR [$tag] $message")
        throwable?.printStackTrace()
    }

    fun i(tag: String, message: String) {
        println("INFO [$tag] $message")
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        System.err.println("WARN [$tag] $message")
        throwable?.printStackTrace()
    }
}
