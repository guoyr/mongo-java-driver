package org.bson;

import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A {@code BsonReader} implementation that reads from an instance of {@code BsonDocument}.  This can be used to decode a {@code
 * BsonDocument} using a {@code Decoder}.
 *
 * @see BsonDocument
 * @see org.bson.codecs.Decoder
 *
 * @since 3.0
 */
public class BsonDocumentReader extends AbstractBsonReader {
    private BsonValue currentValue;
    private Mark mark;
    private int curIndex = -1;
    private int markStartIndex = -1;

    /**
     * Construct a new instance.
     *
     * @param document the document to read from
     */
    public BsonDocumentReader(final BsonDocument document) {
        super();
        setContext(new Context(null, BsonContextType.TOP_LEVEL, document));
        currentValue = document;
    }

    @Override
    protected BsonBinary doReadBinaryData() {
        return currentValue.asBinary();
    }

    @Override
    protected boolean doReadBoolean() {
        return currentValue.asBoolean().getValue();
    }

    @Override
    protected long doReadDateTime() {
        return currentValue.asDateTime().getValue();
    }

    @Override
    protected double doReadDouble() {
        return currentValue.asDouble().getValue();
    }

    @Override
    protected void doReadEndArray() {
        setContext(getContext().getParentContext());
    }

    @Override
    protected void doReadEndDocument() {
        setContext(getContext().getParentContext());
        switch (getContext().getContextType()) {
            case ARRAY:
            case DOCUMENT:
                setState(State.TYPE);
                break;
            case TOP_LEVEL:
                setState(State.DONE);
                break;
            default:
                throw new BSONException("Unexpected ContextType.");
        }
    }

    @Override
    protected int doReadInt32() {
        return currentValue.asInt32().getValue();
    }

    @Override
    protected long doReadInt64() {
        return currentValue.asInt64().getValue();
    }

    @Override
    protected String doReadJavaScript() {
        return currentValue.asJavaScript().getCode();
    }

    @Override
    protected String doReadJavaScriptWithScope() {
        return currentValue.asJavaScriptWithScope().getCode();
    }

    @Override
    protected void doReadMaxKey() {
    }

    @Override
    protected void doReadMinKey() {
    }

    @Override
    protected void doReadNull() {
    }

    @Override
    protected ObjectId doReadObjectId() {
        return currentValue.asObjectId().getValue();
    }

    @Override
    protected BsonRegularExpression doReadRegularExpression() {
        return currentValue.asRegularExpression();
    }

    @Override
    protected BsonDbPointer doReadDBPointer() {
        return currentValue.asDBPointer();
    }

    @Override
    protected void doReadStartArray() {
        BsonArray array = currentValue.asArray();
        setContext(new Context(getContext(), BsonContextType.ARRAY, array));
    }

    @Override
    protected void doReadStartDocument() {
        BsonDocument document;
        if (currentValue.getBsonType() == BsonType.JAVASCRIPT_WITH_SCOPE) {
            document = currentValue.asJavaScriptWithScope().getScope();
        } else {
            document = currentValue.asDocument();
        }
        setContext(new Context(getContext(), BsonContextType.DOCUMENT, document));
    }

    @Override
    protected String doReadString() {
        return currentValue.asString().getValue();
    }

    @Override
    protected String doReadSymbol() {
        return currentValue.asSymbol().getSymbol();
    }

    @Override
    protected BsonTimestamp doReadTimestamp() {
        return currentValue.asTimestamp();
    }

    @Override
    protected void doReadUndefined() {
    }

    @Override
    protected void doSkipName() {
    }

    @Override
    protected void doSkipValue() {
    }

    @Override
    public BsonType readBsonType() {
        if (getState() == State.INITIAL || getState() == State.SCOPE_DOCUMENT) {
            // there is an implied type of Document for the top level and for scope documents
            setCurrentBsonType(BsonType.DOCUMENT);
            setState(State.VALUE);
            return getCurrentBsonType();
        }

        if (getState() != State.TYPE) {
            throwInvalidState("ReadBSONType", State.TYPE);
        }

        switch (getContext().getContextType()) {
            case ARRAY:
                currentValue = getContext().getNextValue();
                if (currentValue == null) {
                    setState(State.END_OF_ARRAY);
                    return BsonType.END_OF_DOCUMENT;
                }
                setState(State.VALUE);
                break;
            case DOCUMENT:
                Map.Entry<String, BsonValue> currentElement = getContext().getNextElement();
                if (currentElement == null) {
                    setState(State.END_OF_DOCUMENT);
                    return BsonType.END_OF_DOCUMENT;
                }
                setCurrentName(currentElement.getKey());
                currentValue = currentElement.getValue();
                setState(State.NAME);
                break;
            default:
                throw new BSONException("Invalid ContextType.");
        }

        setCurrentBsonType(currentValue.getBsonType());
        return getCurrentBsonType();
    }

    @Override
    public void mark() {
        mark = new Mark();
        if (mark.documentIterator != null) {
            if (curIndex >= markStartIndex && curIndex < markStartIndex + mark.documentIterator.markIterator.size()) {
                // information already stored in markIterator
                mark.documentIterator.markIterator = mark.documentIterator.markIterator.subList(curIndex, mark.documentIterator.markIterator.size());
                markStartIndex = curIndex;
            } else {
                markStartIndex = curIndex;
            }
        } else {
            mark.arrayIterator.mark();
        }
    }

    @Override
    public void reset() {
        if (mark.documentIterator != null) {
            mark.documentIterator.reset();
        } else {
            mark.arrayIterator.reset();
        }
    }

    @Override
    protected Context getContext() {
        return (Context) super.getContext();
    }

    protected class Mark extends AbstractBsonReader.Mark {
        private BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>> documentIterator;
        private BsonDocumentMarkableIterator<BsonValue> arrayIterator;
        private BsonValue currentValue;

        protected Mark() {
            super();
            arrayIterator = BsonDocumentReader.this.getContext().arrayIterator;
            documentIterator = BsonDocumentReader.this.getContext().documentIterator;
            currentValue = BsonDocumentReader.this.currentValue;
        }

        protected void reset() {
            super.reset();
            BsonDocumentReader.this.currentValue = currentValue;
            BsonDocumentReader.this.setContext(new Context((Context)parentContext,
                    contextType,
                    documentIterator,
                    arrayIterator));

        }
    }

    private class BsonDocumentMarkableIterator<T> implements Iterator<T> {

        private Iterator<T> baseIterator;
        private List<T> markIterator = new ArrayList<T>();
        private int curIndex = Integer.MAX_VALUE; // position of the cursor
        private int markBeginningIndex = Integer.MAX_VALUE; // index of the beginning of the mark
        private Mark mark;

        protected BsonDocumentMarkableIterator(Iterator<T> baseIterator) {
            this.baseIterator = baseIterator;
        }

        /**
         *
         */
        protected void mark() {
            if (mark != null) {
                throw new BSONException("a mark already exists, must be cleared before creating a new one");
            }
            mark = new Mark();

        }

        /**
         *
         */
        protected void reset() {
            mark.reset();
            mark = null;
            curIndex = 0;
        }


        @Override
        public boolean hasNext() {
            return baseIterator.hasNext() || curIndex < markIterator.size() - 1;
        }

        @Override
        public T next() throws NoSuchElementException {

            T value;
            //TODO: check closed
            if (curIndex < markIterator.size() - 1) {
                curIndex++;
                value = markIterator.get(curIndex);
            } else {
                value = baseIterator.next();
                if (mark != null) {
                    markIterator.add(value);
                }
            }
            return value;
        }

        @Override
        public void remove() {
            // iterator is read only
        }
    }

    protected class Context extends AbstractBsonReader.Context {

        private BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>> documentIterator;
        private BsonDocumentMarkableIterator<BsonValue> arrayIterator;

        protected Context(final Context parentContext, final BsonContextType contextType, final BsonArray array) {
            super(parentContext, contextType);
            arrayIterator = new BsonDocumentMarkableIterator<BsonValue>(array.iterator());
        }

        protected Context(final Context parentContext, final BsonContextType contextType, final BsonDocument document) {
            super(parentContext, contextType);
            documentIterator = new BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>>(document.entrySet().iterator());
        }

        protected Context(final Context parentContext,
                          final BsonContextType contextType,
                          final BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>> documentIterator,
                          final BsonDocumentMarkableIterator<BsonValue> arrayIterator) {
            super(parentContext, contextType);
            this.arrayIterator = arrayIterator;
            this.documentIterator = documentIterator;

        }

        public Map.Entry<String, BsonValue> getNextElement() {
            if (documentIterator.hasNext()) {
                return documentIterator.next();
            } else {
                return null;
            }
        }

        public BsonValue getNextValue() {
            if (arrayIterator.hasNext()) {
                return arrayIterator.next();
            } else {
                return null;
            }
        }

        protected void mark() {

        }

        protected void reset() {

        }

        protected void clearMark() {
            if (documentIterator != null) {
            } else {
            }
        }
    }


}
