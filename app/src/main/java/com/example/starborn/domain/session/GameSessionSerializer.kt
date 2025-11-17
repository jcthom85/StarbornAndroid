package com.example.starborn.domain.session

import androidx.datastore.core.Serializer
import com.example.starborn.datastore.GameSessionProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object GameSessionSerializer : Serializer<GameSessionProto> {
    override val defaultValue: GameSessionProto = GameSessionProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GameSessionProto =
        try {
            GameSessionProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            defaultValue
        }

    override suspend fun writeTo(t: GameSessionProto, output: OutputStream) {
        t.writeTo(output)
    }
}
