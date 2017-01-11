package com.criptext.monkeychatandroid

import com.activeandroid.Model
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Created by gesuwall on 1/11/17.
 */

@Implements(Model::class)
class ShadowModel {

    @Implementation
    fun __constructor__() {

    }

}