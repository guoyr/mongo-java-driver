/*
 *
 *  * Copyright (c) 2008-2014 MongoDB, Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.mongodb.codecs

import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.ByteBuf
import org.bson.ByteBufNIO
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.io.BasicInputBuffer
import org.bson.io.BasicOutputBuffer
import spock.lang.Shared
import spock.lang.Specification

import java.nio.ByteBuffer

/**
 *
 */
class UUIDCodecSpecification extends Specification {

    @Shared private UUIDCodec uuidCodec;
    @Shared private BasicOutputBuffer outputBuffer;

    def setup() {
        uuidCodec = new UUIDCodec();
        outputBuffer = new BasicOutputBuffer();
    }

    def 'should decode long as little endian'() throws IOException {
        given:
        byte[] list = [0, 0, 0, 0,       //Start of document
                               5,                // type (BINARY)
                               95, 105, 100, 0,  // "_id"
                               16, 0, 0, 0,      // int "16" (length)
                               3,                // type (B_UUID_LEGACY)
                               2, 0, 0, 0, 0, 0, 0, 0,
                               1, 0, 0, 0, 0, 0, 0, 0]; //8 bytes for long, 2 longs for UUID, Little Endian
        BasicInputBuffer inputBuffer = new BasicInputBuffer(new ByteBufNIO(ByteBuffer.wrap(list)))
        BsonBinaryReader bsonReader = new BsonBinaryReader(inputBuffer, false)
        UUID expectedUuid = new UUID(2L, 1L)

        bsonReader.readStartDocument()
        bsonReader.readName()

        when:
        UUID actualUuid = uuidCodec.decode(bsonReader, DecoderContext.builder().build())

        then:
        expectedUuid == actualUuid

        cleanup:
        bsonReader.close()
    }

    def 'should encode long as little endian'() throws IOException {
        given:
        UUID uuid = new UUID(2L, 1L)
        BsonBinaryWriter bsonWriter = new BsonBinaryWriter(outputBuffer, false)
        bsonWriter.writeStartDocument()
        bsonWriter.writeName("_id")

        byte[] expectedList = [0, 0, 0, 0,       //Start of document
                               5,                // type (BINARY)
                               95, 105, 100, 0,  // "_id"
                               16, 0, 0, 0,      // int "16" (length)
                               3,                // type (B_UUID_LEGACY)
                               2, 0, 0, 0, 0, 0, 0, 0,
                               1, 0, 0, 0, 0, 0, 0, 0]; //8 bytes for long, 2 longs for UUID, Little Endian

        when:
        uuidCodec.encode(bsonWriter, uuid, EncoderContext.builder().build())

        then:
        outputBuffer.toByteArray() == expectedList

        cleanup:
        bsonWriter.close()
    }
}
