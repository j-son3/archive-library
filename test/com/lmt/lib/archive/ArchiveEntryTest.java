package com.lmt.lib.archive;

import static org.junit.Assert.*;

import java.nio.file.Path;

import org.junit.Test;

public class ArchiveEntryTest {
	private static class EntryImpl extends ArchiveEntry {
		EntryImpl() {
			this.owner = null;
			this.index = 0;
			this.isLocation = false;
			this.isContent = true;
			this.path = Path.of("com", "lmt", "lib", "common", "archive", "data.bin");
			this.size = 500;
			this.lastModified = 1000;
		}
	}

	// isLocation()
	// 期待する値を取得できること
	@Test
	public void testIsLocation() {
		var entry = new EntryImpl();
		assertFalse(entry.isLocation());
	}

	// isContent()
	// 期待する値を取得できること
	@Test
	public void testIsContent() {
		var entry = new EntryImpl();
		assertTrue(entry.isContent());
	}

	// getPath()
	// 期待する値を取得できること
	@Test
	public void testGetPath() {
		var entry = new EntryImpl();
		assertEquals(Path.of("com", "lmt", "lib", "common", "archive", "data.bin"), entry.getPath());
	}

	// getSize()
	// 期待する値を取得できること
	@Test
	public void testGetSize() {
		var entry = new EntryImpl();
		assertEquals(500L, entry.getSize());
	}

	// getLastModified()
	// 期待する値を取得できること
	@Test
	public void testGetLastModified() {
		var entry = new EntryImpl();
		assertEquals(1000L, entry.getLastModified());
	}
}
