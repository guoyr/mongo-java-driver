package org.bson;

import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Iterator;
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
        getContext().mark();
    }

    @Override
    public void reset() {
        getContext().reset();
    }

    @Override
    public void clearMark() {
        getContext().clearMark();
    }

    @Override
    protected Context getContext() {
        return (Context) super.getContext();
    }

    private class BsonDocumentMarkableIterator<T> implements Iterator<T> {

        private Iterator<T> baseIterator;
        private ArrayList<T> markIterator = new ArrayList<T>();
        private int curIndex;
        private boolean marking = false;

        private State markedState;
        private BsonType currentBsonType;
        private BsonContextType currentContextType;
        private AbstractBsonReader.Context parentContext;

        public BsonDocumentMarkableIterator(Iterator<T> baseIterator) {
            this.baseIterator = baseIterator;
        }

        /**
         *
         */
        protected void mark() {
            markedState = BsonDocumentReader.this.getState();
            currentBsonType = BsonDocumentReader.this.getCurrentBsonType();
            currentContextType = BsonDocumentReader.this.getContext().getContextType();
            parentContext = BsonDocumentReader.this.getContext().getParentContext();
            curIndex = Integer.MAX_VALUE;
            marking = true;
        }

        /**
         *
         */
        protected void reset() {
            BsonDocumentReader.this.setState(markedState);
            BsonDocumentReader.this.setCurrentBsonType(currentBsonType);
            BsonDocumentReader.this.getContext().setContextType(currentContextType);
            BsonDocumentReader.this.getContext().setParentContext(parentContext);
            marking = false;
            curIndex = 0;
        }

        /**
         *
         */
        protected void clearMark() {
            markIterator.clear();
            curIndex = Integer.MAX_VALUE;
            reset();
        }

        @Override
        public boolean hasNext() {
            return baseIterator.hasNext() || curIndex < markIterator.size() - 1;
        }

        @Override
        public T next() throws NoSuchElementException {

            T value;

            if (curIndex < markIterator.size() - 1) {
                curIndex++;
                value = markIterator.get(curIndex);
            } else {
                value = baseIterator.next();
                if (marking) {
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
        private BsonDocumentMarkableIterator activeIterator;

        protected Context(final Context parentContext, final BsonContextType contextType, final BsonArray array) {
            super(parentContext, contextType);
            arrayIterator = new BsonDocumentMarkableIterator<BsonValue>(array.iterator());
            activeIterator = (BsonDocumentMarkableIterator)arrayIterator;
        }

        protected Context(final Context parentContext, final BsonContextType contextType, final BsonDocument document) {
            super(parentContext, contextType);
            documentIterator = new BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>>(document.entrySet().iterator());
            activeIterator = (BsonDocumentMarkableIterator)documentIterator;

        }

        protected Context(final Context parentContext, final BsonContextType contextType) {
            super(parentContext, contextType);

        }

        public BsonDocumentMarkableIterator<BsonValue> getArrayIterator() {
            return arrayIterator;
        }

        public BsonDocumentMarkableIterator<Map.Entry<String, BsonValue>> getDocumentIterator() {
            return documentIterator;
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

        @Override
        protected void mark() {
            activeIterator.mark();
        }

        @Override
        protected void reset() {
            activeIterator.reset();
        }

        @Override
        protected void clearMark() {
            activeIterator.clearMark();
        }
    }


}
