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
     *
     * A escrita e feita com INSERT OR IGNORE (`setIfAbsent`) seguida de
     * releitura, e nao com upsert incondicional: se duas chamadas
     * concorrentes (ex.: dois LaunchedEffect paralelos na startup) virem o
     * store vazio ao mesmo tempo, cada uma gera um valor diferente, mas so a
     * primeira a chegar no SQLite persiste — a releitura garante que ambas
     * as chamadas devolvam esse mesmo vencedor, em vez de cada uma devolver
     * o valor que gerou localmente (que poderia divergir do que foi
     * realmente salvo).
     */
    fun deviceId(): String {
        textPrefs.get(DEVICE_ID_KEY)?.let { return it }

        val generated = Random.nextBytes(BYTE_COUNT)
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

        textPrefs.setIfAbsent(DEVICE_ID_KEY, generated)
        return textPrefs.get(DEVICE_ID_KEY) ?: generated
    }
}
