package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Function;

public class SetList<T, S extends Set<R>, R> extends AbstractList<T> implements RandomAccess {

    private final List<T> list = new ObjectArrayList<>();
    private final S set;
    private final Map<R, WriteRecord> writeRecords;
    private final Function<T, R> getKey;

    public SetList(S set, Function<T, R> getKey) {
        this(set, new Object2ObjectOpenHashMap<>(), getKey);
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    public SetList(S set, Map<R, WriteRecord> writeRecords, Function<T, R> getKey) {
        this.set = set;
        this.writeRecords = writeRecords;
        this.getKey = getKey;
    }

    @Override
    public boolean add(T element) {
        WriteRecord currentRecord = captureWriteRecord(element);
        var key = getKey.apply(element);
        if (!set.add(key)) {
            warnDuplicate(element, currentRecord, writeRecords.get(key));
            return false;
        }
        list.add(element);
        writeRecords.put(key, currentRecord);
        modCount++;
        return true;
    }

    @Override
    public void add(int index, T element) {
        WriteRecord currentRecord = captureWriteRecord(element);
        var key = getKey.apply(element);
        if (!set.add(key)) {
            warnDuplicate(element, currentRecord, writeRecords.get(key));
            return;
        }
        list.add(index, element);
        writeRecords.put(key, currentRecord);
        modCount++;
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> collection) {
        int insertIndex = index;
        boolean changed = false;
        for (T element : collection) {
            WriteRecord currentRecord = captureWriteRecord(element);
            var key = getKey.apply(element);
            if (set.add(key)) {
                list.add(insertIndex, element);
                writeRecords.put(key, currentRecord);
                insertIndex++;
                changed = true;
            } else {
                warnDuplicate(element, currentRecord, writeRecords.get(key));
            }
        }
        if (changed) {
            modCount++;
        }
        return changed;
    }

    @Override
    public T set(int index, T element) {
        T oldValue = list.get(index);
        if (Objects.equals(oldValue, element)) {
            return oldValue;
        }

        var key = getKey.apply(element);
        WriteRecord currentRecord = captureWriteRecord(element);
        int existingIndex = list.indexOf(element);
        if (existingIndex >= 0) {
            warnDuplicate(element, currentRecord, writeRecords.get(key));
            list.remove(existingIndex);
            if (existingIndex < index) {
                index--;
            }
            list.set(index, element);
            var oldkey = getKey.apply(oldValue);
            set.remove(oldkey);
            writeRecords.remove(oldkey);
            writeRecords.put(oldkey, currentRecord);
            modCount++;
            return oldValue;
        }

        list.set(index, element);
        var oldkey = getKey.apply(oldValue);
        set.remove(oldkey);
        set.add(key);
        writeRecords.remove(oldkey);
        writeRecords.put(oldkey, currentRecord);
        modCount++;
        return oldValue;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        return addAll(list.size(), collection);
    }

    @Override
    public T remove(int index) {
        T removed = list.remove(index);
        var key = getKey.apply(removed);
        set.remove(key);
        writeRecords.remove(key);
        modCount++;
        return removed;
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index < 0) {
            return false;
        }
        remove(index);
        return true;
    }

    @Override
    public void clear() {
        if (list.isEmpty()) {
            return;
        }
        list.clear();
        set.clear();
        writeRecords.clear();
        modCount++;
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    private WriteRecord captureWriteRecord(T element) {
        return new WriteRecord(
            Thread.currentThread().getName(),
            String.valueOf(element),
            formatStackTrace(captureStackTrace())
        );
    }

    private StackTraceElement[] captureStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = 0;
        while (startIndex < stackTrace.length && isInternalFrame(stackTrace[startIndex])) {
            startIndex++;
        }
        return Arrays.copyOfRange(stackTrace, startIndex, stackTrace.length);
    }

    private boolean isInternalFrame(StackTraceElement element) {
        String className = element.getClassName();
        return Thread.class.getName().equals(className) || SetList.class.getName().equals(className);
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace.length == 0) {
            return "<empty>";
        }
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            builder.append("\tat ").append(element).append(lineSeparator);
        }
        builder.setLength(builder.length() - lineSeparator.length());
        return builder.toString();
    }

    private void warnDuplicate(T element, WriteRecord currentRecord, WriteRecord previousRecord) {
        if (previousRecord == null) {
            NovaEngineeringCore.log.warn(
                "SetList detected duplicate element: {}\ncurrent write:\n{}\nprevious write:\n<missing>",
                element,
                formatWriteRecord(currentRecord)
            );
            return;
        }
        NovaEngineeringCore.log.warn(
            "SetList detected duplicate element: {}\ncurrent write:\n{}\nprevious write:\n{}",
            element,
            formatWriteRecord(currentRecord),
            formatWriteRecord(previousRecord)
        );
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return list.iterator();
    }

    private String formatWriteRecord(WriteRecord record) {
        return "thread: " + record.threadName() + System.lineSeparator()
            + "element: " + record.elementText() + System.lineSeparator()
            + "stack:" + System.lineSeparator()
            + record.stackTraceText();
    }

    private record WriteRecord(String threadName, String elementText, String stackTraceText) {
    }
}
