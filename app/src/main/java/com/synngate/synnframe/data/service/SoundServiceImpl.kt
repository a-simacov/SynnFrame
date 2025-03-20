package com.synngate.synnframe.data.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.service.SoundService

class SoundServiceImpl(
    private val context: Context
) : SoundService {

    private val soundPool: SoundPool
    private val successSoundId: Int
    private val errorSoundId: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        successSoundId = soundPool.load(context, R.raw.success_beep, 1)
        errorSoundId = soundPool.load(context, R.raw.error_beep, 1)
    }

    override fun playSuccessSound() {
        soundPool.play(successSoundId, 1f, 1f, 1, 0, 1f)
    }

    override fun playErrorSound() {
        soundPool.play(errorSoundId, 1f, 1f, 1, 0, 1f)
    }
}