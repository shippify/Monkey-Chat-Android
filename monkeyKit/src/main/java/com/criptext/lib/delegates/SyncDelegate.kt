package com.criptext.lib.delegates

import com.criptext.http.HttpSync

/**
 * Created by gabriel on 2/13/17.
 */

interface SyncDelegate {
    /**
     * After the SyncDatabase() function of MonkeyKitSocketService finishes updating the database,
     * this callback is executed so that you can use that same data to update the UI.
     * guardarlos en la base de datos. La implementacion de este metodo debe de actualizar las conversaciones
     * @param data The data used to sync the database. With this callback your UI should reflect
     * the new state of your database.
     */
    fun onSyncComplete(data: HttpSync.SyncData)
}
