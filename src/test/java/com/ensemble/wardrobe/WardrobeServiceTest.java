package com.ensemble.wardrobe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ensemble.storage.PhotoStorage;
import com.ensemble.wardrobe.dto.TagRequest;

@ExtendWith(MockitoExtension.class)
class WardrobeServiceTest {

	@Mock
	WardrobeRepository repository;

	@Mock
	PhotoStorage photoStorage;

	@InjectMocks
	WardrobeService service;

	private TagRequest tags() {
		return new TagRequest("top", "navy", "white", 3, "striped", 2, List.of("cotton"));
	}

	private Item existing(String id) {
		Item item = new Item();
		item.setItemId(id);
		item.setPhotoKey(id + ".jpg");
		return item;
	}

	@Test
	void create_generatesIdStoresPhotoAndPersistsItem() {
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Item created = service.create(tags(), new byte[]{1, 2, 3});

		assertThat(created.getItemId()).isNotBlank();
		assertThat(created.getPhotoKey()).isEqualTo(created.getItemId() + ".jpg");
		assertThat(created.getCreatedAt()).isNotNull();
		assertThat(created.getWornCount()).isZero();
		assertThat(created.getCategory()).isEqualTo("top");

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(photoStorage).save(keyCaptor.capture(), eq(new byte[]{1, 2, 3}));
		assertThat(keyCaptor.getValue()).isEqualTo(created.getItemId() + ".jpg");
		verify(repository).save(created);
	}

	@Test
	void list_returnsAllItems() {
		when(repository.findAll()).thenReturn(List.of(existing("a"), existing("b")));

		assertThat(service.list()).extracting(Item::getItemId).containsExactly("a", "b");
	}

	@Test
	void get_whenPresent_returnsItem() {
		when(repository.findById("x")).thenReturn(Optional.of(existing("x")));

		assertThat(service.get("x").getItemId()).isEqualTo("x");
	}

	@Test
	void get_whenMissing_throwsItemNotFound() {
		when(repository.findById("nope")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ItemNotFoundException.class)
			.isThrownBy(() -> service.get("nope"));
	}

	@Test
	void updateTags_appliesTagsAndSaves() {
		when(repository.findById("x")).thenReturn(Optional.of(existing("x")));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Item updated = service.updateTags("x", tags());

		assertThat(updated.getCategory()).isEqualTo("top");
		assertThat(updated.getWarmth()).isEqualTo(2);
		verify(repository).save(updated);
	}

	@Test
	void updateTags_whenMissing_throwsItemNotFound() {
		when(repository.findById("nope")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ItemNotFoundException.class)
			.isThrownBy(() -> service.updateTags("nope", tags()));
		verify(repository, never()).save(any());
	}

	@Test
	void delete_removesPhotoThenItem() {
		when(repository.findById("x")).thenReturn(Optional.of(existing("x")));

		service.delete("x");

		verify(photoStorage).delete("x.jpg");
		verify(repository).deleteById("x");
	}

	@Test
	void delete_whenMissing_throwsAndTouchesNothing() {
		when(repository.findById("nope")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ItemNotFoundException.class)
			.isThrownBy(() -> service.delete("nope"));
		verify(photoStorage, never()).delete(any());
		verify(repository, never()).deleteById(any());
	}

	@Test
	void loadPhoto_returnsBytesFromStorage() {
		when(repository.findById("x")).thenReturn(Optional.of(existing("x")));
		when(photoStorage.load("x.jpg")).thenReturn(new byte[]{9, 9});

		assertThat(service.loadPhoto("x")).containsExactly(9, 9);
	}

	@Test
	void loadPhoto_whenMissing_throwsItemNotFound() {
		when(repository.findById("nope")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ItemNotFoundException.class)
			.isThrownBy(() -> service.loadPhoto("nope"));
		verify(photoStorage, never()).load(any());
	}
}
