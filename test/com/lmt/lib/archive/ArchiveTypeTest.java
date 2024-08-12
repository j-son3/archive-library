package com.lmt.lib.archive;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArchiveTypeTest {
	// isFolderArchive()
	// アーカイブ種別ごとに期待する値が返ること
	@Test
	public void testIsFolderArchive() {
		assertTrue(ArchiveType.FOLDER.isFolderArchive());
		assertFalse(ArchiveType.ZIP.isFolderArchive());
		assertFalse(ArchiveType.SEVEN_ZIP.isFolderArchive());
		assertFalse(ArchiveType.RAR.isFolderArchive());
		assertFalse(ArchiveType.UNKNOWN.isFolderArchive());
	}

	// isFileArchive()
	// アーカイブ種別ごとに期待する値が返ること
	@Test
	public void testIsFileArchive() {
		assertFalse(ArchiveType.FOLDER.isFileArchive());
		assertTrue(ArchiveType.ZIP.isFileArchive());
		assertTrue(ArchiveType.SEVEN_ZIP.isFileArchive());
		assertTrue(ArchiveType.RAR.isFileArchive());
		assertFalse(ArchiveType.FOLDER.isFileArchive());
	}

	// isUnknown()
	// アーカイブ種別ごとに期待する値が返ること
	@Test
	public void testIsUnknown() {
		assertFalse(ArchiveType.FOLDER.isUnknown());
		assertFalse(ArchiveType.ZIP.isUnknown());
		assertFalse(ArchiveType.SEVEN_ZIP.isUnknown());
		assertFalse(ArchiveType.RAR.isUnknown());
		assertTrue(ArchiveType.UNKNOWN.isUnknown());
	}

	// open(Path)
	// アーカイブ種別ごとに期待するクラスのアーカイブオブジェクトが構築されること
	@Test
	public void testOpen() {
		// TODO
	}
}
