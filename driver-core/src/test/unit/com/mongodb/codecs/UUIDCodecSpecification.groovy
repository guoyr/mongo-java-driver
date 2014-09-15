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
import org.bson.ByteBufNIO
import org.bson.UuidRepresentation
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.RootCodecRegistry
import org.bson.io.BasicInputBuffer
import org.bson.io.BasicOutputBuffer
import spock.lang.Shared
import spock.lang.Specification

import java.nio.ByteBuffer

/**
 *
 */
class UUIDCodecSpecification extends Specification {

    @Shared private BasicOutputBuffer outputBuffer;

    def setup() {
        outputBuffer = new BasicOutputBuffer();
    }

    def 'should decode different types of UUID'(UUIDCodec codec, byte[] list) throws IOException {

        given:

        BasicInputBuffer inputBuffer = new BasicInputBuffer(new ByteBufNIO(ByteBuffer.wrap(list)))
        BsonBinaryReader bsonReader = new BsonBinaryReader(inputBuffer, false)
        UUID expectedUuid = UUID.fromString('08070605-0403-0201-100f-0e0d0c0b0a09')

        bsonReader.readStartDocument()
        bsonReader.readName()

        when:
        UUID actualUuid = codec.decode(bsonReader, DecoderContext.builder().build())

        then:
        expectedUuid == actualUuid

        cleanup:
        bsonReader.close()

        where:

        codec << [
                new UUIDCodec(UuidRepresentation.JAVA_LEGACY, UuidRepresentation.JAVA_LEGACY),
                new UUIDCodec(UuidRepresentation.JAVA_LEGACY, UuidRepresentation.STANDARD),
                new UUIDCodec(UuidRepresentation.JAVA_LEGACY, UuidRepresentation.PYTHON_LEGACY),
                new UUIDCodec(UuidRepresentation.JAVA_LEGACY, UuidRepresentation.C_SHARP_LEGACY),
        ]

        list << [
                [0, 0, 0, 0,       //Start of document
                 5,                // type (BINARY)
                 95, 105, 100, 0,  // "_id"
                 16, 0, 0, 0,      // int "16" (length)
                 3,                // type (B_UUID_LEGACY) JAVA_LEGACY
                 1, 2, 3, 4, 5, 6, 7, 8,
                 9, 10, 11, 12, 13, 14, 15, 16], //8 bytes for long, 2 longs for UUID, Little Endian

                [0, 0, 0, 0,       //Start of document
                 5,                // type (BINARY)
                 95, 105, 100, 0,  // "_id"
                 16, 0, 0, 0,      // int "16" (length)
                 4,                // type (UUID)
                 8, 7, 6, 5, 4, 3, 2, 1,
                 16, 15, 14, 13, 12, 11, 10, 9], //8 bytes for long, 2 longs for UUID, Big Endian

                [0, 0, 0, 0,       //Start of document
                 5,                // type (BINARY)
                 95, 105, 100, 0,  // "_id"
                 16, 0, 0, 0,      // int "16" (length)
                 3,                // type (B_UUID_LEGACY) PYTHON_LEGACY
                 8, 7, 6, 5, 4, 3, 2, 1,
                 16, 15, 14, 13, 12, 11, 10, 9], //8 bytes for long, 2 longs for UUID, Big Endian

                [0, 0, 0, 0,       //Start of document
                 5,                // type (BINARY)
                 95, 105, 100, 0,  // "_id"
                 16, 0, 0, 0,      // int "16" (length)
                 3,                // type (B_UUID_LEGACY) CSHARP_LEGACY
                 5, 6, 7, 8, 3, 4, 1, 2,
                 16, 15, 14, 13, 12, 11, 10, 9], //8 bytes for long, 2 longs for UUID, Big Endian
        ]

    }

    def 'should encode different types of UUIDs'(Byte bsonSubType,
                                                 UUIDCodec codec,
                                                 UUID uuid) throws IOException {
        given:

        byte[] encodedDoc = [0, 0, 0, 0,       //Start of document
                             5,                // type (BINARY)
                             95, 105, 100, 0,  // "_id"
                             16, 0, 0, 0,      // int "16" (length)
                             0,                // bsonSubType
                             1, 2, 3, 4, 5, 6, 7, 8,
                             9, 10, 11, 12, 13, 14, 15, 16] //8 bytes for long, 2 longs for UUID

        encodedDoc[13] = bsonSubType

        BsonBinaryWriter bsonWriter = new BsonBinaryWriter(outputBuffer, false)
        bsonWriter.writeStartDocument()
        bsonWriter.writeName('_id')

        when:
        codec.encode(bsonWriter, uuid, EncoderContext.builder().build())

        then:
        outputBuffer.toByteArray() == encodedDoc

        cleanup:
        bsonWriter.close()

        where:

        bsonSubType << [3, 4, 3, 3]

        codec << [
                new UUIDCodec(UuidRepresentation.JAVA_LEGACY, UuidRepresentation.JAVA_LEGACY),
                new UUIDCodec(UuidRepresentation.STANDARD, UuidRepresentation.JAVA_LEGACY),
                new UUIDCodec(UuidRepresentation.PYTHON_LEGACY, UuidRepresentation.JAVA_LEGACY),
                new UUIDCodec(UuidRepresentation.C_SHARP_LEGACY, UuidRepresentation.JAVA_LEGACY),
        ]

        uuid << [
                UUID.fromString('08070605-0403-0201-100f-0e0d0c0b0a09'), // Java legacy UUID
                UUID.fromString('01020304-0506-0708-090a-0b0c0d0e0f10'), // simulated standard UUID
                UUID.fromString('01020304-0506-0708-090a-0b0c0d0e0f10'), // simulated Python UUID
                UUID.fromString('04030201-0605-0807-090a-0b0c0d0e0f10') // simulated C# UUID
        ]
    }

    def 'should get the codec with the correct representation from the registry'() {
        given:
        RootCodecRegistry registry = new RootCodecRegistry(
                [new UUIDCodecProvider(UuidRepresentation.STANDARD, UuidRepresentation.STANDARD)])
        Codec<UUID> codec = registry.get(UUID)

        BsonBinaryWriter bsonWriter = new BsonBinaryWriter(outputBuffer, false)
        bsonWriter.writeStartDocument()
        bsonWriter.writeName('_id')

        byte[] encodedDoc = [0, 0, 0, 0,       //Start of document
                             5,                // type (BINARY)
                             95, 105, 100, 0,  // "_id"
                             16, 0, 0, 0,      // int "16" (length)
                             4,                // bsonSubType
                             1, 2, 3, 4, 5, 6, 7, 8,
                             9, 10, 11, 12, 13, 14, 15, 16] //8 bytes for long, 2 longs for UUID

        def uuid = UUID.fromString('01020304-0506-0708-090a-0b0c0d0e0f10')

        when:
        codec.encode(bsonWriter, uuid, EncoderContext.builder().build())

        then:
        bsonWriter.getBuffer().toByteArray() == encodedDoc

    }
}
