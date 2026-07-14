package com.ensemble.wardrobe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ensemble.storage.PhotoStorage;
import com.ensemble.wardrobe.dto.ItemMapper;
import com.ensemble.wardrobe.dto.TagRequest;

/**
 * Wardrobe business logic: coordinates the repository (item records) and photo
 * storage. Owns id generation, the derived photo key, and the not-found rule
 * that every id-based operation shares. Callers work with {@link Item} and
 * {@link TagRequest}; the controller maps to/from DTOs.
 */
@Service
public class WardrobeService {

	private final WardrobeRepository repository;
	private final PhotoStorage photoStorage;

	public WardrobeService(WardrobeRepository repository, PhotoStorage photoStorage) {
		this.repository = repository;
		this.photoStorage = photoStorage;
	}

	/**
	 * Creates an item: generates the id, stores the (compressed) photo under a
	 * derived key, and persists the record. The photo is validated by storage —
	 * invalid bytes raise {@code InvalidImageException}.
	 */
	public Item create(TagRequest tags, byte[] photoBytes) {
		String itemId = UUID.randomUUID().toString();
		String photoKey = itemId + ".jpg";
		photoStorage.save(photoKey, photoBytes);

		Item item = new Item();
		item.setItemId(itemId);
		item.setPhotoKey(photoKey);
		item.setCreatedAt(Instant.now());
		item.setWornCount(0);
		ItemMapper.applyTags(item, tags);
		return repository.save(item);
	}

	public List<Item> list() {
		return repository.findAll();
	}

	/** Returns the item or throws {@link ItemNotFoundException}. */
	public Item get(String itemId) {
		return repository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
	}

	/** Replaces the tag fields of an existing item. */
	public Item updateTags(String itemId, TagRequest tags) {
		Item item = get(itemId);
		ItemMapper.applyTags(item, tags);
		return repository.save(item);
	}

	/** Removes an existing item and its photo. */
	public void delete(String itemId) {
		Item item = get(itemId);
		photoStorage.delete(item.getPhotoKey());
		repository.deleteById(itemId);
	}

	/** Loads the stored photo bytes for an existing item. */
	public byte[] loadPhoto(String itemId) {
		Item item = get(itemId);
		return photoStorage.load(item.getPhotoKey());
	}
}
