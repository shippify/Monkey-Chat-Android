package com.criptext.lib

import android.content.ServiceConnection

/**
 * Created by gesuwall on 3/8/17.
 */

abstract class CancellableServiceConnection: ServiceConnection {
    var cancelled = false
}
