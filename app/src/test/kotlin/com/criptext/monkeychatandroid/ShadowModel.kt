package com.criptext.monkeychatandroid

import com.activeandroid.Model
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Created by gesuwall on 1/11/17.
 */
@Implements(Model::class)
class ShadowModel {
    var saved = false
        private set

    @Implementation
    fun __constructor__() {

    }

    @Implementation
    fun save() {
        saved = true
    }

}

