package com.redis.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.redis.dto.ItemDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@EnableCaching
public class ItemService {

    private final List<ItemDTO> items = new ArrayList<>();
    private final String HASH_KEY = "item";
    private Long currentId = 1L;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    private void initDummyData() {
        ItemDTO item1 = new ItemDTO();
        item1.setId(currentId++);
        item1.setName("Item 1");
        item1.setDescription("Description for Item 1");
        items.add(item1);

        ItemDTO item2 = new ItemDTO();
        item2.setId(currentId++);
        item2.setName("Item 2");
        item2.setDescription("Description for Item 2");
        items.add(item2);

        ItemDTO item3 = new ItemDTO();
        item3.setId(currentId++);
        item3.setName("Item 3");
        item3.setDescription("Description for Item 3");
        items.add(item3);
    }

    @Cacheable(value = "item")
    public List<ItemDTO> getAllItems() {
        System.out.println("Called");
        simulateSlowService();
        return items;
    }

    @Cacheable(value = "item", key = "#id")
    public Optional<ItemDTO> getItemById(Long id) {
        System.out.println("Called");
        simulateSlowService();
        return items.stream().filter(item -> item.getId().equals(id)).findFirst();
    }

    @CachePut(value = "item",  key = "#itemDTO.id")
    public ItemDTO createItem(ItemDTO itemDTO) {
        System.out.println("Called");
        itemDTO.setId(currentId++);
        items.add(itemDTO);
        return itemDTO;
    }

    @CachePut(value = "item", key = "#id")
    public Optional<ItemDTO> updateItem(Long id, ItemDTO updatedItem) {
        System.out.println("Called");
        return getItemById(id).map(item -> {
            item.setName(updatedItem.getName());
            item.setDescription(updatedItem.getDescription());
            return item;
        });
    }

    @CacheEvict(value = "item", key = "#id")
    public boolean deleteItem(Long id) {
        System.out.println("Called");
        return items.removeIf(item -> item.getId().equals(id));
    }

    public String getCachedDataAsJson(Long key) {
        Cache cache = cacheManager.getCache("item");
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                try {
                    return objectMapper.writeValueAsString(valueWrapper.get());
                } catch (Exception e) {
                    throw new RuntimeException("Error converting cached value to JSON", e);
                }
            }
        }
        return "{}";
    }

    public String getAllCachedDataAsJson() {
        Cache cache = cacheManager.getCache("item");
        if (cache == null) {
            return "{}";
        }
        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof Map) {
            Map<Object, Object> cacheEntries = (Map<Object, Object>) nativeCache;
            try {
                return objectMapper.writeValueAsString(cacheEntries);
            } catch (Exception e) {
                throw new RuntimeException("Error converting cached entries to JSON", e);
            }
        }

        return "{}";
    }

    private void simulateSlowService() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
