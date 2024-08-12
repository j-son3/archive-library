package com.lmt.lib.archive;

import static org.junit.Assert.*;

import org.junit.Test;

public class StandardTypeTesterTest {
	// apply(Path)
	// 実在するフォルダを指定するとフォルダアーカイブと判定されること
	@Test
	public void testApply_ExistFolder() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.FOLDER, tester.apply(TestData.LOCATION));
	}

	// apply(Path)
	// 実在する".zip"で終わるフォルダを指定するとフォルダアーカイブと判定されること
	@Test
	public void testApply_ExistZipNameFolder() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.FOLDER, tester.apply(TestData.ZIP_NAME_FOLDER));
	}

	// apply(Path)
	// 実在する".rar"で終わるフォルダを指定するとフォルダアーカイブと判定されること
	@Test
	public void testApply_ExistRarNameFolder() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.FOLDER, tester.apply(TestData.RAR_NAME_FOLDER));
	}

	// apply(Path)
	// 実在する".7z"で終わるフォルダを指定するとフォルダアーカイブと判定されること
	@Test
	public void testApply_Exist7zNameFolder() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.FOLDER, tester.apply(TestData.SEVEN_ZIP_NAME_FOLDER));
	}

	// apply(Path)
	// フォルダではない名前(ファイル有無に関係なく拡張子なし、または不明な拡張子)を指定すると不明と判定されること
	@Test
	public void testApply_UnsupportedName() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.UNKNOWN, tester.apply(TestData.NON_EXTENSION_FILE));
		assertEquals(ArchiveType.UNKNOWN, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("not_found")));
		assertEquals(ArchiveType.UNKNOWN, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.xxx")));
	}

	// apply(Path)
	// 存在有無に関わらず".zip"(英字大小無視)で終わる名前のパスを指定するとZIPファイルアーカイブと判定されること
	@Test
	public void testApply_ZipNameFile() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.ZIP, tester.apply(TestData.ZIP_ARCHIVE));
		assertEquals(ArchiveType.ZIP, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.zip")));
		assertEquals(ArchiveType.ZIP, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.ZiP")));
	}

	// apply(Path)
	// 存在有無に関わらず".rar"(英字大小無視)で終わる名前のパスを指定するとZIPファイルアーカイブと判定されること
	@Test
	public void testApply_RarNameFile() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.RAR, tester.apply(TestData.RAR4_ARCHIVE));
		assertEquals(ArchiveType.RAR, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.rar")));
		assertEquals(ArchiveType.RAR, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.RaR")));
	}

	// apply(Path)
	// 存在有無に関わらず".7z"(英字大小無視)で終わる名前のパスを指定するとZIPファイルアーカイブと判定されること
	@Test
	public void testApply_7zNameFile() {
		var tester = new StandardTypeTester();
		assertEquals(ArchiveType.SEVEN_ZIP, tester.apply(TestData.SEVEN_ZIP_ARCHIVE));
		assertEquals(ArchiveType.SEVEN_ZIP, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.7z")));
		assertEquals(ArchiveType.SEVEN_ZIP, tester.apply(TestData.STANDARD_TYPE_TESTER_LOCATION.resolve("file.7Z")));
	}
}
