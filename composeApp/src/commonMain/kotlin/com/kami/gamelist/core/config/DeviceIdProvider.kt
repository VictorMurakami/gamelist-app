package com.kami.gamelist.core.config

import com.kami.gamelist.data.local.TextPrefDataSource
import kotlin.random.Random

class DeviceIdProvider(private val textPrefs: TextPrefDataSource) {

    companion object {
        private const val DEVICE_ID_KEY = "device_id"
        private const val BYTE_COUNT = 16
    }

    /**
     * UUID aleatorio gerado na primeira execucao e persistido.
     *
     * Nao e id de hardware: as duas plataformas restringem esses ids e eles
     * nao sobrevivem a reinstalacao de forma confiavel. Reinstalar o app gera
     * um id novo e pode mudar o bucket de rollout — aceitavel para rollout
     * gradual.
     */
    fun deviceId(): String {
        textPrefs.get(DEVICE_ID_KEY)?.let { return it }

        val generated = Random.nextBytes(BYTE_COUNT)
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

        textPrefs.set(DEVICE_ID_KEY, generated)
        return generated
    }
}
