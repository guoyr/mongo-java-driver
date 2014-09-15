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

package com.mongodb.codecs;

import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.UUID;

/**
 *
 */
public class UUIDCodecProvider implements CodecProvider {

    private UuidRepresentation encoderRepresentation;
    private UuidRepresentation decoderRepresentation;

    /**
     * Set the UUIDRepresentation to be used in the codec
     * default is JAVA_LEGACY to be compatible with existing documents
     *
     * @param encoderRepresentation the representation of UUID for encoding
     * @param decoderRepresentation the representation of the UUID for decoding
     *
     * @since 3.0
     * @see org.bson.UuidRepresentation
     */
    public UUIDCodecProvider(final UuidRepresentation encoderRepresentation, final UuidRepresentation decoderRepresentation) {
        this.encoderRepresentation = encoderRepresentation;
        this.decoderRepresentation = decoderRepresentation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (clazz == UUID.class) {
            return (Codec<T>) (new UUIDCodec(encoderRepresentation, decoderRepresentation));
        }
        return null;
    }
}
