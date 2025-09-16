package org.springframework.yangxm.ai.mcp.method.tool.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.yangxm.ai.util.Assert;
import reactor.util.annotation.Nullable;

@SuppressWarnings("unused")
public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
    private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;
    private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;
    private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;

    private final Segment[] segments;
    private final float loadFactor;
    private final ReferenceType referenceType;
    private final int shift;
    private volatile Set<Map.Entry<K, V>> entrySet;

    public ConcurrentReferenceHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    public ConcurrentReferenceHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
    }

    public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }

    public ConcurrentReferenceHashMap(int initialCapacity, ReferenceType referenceType) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
    }

    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
    }

    @SuppressWarnings("unchecked")
    public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {
        Assert.notNull(referenceType, "Reference type must not be null");
        this.loadFactor = loadFactor;
        this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
        int size = 1 << this.shift;
        this.referenceType = referenceType;
        int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
        int initialSize = 1 << calculateShift(roundedUpSegmentCapacity, MAXIMUM_SEGMENT_SIZE);
        Segment[] segments = (Segment[]) Array.newInstance(Segment.class, size);
        int resizeThreshold = (int) (initialSize * getLoadFactor());
        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Segment(initialSize, resizeThreshold);
        }
        this.segments = segments;
    }

    protected final float getLoadFactor() {
        return this.loadFactor;
    }

    protected final int getSegmentsSize() {
        return this.segments.length;
    }

    protected final Segment getSegment(int index) {
        return this.segments[index];
    }

    protected ReferenceManager createReferenceManager() {
        return new ReferenceManager();
    }

    protected int getHash(Object o) {
        int hash = (o != null ? o.hashCode() : 0);
        hash += (hash << 15) ^ 0xffffcd7d;
        hash ^= (hash >>> 10);
        hash += (hash << 3);
        hash ^= (hash >>> 6);
        hash += (hash << 2) + (hash << 14);
        hash ^= (hash >>> 16);
        return hash;
    }

    @Override
    public V get(Object key) {
        Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
        Entry<K, V> entry = (ref != null ? ref.get() : null);
        return (entry != null ? entry.getValue() : null);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
        Entry<K, V> entry = (ref != null ? ref.get() : null);
        return (entry != null ? entry.getValue() : defaultValue);
    }

    @Override
    public boolean containsKey(Object key) {
        Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
        Entry<K, V> entry = (ref != null ? ref.get() : null);
        return (entry != null && nullSafeEquals(entry.getKey(), key));
    }

    protected final Reference<K, V> getReference(Object key, Restructure restructure) {
        int hash = getHash(key);
        return getSegmentForHash(hash).getReference(key, hash, restructure);
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, true);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return put(key, value, false);
    }

    private V put(final K key, final V value, final boolean overwriteExisting) {
        return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.RESIZE) {
            @Override

            protected V execute(Reference<K, V> ref, Entry<K, V> entry, Entries<V> entries) {
                if (entry != null) {
                    V oldValue = entry.getValue();
                    if (overwriteExisting) {
                        entry.setValue(value);
                    }
                    return oldValue;
                }
                // Assert.state(entries != null, "No entries segment");
                entries.add(value);
                return null;
            }
        });
    }

    @Override
    public V remove(Object key) {
        return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
            @Override
            protected V execute(Reference<K, V> ref, Entry<K, V> entry) {
                if (entry != null) {
                    if (ref != null) {
                        ref.release();
                    }
                    return entry.value;
                }
                return null;
            }
        });
    }

    @Override
    public boolean remove(Object key, final Object value) {
        Boolean result = doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
            @Override
            protected Boolean execute(Reference<K, V> ref, Entry<K, V> entry) {
                if (entry != null && nullSafeEquals(entry.getValue(), value)) {
                    if (ref != null) {
                        ref.release();
                    }
                    return true;
                }
                return false;
            }
        });
        return (Boolean.TRUE.equals(result));
    }

    @Override
    public boolean replace(K key, final V oldValue, final V newValue) {
        Boolean result = doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
            @Override
            protected Boolean execute(Reference<K, V> ref, Entry<K, V> entry) {
                if (entry != null && nullSafeEquals(entry.getValue(), oldValue)) {
                    entry.setValue(newValue);
                    return true;
                }
                return false;
            }
        });
        return (Boolean.TRUE.equals(result));
    }

    @Override
    public V replace(K key, final V value) {
        return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
            @Override

            protected V execute(Reference<K, V> ref, Entry<K, V> entry) {
                if (entry != null) {
                    V oldValue = entry.getValue();
                    entry.setValue(value);
                    return oldValue;
                }
                return null;
            }
        });
    }

    @Override
    public void clear() {
        for (Segment segment : this.segments) {
            segment.clear();
        }
    }

    public void purgeUnreferencedEntries() {
        for (Segment segment : this.segments) {
            segment.restructureIfNecessary(false);
        }
    }

    @Override
    public int size() {
        int size = 0;
        for (Segment segment : this.segments) {
            size += segment.getCount();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (Segment segment : this.segments) {
            if (segment.getCount() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = this.entrySet;
        if (entrySet == null) {
            entrySet = new EntrySet();
            this.entrySet = entrySet;
        }
        return entrySet;
    }

    private <T> T doTask(Object key, Task<T> task) {
        int hash = getHash(key);
        return getSegmentForHash(hash).doTask(hash, key, task);
    }

    private Segment getSegmentForHash(int hash) {
        return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
    }

    protected static int calculateShift(int minimumValue, int maximumValue) {
        int shift = 0;
        int value = 1;
        while (value < minimumValue && value < maximumValue) {
            value <<= 1;
            shift++;
        }
        return shift;
    }

    public enum ReferenceType {
        SOFT,
        WEAK
    }

    protected final class Segment extends ReentrantLock {
        private final ReferenceManager referenceManager;
        private final int initialSize;
        private volatile Reference<K, V>[] references;
        private final AtomicInteger count = new AtomicInteger();
        private int resizeThreshold;

        public Segment(int initialSize, int resizeThreshold) {
            this.referenceManager = createReferenceManager();
            this.initialSize = initialSize;
            this.references = createReferenceArray(initialSize);
            this.resizeThreshold = resizeThreshold;
        }

        public Reference<K, V> getReference(Object key, int hash, Restructure restructure) {
            if (restructure == Restructure.WHEN_NECESSARY) {
                restructureIfNecessary(false);
            }
            if (this.count.get() == 0) {
                return null;
            }
            Reference<K, V>[] references = this.references;
            int index = getIndex(hash, references);
            Reference<K, V> head = references[index];
            return findInChain(head, key, hash);
        }

        <T> T doTask(final int hash, final Object key, final Task<T> task) {
            boolean resize = task.hasOption(TaskOption.RESIZE);
            if (task.hasOption(TaskOption.RESTRUCTURE_BEFORE)) {
                restructureIfNecessary(resize);
            }
            if (task.hasOption(TaskOption.SKIP_IF_EMPTY) && this.count.get() == 0) {
                return task.execute(null, null, null);
            }
            lock();
            try {
                final int index = getIndex(hash, this.references);
                final Reference<K, V> head = this.references[index];
                Reference<K, V> ref = findInChain(head, key, hash);
                Entry<K, V> entry = (ref != null ? ref.get() : null);
                Entries<V> entries = value -> {
                    @SuppressWarnings("unchecked")
                    Entry<K, V> newEntry = new Entry<>((K) key, value);
                    Reference<K, V> newReference = Segment.this.referenceManager.createReference(newEntry, hash, head);
                    Segment.this.references[index] = newReference;
                    Segment.this.count.incrementAndGet();
                };
                return task.execute(ref, entry, entries);
            } finally {
                unlock();
                if (task.hasOption(TaskOption.RESTRUCTURE_AFTER)) {
                    restructureIfNecessary(resize);
                }
            }
        }

        public void clear() {
            if (this.count.get() == 0) {
                return;
            }
            lock();
            try {
                this.references = createReferenceArray(this.initialSize);
                this.resizeThreshold = (int) (this.references.length * getLoadFactor());
                this.count.set(0);
            } finally {
                unlock();
            }
        }

        void restructureIfNecessary(boolean allowResize) {
            int currCount = this.count.get();
            boolean needsResize = allowResize && (currCount > 0 && currCount >= this.resizeThreshold);
            Reference<K, V> ref = this.referenceManager.pollForPurge();
            if (ref != null || (needsResize)) {
                restructure(allowResize, ref);
            }
        }

        private void restructure(boolean allowResize, Reference<K, V> ref) {
            boolean needsResize;
            lock();
            try {
                int expectedCount = this.count.get();
                Set<Reference<K, V>> toPurge = Collections.emptySet();
                if (ref != null) {
                    toPurge = new HashSet<>();
                    while (ref != null) {
                        toPurge.add(ref);
                        ref = this.referenceManager.pollForPurge();
                    }
                }
                expectedCount -= toPurge.size();

                needsResize = (expectedCount > 0 && expectedCount >= this.resizeThreshold);
                boolean resizing = false;
                int restructureSize = this.references.length;
                if (allowResize && needsResize && restructureSize < MAXIMUM_SEGMENT_SIZE) {
                    restructureSize <<= 1;
                    resizing = true;
                }

                int newCount = 0;
                if (resizing) {
                    Reference<K, V>[] restructured = createReferenceArray(restructureSize);
                    for (Reference<K, V> reference : this.references) {
                        ref = reference;
                        while (ref != null) {
                            if (!toPurge.contains(ref)) {
                                Entry<K, V> entry = ref.get();
                                if (entry != null) {
                                    int index = getIndex(ref.getHash(), restructured);
                                    restructured[index] = this.referenceManager.createReference(
                                            entry, ref.getHash(), restructured[index]);
                                    newCount++;
                                }
                            }
                            ref = ref.getNext();
                        }
                    }
                    this.references = restructured;
                    this.resizeThreshold = (int) (this.references.length * getLoadFactor());
                } else {
                    for (int i = 0; i < this.references.length; i++) {
                        Reference<K, V> purgedRef = null;
                        ref = this.references[i];
                        while (ref != null) {
                            if (!toPurge.contains(ref)) {
                                Entry<K, V> entry = ref.get();
                                if (entry != null) {
                                    purgedRef = this.referenceManager.createReference(entry, ref.getHash(), purgedRef);
                                }
                                newCount++;
                            }
                            ref = ref.getNext();
                        }
                        this.references[i] = purgedRef;
                    }
                }
                this.count.set(Math.max(newCount, 0));
            } finally {
                unlock();
            }
        }

        private Reference<K, V> findInChain(Reference<K, V> ref, Object key, int hash) {
            Reference<K, V> currRef = ref;
            while (currRef != null) {
                if (currRef.getHash() == hash) {
                    Entry<K, V> entry = currRef.get();
                    if (entry != null) {
                        K entryKey = entry.getKey();
                        if (nullSafeEquals(entryKey, key)) {
                            return currRef;
                        }
                    }
                }
                currRef = currRef.getNext();
            }
            return null;
        }

        @SuppressWarnings({"unchecked"})
        private Reference<K, V>[] createReferenceArray(int size) {
            return new Reference[size];
        }

        private int getIndex(int hash, Reference<K, V>[] references) {
            return (hash & (references.length - 1));
        }

        public int getSize() {
            return this.references.length;
        }

        public int getCount() {
            return this.count.get();
        }
    }

    protected interface Reference<K, V> {
        Entry<K, V> get();

        int getHash();

        Reference<K, V> getNext();

        void release();
    }

    protected static final class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private volatile V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V previous = this.value;
            this.value = value;
            return previous;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> that = (Map.Entry<?, ?>) other;
                return nullSafeEquals(getKey(), that.getKey()) && nullSafeEquals(getValue(), that.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (nullSafeHashCode(this.key) ^ nullSafeHashCode(this.value));
        }

        @Override
        public String toString() {
            return (this.key + "=" + this.value);
        }
    }

    private abstract class Task<T> {
        private final EnumSet<TaskOption> options;

        public Task(TaskOption... options) {
            this.options = (options.length == 0 ? EnumSet.noneOf(TaskOption.class) : EnumSet.of(options[0], options));
        }

        public boolean hasOption(TaskOption option) {
            return this.options.contains(option);
        }

        protected T execute(Reference<K, V> ref, Entry<K, V> entry, Entries<V> entries) {
            return execute(ref, entry);
        }

        protected T execute(Reference<K, V> ref, Entry<K, V> entry) {
            return null;
        }
    }

    private enum TaskOption {
        RESTRUCTURE_BEFORE, RESTRUCTURE_AFTER, SKIP_IF_EMPTY, RESIZE
    }

    private interface Entries<V> {
        void add(V value);
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                Reference<K, V> ref = ConcurrentReferenceHashMap.this.getReference(entry.getKey(), Restructure.NEVER);
                Entry<K, V> otherEntry = (ref != null ? ref.get() : null);
                if (otherEntry != null) {
                    return nullSafeEquals(entry.getValue(), otherEntry.getValue());
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                return ConcurrentReferenceHashMap.this.remove(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public int size() {
            return ConcurrentReferenceHashMap.this.size();
        }

        @Override
        public void clear() {
            ConcurrentReferenceHashMap.this.clear();
        }
    }

    private class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private int segmentIndex;
        private int referenceIndex;
        private Reference<K, V>[] references;
        private Reference<K, V> reference;
        private Entry<K, V> next;
        private Entry<K, V> last;

        public EntryIterator() {
            moveToNextSegment();
        }

        @Override
        public boolean hasNext() {
            getNextIfNecessary();
            return (this.next != null);
        }

        @Override
        public Entry<K, V> next() {
            getNextIfNecessary();
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            this.last = this.next;
            this.next = null;
            return this.last;
        }

        private void getNextIfNecessary() {
            while (this.next == null) {
                moveToNextReference();
                if (this.reference == null) {
                    return;
                }
                this.next = this.reference.get();
            }
        }

        private void moveToNextReference() {
            if (this.reference != null) {
                this.reference = this.reference.getNext();
            }
            while (this.reference == null && this.references != null) {
                if (this.referenceIndex >= this.references.length) {
                    moveToNextSegment();
                    this.referenceIndex = 0;
                } else {
                    this.reference = this.references[this.referenceIndex];
                    this.referenceIndex++;
                }
            }
        }

        private void moveToNextSegment() {
            this.reference = null;
            this.references = null;
            if (this.segmentIndex < ConcurrentReferenceHashMap.this.segments.length) {
                this.references = ConcurrentReferenceHashMap.this.segments[this.segmentIndex].references;
                this.segmentIndex++;
            }
        }

        @Override
        public void remove() {
            ConcurrentReferenceHashMap.this.remove(this.last.getKey());
            this.last = null;
        }
    }

    protected enum Restructure {
        WHEN_NECESSARY, NEVER
    }

    protected class ReferenceManager {
        private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<>();

        public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {
            if (ConcurrentReferenceHashMap.this.referenceType == ReferenceType.WEAK) {
                return new WeakEntryReference<>(entry, hash, next, this.queue);
            }
            return new SoftEntryReference<>(entry, hash, next, this.queue);
        }

        @SuppressWarnings("unchecked")
        public Reference<K, V> pollForPurge() {
            return (Reference<K, V>) this.queue.poll();
        }
    }

    private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {
        private final int hash;
        private final Reference<K, V> nextReference;

        public SoftEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {

            super(entry, queue);
            this.hash = hash;
            this.nextReference = next;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override

        public Reference<K, V> getNext() {
            return this.nextReference;
        }

        @Override
        public void release() {
            enqueue();
        }
    }

    private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {
        private final int hash;
        private final Reference<K, V> nextReference;

        public WeakEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
            super(entry, queue);
            this.hash = hash;
            this.nextReference = next;
        }

        @Override
        public int getHash() {
            return this.hash;
        }

        @Override

        public Reference<K, V> getNext() {
            return this.nextReference;
        }

        @Override
        public void release() {
            enqueue();
        }
    }

    public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    private static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] && o2 instanceof Object[]) {
            Object[] objects1 = (Object[]) o1;
            Object[] objects2 = (Object[]) o2;
            return Arrays.equals(objects1, objects2);
        }
        if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            boolean[] booleans1 = (boolean[]) o1;
            boolean[] booleans2 = (boolean[]) o2;
            return Arrays.equals(booleans1, booleans2);
        }
        if (o1 instanceof byte[] && o2 instanceof byte[]) {
            byte[] bytes1 = (byte[]) o1;
            byte[] bytes2 = (byte[]) o2;
            return Arrays.equals(bytes1, bytes2);
        }
        if (o1 instanceof char[] && o2 instanceof char[]) {
            char[] chars1 = (char[]) o1;
            char[] chars2 = (char[]) o2;
            return Arrays.equals(chars1, chars2);
        }
        if (o1 instanceof double[] && o2 instanceof double[]) {
            double[] doubles1 = (double[]) o1;
            double[] doubles2 = (double[]) o2;
            return Arrays.equals(doubles1, doubles2);
        }
        if (o1 instanceof float[] && o2 instanceof float[]) {
            float[] floats1 = (float[]) o1;
            float[] floats2 = (float[]) o2;
            return Arrays.equals(floats1, floats2);
        }
        if (o1 instanceof int[] && o2 instanceof int[]) {
            int[] ints1 = (int[]) o1;
            int[] ints2 = (int[]) o2;
            return Arrays.equals(ints1, ints2);
        }
        if (o1 instanceof long[] && o2 instanceof long[]) {
            long[] longs1 = (long[]) o1;
            long[] longs2 = (long[]) o2;
            return Arrays.equals(longs1, longs2);
        }
        if (o1 instanceof short[] && o2 instanceof short[]) {
            short[] shorts1 = (short[]) o1;
            short[] shorts2 = (short[]) o2;
            return Arrays.equals(shorts1, shorts2);
        }
        return false;
    }

    public static int nullSafeHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                Object[] objects = (Object[]) obj;
                return Arrays.hashCode(objects);
            }
            if (obj instanceof boolean[]) {
                boolean[] booleans = (boolean[]) obj;
                return Arrays.hashCode(booleans);
            }
            if (obj instanceof byte[]) {
                byte[] bytes = (byte[]) obj;
                return Arrays.hashCode(bytes);
            }
            if (obj instanceof char[]) {
                char[] chars = (char[]) obj;
                return Arrays.hashCode(chars);
            }
            if (obj instanceof double[]) {
                double[] doubles = (double[]) obj;
                return Arrays.hashCode(doubles);
            }
            if (obj instanceof float[]) {
                float[] floats = (float[]) obj;
                return Arrays.hashCode(floats);
            }
            if (obj instanceof int[]) {
                int[] ints = (int[]) obj;
                return Arrays.hashCode(ints);
            }
            if (obj instanceof long[]) {
                long[] longs = (long[]) obj;
                return Arrays.hashCode(longs);
            }
            if (obj instanceof short[]) {
                short[] shorts = (short[]) obj;
                return Arrays.hashCode(shorts);
            }
        }
        return obj.hashCode();
    }
}
