package com.criptext

import com.criptext.lib.BuildConfig
import com.criptext.security.ShadowAESInitializer
import com.criptext.security.ShadowAESUtil
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Created by gesuwall on 12/22/16.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowAESInitializer::class,
        ShadowAESUtil::class, ShadowMonkeyKitService::class))
class `MK Delegate Test` {
    @Test
    fun `Should start service during startup`() {
        var controller = Robolectric.buildActivity(TestDelegateActivity::class.java)
                .create().start().visible()
        var act = controller.get()
        val intent = Shadows.shadowOf(act).peekNextStartedService()
        intent `should not be` null
        var testService = TestService()
        val binder = testService.onBind(null) as MonkeyKitSocketService.MonkeyBinder
        act.connectWithNewService(binder)
    }
}