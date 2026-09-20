package io.github.insideranh.stellarprotect.cache;

import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.items.ItemTemplate;

import java.util.*;

public class ItemsCache {

    private static final int INITIAL_CAPACITY = 8192;
    private static final float LOAD_FACTOR = 0.7f;

    private final int capacity = INITIAL_CAPACITY;
    private final int mask = INITIAL_CAPACITY - 1;
    private final ItemTemplate[] items = new ItemTemplate[INITIAL_CAPACITY];
    private final Map<Long, ItemTemplate> allItems = new LinkedHashMap<>();
    private final Set<Long> indexedIds = new HashSet<>();

    private final IndexEntry[] displayNameIndex = new IndexEntry[INITIAL_CAPACITY];
    private final IndexEntry[] loreIndex = new IndexEntry[INITIAL_CAPACITY];
    private final IndexEntry[] typeNameIndex = new IndexEntry[INITIAL_CAPACITY];

    private final Map<String, IntSet> displayNameTokens = new HashMap<>();
    private final Map<String, IntSet> loreTokens = new HashMap<>();
    private final Map<String, IntSet> typeNameTokens = new HashMap<>();

    private final int[] validPositions = new int[INITIAL_CAPACITY];
    private int validCount = 0;
    private int nextFreeHint = 0;

    private int size = 0;

    private static long hash(String str) {
        if (str == null) return 0;

        long hash = 0;
        final int len = str.length();

        int i = 0;
        for (; i < len - 3; i += 4) {
            hash = (hash << 5) - hash + str.charAt(i);
            hash = (hash << 5) - hash + str.charAt(i + 1);
            hash = (hash << 5) - hash + str.charAt(i + 2);
            hash = (hash << 5) - hash + str.charAt(i + 3);
        }

        for (; i < len; i++) {
            hash = (hash << 5) - hash + str.charAt(i);
        }

        return hash;
    }

    private void indexSubstrings(String text, Map<String, IntSet> tokenMap, int position) {
        if (text == null || text.isEmpty()) return;

        String lowerText = text.toLowerCase(Locale.ROOT);

        for (int len = 2; len <= Math.min(6, lowerText.length()); len++) {
            for (int i = 0; i <= lowerText.length() - len; i++) {
                String substring = lowerText.substring(i, i + len);

                tokenMap.computeIfAbsent(substring, k -> new IntSet()).add(position);
            }
        }

        String[] words = lowerText.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                tokenMap.computeIfAbsent(word, k -> new IntSet()).add(position);
            }
        }
    }

    private int findFreePosition() {
        for (int i = nextFreeHint; i < capacity; i++) {
            if (items[i] == null) {
                nextFreeHint = i + 1;
                return i;
            }
        }
        for (int i = 0; i < nextFreeHint; i++) {
            if (items[i] == null) {
                nextFreeHint = i + 1;
                return i;
            }
        }
        throw new IllegalStateException("Cache full");
    }

    private void addToStringIndex(IndexEntry[] index, String key, int position) {
        if (key == null) return;

        long hash = hash(key);
        int slot = (int) (hash & mask);

        IndexEntry entry = new IndexEntry(hash, position);

        while (index[slot] != null) {
            slot = (slot + 1) & mask;
        }

        index[slot] = entry;
    }

    private ItemTemplate searchInStringIndex(IndexEntry[] index, String key) {
        if (key == null) return null;

        long hash = hash(key);
        int slot = (int) (hash & mask);

        while (index[slot] != null) {
            IndexEntry entry = index[slot];

            if (entry.hash == hash) {
                ItemTemplate item = items[entry.position];

                if (item != null &&
                    ((index == typeNameIndex && key.equals(item.getTypeName()) ||
                        (index == loreIndex && key.equals(item.getLore())) ||
                        (index == displayNameIndex && key.equals(item.getDisplayName()))))) {
                    return item;
                }
            }

            slot = (slot + 1) & mask;
        }

        return null;
    }

    private ItemTemplate searchById(long id) {
        return allItems.get(id);
    }

    private List<Long> findContains(String searchText, Map<String, IntSet> tokenMap, FieldType fieldType) {
        if (searchText == null || searchText.isEmpty()) {
            return new ArrayList<>();
        }

        String lowerSearch = searchText.toLowerCase(Locale.ROOT);
        List<Long> results = new ArrayList<>();

        IntSet candidatePositions = tokenMap.get(lowerSearch);
        if (candidatePositions != null) {
            int[] positions = candidatePositions.toArray();
            for (int pos : positions) {
                ItemTemplate item = items[pos];
                if (item != null) {
                    results.add(item.id);
                }
            }
            appendOverflowMatches(results, lowerSearch, fieldType);
            return results;
        }

        String bestMatch = null;
        IntSet bestPositions = null;

        for (int len = Math.min(lowerSearch.length(), 6); len >= 2; len--) {
            for (int i = 0; i <= lowerSearch.length() - len; i++) {
                String candidate = lowerSearch.substring(i, i + len);
                IntSet positions = tokenMap.get(candidate);
                if (positions != null) {
                    bestMatch = candidate;
                    bestPositions = positions;
                    break;
                }
            }
            if (bestMatch != null) break;
        }

        if (bestPositions != null) {
            int[] positions = bestPositions.toArray();
            for (int pos : positions) {
                ItemTemplate item = items[pos];
                if (item != null) {
                    String fieldValue = getFieldValue(item, fieldType);
                    if (fieldValue != null && fieldValue.contains(lowerSearch)) {
                        results.add(item.id);
                    }
                }
            }
            appendOverflowMatches(results, lowerSearch, fieldType);
            return results;
        }

        for (int i = 0; i < validCount; i++) {
            int pos = validPositions[i];
            ItemTemplate item = items[pos];
            if (item != null) {
                String fieldValue = getFieldValue(item, fieldType);
                if (fieldValue != null && fieldValue.contains(lowerSearch)) {
                    results.add(item.id);
                }
            }
        }

        appendOverflowMatches(results, lowerSearch, fieldType);
        return results;
    }

    private void appendOverflowMatches(List<Long> results, String lowerSearch, FieldType fieldType) {
        for (ItemTemplate item : allItems.values()) {
            if (indexedIds.contains(item.id)) continue;

            String fieldValue = getFieldValue(item, fieldType);
            if (fieldValue != null && fieldValue.contains(lowerSearch)) {
                results.add(item.id);
            }
        }
    }

    private ItemTemplate findExactOverflow(String expected, FieldType fieldType) {
        if (expected == null) return null;

        for (ItemTemplate item : allItems.values()) {
            if (indexedIds.contains(item.id)) continue;
            if (expected.equals(getFieldValue(item, fieldType))) return item;
        }
        return null;
    }

    private String getFieldValue(ItemTemplate item, FieldType fieldType) {
        switch (fieldType) {
            case DISPLAY_NAME:
                return item.getDisplayName();
            case LORE:
                return item.getLore();
            case TYPE_NAME:
                return item.getTypeName();
            case LOWER_DISPLAY_NAME:
                return item.getDisplayNameLower();
            case LOWER_LORE:
                return item.getLoreLower();
            case LOWER_TYPE_NAME:
                return item.getTypeNameLower();
            default:
                return null;
        }
    }

    public synchronized boolean put(ItemTemplate item) {
        if (item == null) {
            return false;
        }

        if (allItems.putIfAbsent(item.id, item) != null) return true;

        // Keep the allocation-heavy substring indexes bounded. Historical templates
        // beyond the search tier remain available by ID and are scanned only for an
        // explicit staff lookup.
        if (size + 1 > capacity * LOAD_FACTOR) return true;

        putWithoutResize(item);
        return true;
    }

    private void putWithoutResize(ItemTemplate item) {

        int position = findFreePosition();

        items[position] = item;
        indexedIds.add(item.id);

        validPositions[validCount++] = position;

        if (item.getDisplayName() != null) addToStringIndex(displayNameIndex, item.getDisplayName(), position);
        if (item.getLore() != null) addToStringIndex(loreIndex, item.getLore(), position);
        if (item.getTypeName() != null) addToStringIndex(typeNameIndex, item.getTypeName(), position);

        if (item.getDisplayName() != null) indexSubstrings(item.getDisplayName(), displayNameTokens, position);
        if (item.getLore() != null) indexSubstrings(item.getLore(), loreTokens, position);
        if (item.getTypeName() != null) indexSubstrings(item.getTypeName(), typeNameTokens, position);

        size++;
    }

    public synchronized ItemTemplate getById(long id) {
        return searchById(id);
    }

    public synchronized ItemTemplate getByDisplayNameExact(String displayName) {
        ItemTemplate indexed = searchInStringIndex(displayNameIndex, displayName);
        return indexed != null ? indexed : findExactOverflow(displayName, FieldType.DISPLAY_NAME);
    }

    public synchronized ItemTemplate getByLoreExact(String lore) {
        ItemTemplate indexed = searchInStringIndex(loreIndex, lore);
        return indexed != null ? indexed : findExactOverflow(lore, FieldType.LORE);
    }

    public synchronized ItemTemplate getByTypeNameExact(String typeName) {
        ItemTemplate indexed = searchInStringIndex(typeNameIndex, typeName);
        return indexed != null ? indexed : findExactOverflow(typeName, FieldType.TYPE_NAME);
    }

    public synchronized List<Long> findIdsByDisplayNameContains(String searchText) {
        return findContains(searchText, displayNameTokens, FieldType.LOWER_DISPLAY_NAME);
    }

    public synchronized List<Long> findIdsByLoreContains(String searchText) {
        return findContains(searchText, loreTokens, FieldType.LOWER_LORE);
    }

    public synchronized List<Long> findIdsByTypeNameContains(List<String> searchTexts, FieldType fieldType) {
        if (searchTexts.isEmpty()) return Collections.emptyList();

        List<Long> result = new ArrayList<>();
        for (String searchText : searchTexts) {
            result.addAll(findContains(searchText, tokenMap(fieldType), fieldType));
        }
        return result;
    }

    public synchronized List<Long> findIdsContains(Map<String, List<String>> searchTexts) {
        if (searchTexts.isEmpty()) return Collections.emptyList();

        List<Long> result = new ArrayList<>();
        if (searchTexts.containsKey(ArgumentsParser.MATERIAL_TYPE)) {
            result.addAll(findIdsByTypeNameContains(searchTexts.get(ArgumentsParser.MATERIAL_TYPE), FieldType.LOWER_TYPE_NAME));
        }
        if (searchTexts.containsKey(ArgumentsParser.DISPLAY)) {
            result.addAll(findIdsByTypeNameContains(searchTexts.get(ArgumentsParser.DISPLAY), FieldType.LOWER_DISPLAY_NAME));
        }
        if (searchTexts.containsKey(ArgumentsParser.LORE)) {
            result.addAll(findIdsByTypeNameContains(searchTexts.get(ArgumentsParser.LORE), FieldType.LOWER_LORE));
        }

        return result;
    }

    private Map<String, IntSet> tokenMap(FieldType fieldType) {
        switch (fieldType) {
            case DISPLAY_NAME:
            case LOWER_DISPLAY_NAME:
                return displayNameTokens;
            case LORE:
            case LOWER_LORE:
                return loreTokens;
            case TYPE_NAME:
            case LOWER_TYPE_NAME:
            default:
                return typeNameTokens;
        }
    }

    public synchronized ItemTemplate[] items() {
        return allItems.values().toArray(new ItemTemplate[0]);
    }

    public synchronized int size() {
        return allItems.size();
    }

    public enum FieldType {DISPLAY_NAME, LORE, TYPE_NAME, LOWER_DISPLAY_NAME, LOWER_LORE, LOWER_TYPE_NAME}

    private static class IntSet {

        private int[] keys;
        private boolean[] allocated;
        private int size;

        public IntSet() {
            this(16);
        }

        public IntSet(int capacity) {
            capacity = nextPowerOfTwo(capacity);
            keys = new int[capacity];
            allocated = new boolean[capacity];
            size = 0;
        }

        private static int nextPowerOfTwo(int n) {
            n--;
            n |= n >> 1;
            n |= n >> 2;
            n |= n >> 4;
            n |= n >> 8;
            n |= n >> 16;
            return n + 1;
        }

        public void add(int key) {
            float loadFactor = 0.75f;
            if (size >= keys.length * loadFactor) {
                resize();
            }

            int slot = key & (keys.length - 1);
            while (allocated[slot]) {
                if (keys[slot] == key) return;
                slot = (slot + 1) & (keys.length - 1);
            }

            keys[slot] = key;
            allocated[slot] = true;
            size++;
        }

        public int[] toArray() {
            int[] result = new int[size];
            int idx = 0;
            for (int i = 0; i < allocated.length; i++) {
                if (allocated[i]) {
                    result[idx++] = keys[i];
                }
            }
            return result;
        }

        private void resize() {

            int[] oldKeys = keys;
            boolean[] oldAllocated = allocated;

            keys = new int[oldKeys.length * 2];
            allocated = new boolean[oldAllocated.length * 2];
            size = 0;

            for (int i = 0; i < oldAllocated.length; i++) {
                if (oldAllocated[i]) {
                    add(oldKeys[i]);
                }
            }

        }

    }

    private static class IndexEntry {

        final long hash;
        final int position;
        IndexEntry(long hash, int position) {
            this.hash = hash;
            this.position = position;
        }

    }

}
