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

package com.mongodb

import com.mongodb.codecs.DocumentCodec
import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.BsonDbPointer
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonReader
import org.bson.BsonRegularExpression
import org.bson.BsonTimestamp
import org.bson.BsonUndefined
import org.bson.BsonWriter
import org.bson.ByteBufNIO
import org.bson.codecs.EncoderContext
import org.bson.io.BasicInputBuffer
import org.bson.json.JsonReader
import org.bson.types.Binary
import org.bson.types.Code
import org.bson.types.MaxKey
import org.bson.types.MinKey
import org.bson.types.ObjectId
import org.bson.types.Symbol
import org.mongodb.CodeWithScope
import org.mongodb.Document
import spock.lang.Shared
import spock.lang.Specification

import java.nio.ByteBuffer

import static java.util.Arrays.asList

/**
 *
 */
class LimitedLookaheadMarkSpecification extends Specification {

    @Shared Document doc
    @Shared BsonDocument bsonDoc = new BsonDocument()
    @Shared StringWriter stringWriter = new StringWriter()

    def setupSpec() {
        doc = new Document()
        doc.with {
            put('int64', 52L)
            put('null', null)
            put('date', new Date())
            put('regex', new BsonRegularExpression('^test.*regex.*xyz$', 'i'))
            put('string', 'the fox ...')
            put('symbol', new Symbol('ruby stuff'))
            put('undefined', new BsonUndefined())
            put('array', asList(1, 1L, true, [1, 2, 3], new Document('a', 1), null))
            put('document', new Document('a', 2))
        }
    }

    def 'Lookahead should work after reading end of document'(BsonWriter writer) {
        given:
        doc

        when:
        new DocumentCodec().encode(writer, doc, EncoderContext.builder().build())

        BsonReader reader
        if (writer instanceof BsonDocumentWriter) {
            reader = new BsonDocumentReader(bsonDoc)
        } else if (writer instanceof BsonBinaryWriter) {
            reader = new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(
                    ByteBuffer.wrap(writer.buffer.toByteArray()))), true)
        } else {
            println(stringWriter.toString())
            reader = new JsonReader(stringWriter.toString())
        }

        reader.readStartDocument()
        reader.mark()
        reader.readName()
        reader.readInt64()
        reader.readName()
        reader.readNull()
        reader.readDateTime()

        then:
        1

        where:
        writer << [
            new BsonDocumentWriter(bsonDoc)
        ]
    }
}
