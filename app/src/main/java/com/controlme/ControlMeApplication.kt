package com.controlme

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。Hilt 从这里注入依赖图。
 */
@HiltAndroidApp
class ControlMeApplication : Application()