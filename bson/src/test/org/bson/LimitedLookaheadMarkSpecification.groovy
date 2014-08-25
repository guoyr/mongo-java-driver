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

package org.bson

import org.bson.io.BasicInputBuffer
import org.bson.io.BasicOutputBuffer
import org.bson.json.JsonReader
import org.bson.json.JsonWriter
import spock.lang.Shared
import spock.lang.Specification

import java.nio.ByteBuffer

@SuppressWarnings('UnnecessaryObjectReferences')
class LimitedLookaheadMarkSpecification extends Specification {

    @Shared BsonDocument bsonDoc
    @Shared BasicOutputBuffer buffer
    @Shared StringWriter stringWriter

    def setupSpec() {
        bsonDoc = new BsonDocument()
        buffer = new BasicOutputBuffer()
        stringWriter = new StringWriter()
    }

    def 'should throw if mark without resetting previous mark'(BsonWriter writer) {
        given:
        writer.with {
            writeStartDocument()
            writeInt64('int64', 52L)
            writeEndDocument()
        }

        when:
        BsonReader reader
        if (writer instanceof BsonDocumentWriter) {
            reader = new BsonDocumentReader(bsonDoc)
        } else if (writer instanceof BsonBinaryWriter) {

            reader = new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(
                    ByteBuffer.wrap(buffer.toByteArray()))), true)
        } else {
            reader = new JsonReader(stringWriter.toString())
        }

        reader.readStartDocument()
        reader.mark()
        reader.mark()

        then:
        thrown(BSONException)

        where:
        writer << [
                new BsonDocumentWriter(bsonDoc),
                new BsonBinaryWriter(buffer, false),
//            new JsonWriter(stringWriter) //java-1406
        ]
    }

    def 'should throw if reset without mark'(BsonWriter writer) {
        given:
        writer.with {
            writeStartDocument()
            writeInt64('int64', 52L)
            writeEndDocument()
        }

        when:
        BsonReader reader
        if (writer instanceof BsonDocumentWriter) {
            reader = new BsonDocumentReader(bsonDoc)
        } else if (writer instanceof BsonBinaryWriter) {

            reader = new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(
                    ByteBuffer.wrap(buffer.toByteArray()))), true)
        } else {
            reader = new JsonReader(stringWriter.toString())
        }

        reader.readStartDocument()
        reader.reset()

        then:
        thrown(BSONException)

        where:
        writer << [
                new BsonDocumentWriter(bsonDoc),
                new BsonBinaryWriter(buffer, false),
//            new JsonWriter(stringWriter) //java-1406
        ]
    }

    def 'Lookahead should work after reading end of document'(BsonWriter writer) {
        given:
        writer.with {
            writeStartDocument()
            writeInt64('int64', 52L)
            writeUndefined('undefined')
            writeStartArray('array')
            writeInt32(1)
            writeInt64(2L)
            writeStartArray()
            writeInt32(3)
            writeInt32(4)
            writeEndArray()
            writeStartDocument()
            writeInt32('a', 5)
            writeEndDocument()
            writeNull()
            writeEndArray()
            writeStartDocument('document')
            writeInt32('a', 6)
            writeEndDocument()
            writeEndDocument()
        }
        

        when:
        BsonReader reader
        if (writer instanceof BsonDocumentWriter) {
            reader = new BsonDocumentReader(bsonDoc)
        } else if (writer instanceof BsonBinaryWriter) {

            reader = new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(
                    ByteBuffer.wrap(buffer.toByteArray()))), true)
        } else {
            reader = new JsonReader(stringWriter.toString())
        }

        then:

        reader.readStartDocument()
        // mark beginning of document * 1
        reader.mark()
        reader.readName() == 'int64'
        reader.readInt64() == 52L
        // reset to beginning of document * 2
        reader.reset()
        // mark beginning of document * 2
        reader.mark()
        reader.readName() == 'int64'
        reader.readInt64() == 52L
        // reset to beginning of document * 3
        reader.reset()
        // mark beginning of document * 3
        reader.mark()
        reader.readName() == 'int64'
        reader.readInt64() == 52L
        reader.readName() == 'undefined'
        reader.readUndefined()
        reader.readName() == 'array'
        reader.readStartArray()
        reader.readInt32() == 1
        reader.readInt64() == 2
        reader.readStartArray()
        reader.readInt32() == 3
        reader.readInt32() == 4
        reader.readEndArray()
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 5
        reader.readEndDocument()
        reader.readNull()
        reader.readEndArray()
        reader.readName() == 'document'
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 6
        reader.readEndDocument()
        reader.readEndDocument()
        // read entire document, reset to beginning
        reader.reset()
        reader.readName() == 'int64'
        reader.readInt64() == 52L
        reader.readName() == 'undefined'
        reader.readUndefined()
        reader.readName() == 'array'
        // mar in outer-document * 1
        reader.mark()
        reader.readStartArray()
        reader.readInt32() == 1
        reader.readInt64() == 2
        reader.readStartArray()
        // reset in sub-document * 1
        reader.reset()
        // mark in outer-document * 2
        reader.mark()
        reader.readStartArray()
        reader.readInt32() == 1
        reader.readInt64() == 2
        reader.readStartArray()
        reader.readInt32() == 3
        // reset in sub-document * 2
        reader.reset()
        reader.readStartArray()
        reader.readInt32() == 1
        reader.readInt64() == 2
        reader.readStartArray()
        reader.readInt32() == 3
        reader.readInt32() == 4
        // mark in sub-document * 1
        reader.mark()
        reader.readEndArray()
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 5
        reader.readEndDocument()
        reader.readNull()
        reader.readEndArray()
        // reset in outer-document * 1
        reader.reset()
        // mark in sub-document * 2
        reader.mark()
        reader.readEndArray()
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 5
        reader.readEndDocument()
        reader.readNull()
        reader.readEndArray()
        // reset in out-document * 2
        reader.reset()
        reader.readEndArray()
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 5
        reader.readEndDocument()
        reader.readNull()
        reader.readEndArray()
        reader.readName() == 'document'
        reader.readStartDocument()
        reader.readName() == 'a'
        reader.readInt32() == 6
        reader.readEndDocument()
        reader.readEndDocument()

        where:
        writer << [
            new BsonDocumentWriter(bsonDoc),
            new BsonBinaryWriter(buffer, false),
            new JsonWriter(stringWriter)
        ]
    }
}
