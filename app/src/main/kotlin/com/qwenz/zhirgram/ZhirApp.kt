package com.qwenz.zhirgram

import android.app.Application
import com.qwenz.zhirgram.data.SettingsRepository
import com.qwenz.zhirgram.td.TdRepository

/** Держит единственные экземпляры TDLib-репозитория и настроек. */
class ZhirApp : Application() {
    val td: TdRepository by lazy { TdRepository(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
