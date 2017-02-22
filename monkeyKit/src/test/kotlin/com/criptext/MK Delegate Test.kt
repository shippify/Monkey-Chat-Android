package com.criptext

import com.criptext.comunication.MessageTypes
import com.criptext.comunication.PushMessage
import com.criptext.http.HttpSync
import com.criptext.lib.BuildConfig
import com.criptext.security.ShadowAESInitializer
import com.criptext.security.ShadowAESUtil
import com.google.gson.JsonObject
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.util.ActivityController

/**
 * Created by gesuwall on 12/22/16.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, shadows=arrayOf(ShadowAESInitializer::class,
        ShadowAESUtil::class, ShadowMonkeyKitService::class), manifest = "TestManifest.xml")
class `MK Delegate Test` {
    lateinit var controller: ActivityController<TestDelegateActivity>
    lateinit var service: TestService

    @Before
    fun initActivity() {
        MonkeyKitSocketService.status = MonkeyKitSocketService.ServiceStatus.dead
        ShadowMonkeyKitService.cleanup()
        controller = Robolectric.buildActivity(TestDelegateActivity::class.java)
                .create().start().visible()
        initBoundService()
    }

    fun initBoundService() {
        val act = controller.get()
        service = TestService()
        val binder = service.onBind(null) as MonkeyKitSocketService.MonkeyBinder
        act.connectWithNewService(binder)
    }


    @Test
    fun `Should start service during startup and start socket connection`() {
        val act = controller.get()
        val intent = Shadows.shadowOf(act).peekNextStartedService()
        intent `should not be` null

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        act.isSocketConnected `should equal` true
        ShadowMonkeyKitService.isOnline `should equal` true
    }

    @Test
    fun `Should not stop service and disconnect after onStop`() {
        var act = controller.get()

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        act.isSocketConnected `should equal` true
        ShadowMonkeyKitService.isOnline `should equal` true

        act = controller.stop().get()

        act.isSocketConnected `should equal` true
        ShadowMonkeyKitService.isOnline `should equal` false
    }

    @Test
    fun `Should stop service after user leaving`() {
        var act = controller.get()

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        act.isSocketConnected `should equal` true
        ShadowMonkeyKitService.isOnline `should equal` true

        act = controller.userLeaving().stop().get()

        val shadowAct = Shadows.shadowOf(act)
        val stopServiceIntent = shadowAct.nextStoppedService

        stopServiceIntent `should not be` null
        ShadowMonkeyKitService.isOnline `should equal` false
    }

    @Test
    fun `Should not stop service after user leaving if keepServiceAlive is true`() {
        var act = controller.get()

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        act.isSocketConnected `should equal` true
        ShadowMonkeyKitService.isOnline `should equal` true

        act.keepAlive()
        act = controller.userLeaving().stop().get()

        val shadowAct = Shadows.shadowOf(act)
        val stopServiceIntent = shadowAct.nextStoppedService

        stopServiceIntent `should be` null
        ShadowMonkeyKitService.isOnline `should equal` true
    }

    @Test
    fun `Should send text messages to service and store them in local DB`() {
        var act = controller.get()

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        val newMsg = act.persistMessageAndSend("Test", "me", "you", JsonObject(), PushMessage("push"), true)

        val storedMsg = act.getStoredMessage(newMsg.message_id)
        storedMsg `should be` newMsg

        val sentMsg = ShadowMonkeyKitService.findSentMessage(newMsg.message_id)
        sentMsg `should be` newMsg
        act.hasMessagesToForwardToService `should equal` false

    }

    @Test
    fun `Should send file messages to service and store them in local DB`() {
        var act = controller.get()

        ShadowMonkeyKitService.simulateConnectionProcess(service, HttpSync.SyncData("my_id",
                HttpSync.SyncResponse(listOf(), listOf(), listOf())))

        val newMsg = act.persistFileMessageAndSend("Test/Path", "me", "you", MessageTypes.blMessagePhoto,
                JsonObject(), PushMessage("push"), true)

        val storedMsg = act.getStoredMessage(newMsg.message_id)
        storedMsg `should be` newMsg

        val sentMsg = ShadowMonkeyKitService.findSentMessage(newMsg.message_id)
        sentMsg `should be` newMsg
        act.hasMessagesToForwardToService `should equal` false

    }

    @Test
    fun `Should keep reference to sent messages until service becomes available`() {
        controller = Robolectric.buildActivity(TestDelegateActivity::class.java)
        var act = controller.create().start().get()

        act.isSocketConnected `should be` false

        val newTextMsg = act.persistMessageAndSend("Test", "me", "you", JsonObject(), PushMessage("push"), true)
        val newFileMsg = act.persistFileMessageAndSend("Test/Path", "me", "you", MessageTypes.blMessagePhoto,
                JsonObject(), PushMessage("push"), true)

        ShadowMonkeyKitService.findSentMessage(newTextMsg.message_id) `should be` null
        ShadowMonkeyKitService.findSentMessage(newFileMsg.message_id) `should be` null

        act.hasMessagesToForwardToService `should equal` true

        act = controller.stop().destroy().get()

        act.getStoredPendingMessage(newTextMsg.message_id) `should be` newTextMsg
        act.getStoredPendingMessage(newFileMsg.message_id) `should be` newFileMsg

        //simulate configuration change
        controller = Robolectric.buildActivity(TestDelegateActivity::class.java)
        act = controller.create().start().get()
        initBoundService()

        act.hasMessagesToForwardToService `should equal` false


    }
}