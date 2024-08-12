package com.lmt.lib.archive;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.After;
import org.junit.Test;

import com.lmt.lib.archive.impl.FolderArchive;
import com.lmt.lib.archive.impl.SzjbArchive;

public class ArchiveManagerTest {
	@After
	public void tearDown() throws Exception {
		// 1件テストが完了するごとに未初期化状態に戻す
		var am = ArchiveManager.getInstance();
		Tests.setf(am, "mIsInitialized", false);
		Tests.setf(am, "mTypeTester", null);
	}

	// getInstance()
	// インスタンスが取得され、2回目以降も同じインスタンスを返すこと
	@Test
	public void testGetInstance() {
		var am1 = ArchiveManager.getInstance();
		var am2 = ArchiveManager.getInstance();
		assertNotNull(am1);
		assertSame(am1, am2);
	}

	// initialize(Function)
	// 初回呼び出しでアーカイブ種別判定処理が登録され、初期化済みになること
	@Test
	public void testInitialize_Normal() throws Exception {
		Function<Path, ArchiveType> judgement = p -> null;
		var am = ArchiveManager.getInstance();
		am.initialize(judgement);
		assertTrue(Tests.getf(am, "mIsInitialized"));
		assertSame(judgement, Tests.getf(am, "mTypeTester"));
	}

	// initialize(Function)
	// IllegalStateException アーカイブマネージャが初期化済み
	@Test
	public void testInitialize_AlreadyInitialized() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize(p -> null);
		assertThrows(IllegalStateException.class, () -> am.initialize(p -> null));
	}

	// initialize(Function)
	// NullPointerException typeTesterがnull
	@Test
	public void testInitialize_NullTypeJudgement() throws Exception {
		var am = ArchiveManager.getInstance();
		assertThrows(NullPointerException.class, () -> am.initialize(null));
	}

	// initialize()
	// 初回呼び出しでアーカイブ種別判定処理(標準の種別判定処理)が登録され、初期化済みになること
	@Test
	public void testInitialize2_Normal() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertTrue(Tests.getf(am, "mIsInitialized"));
		assertEquals(StandardTypeTester.class, Tests.getf(am, "mTypeTester").getClass());
	}

	// initialize()
	// IllegalStateException アーカイブマネージャが初期化済み
	@Test
	public void testInitialize2_AlreadyInitialized() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertThrows(IllegalStateException.class, () -> am.initialize());
	}

	// isInitialized()
	// 初期化前に呼び出すとfalseを返すこと
	@Test
	public void testIsInitialized_BeforeInitialize() {
		var am = ArchiveManager.getInstance();
		assertFalse(am.isInitialized());
	}

	// isInitialized()
	// 初期化後に呼び出すとtrueを返すこと
	@Test
	public void testIsInitialized_AfterInitialize() {
		var am = ArchiveManager.getInstance();
		am.initialize(p -> null);
		assertTrue(am.isInitialized());
	}

	// getArchiveType(Path)
	// 初期化で設定したアーカイブ種別判定処理が実行され、その処理で返した戻り値を返すこと
	@Test
	public void testGetArchiveType_Normal() {
		var ran = new AtomicBoolean(false);
		var am = ArchiveManager.getInstance();
		am.initialize(p -> { ran.set(true); return ArchiveType.ZIP; });
		assertEquals(ArchiveType.ZIP, am.getArchiveType(Path.of("test")));
		assertTrue(ran.get());
	}

	// getArchiveType(Path)
	// アーカイブ種別判定処理がスローした例外がそのままスローされること
	@Test
	public void testGetArchiveType_ThrownException() {
		var exc = new IllegalArgumentException();
		var am = ArchiveManager.getInstance();
		am.initialize(p -> { throw exc; });
		assertThrows(exc.getClass(), () -> am.getArchiveType(Path.of("test")));
	}

	// getArchiveType(Path)
	// NullPointerException pathがnull
	@Test
	public void testGetArchiveType_NullPath() {
		var ran = new AtomicBoolean(false);
		var am = ArchiveManager.getInstance();
		am.initialize(p -> { ran.set(true); return ArchiveType.ZIP; });
		assertThrows(NullPointerException.class, () -> am.getArchiveType(null));
		assertFalse(ran.get());
	}

	// getArchiveType(Path)
	// IllegalStateException アーカイブマネージャが初期化されていない
	@Test
	public void testGetArchiveType_NotInitialized() {
		var am = ArchiveManager.getInstance();
		assertThrows(IllegalStateException.class, () -> am.getArchiveType(Path.of("test")));
	}

	// open(Path)
	// アーカイブ種別判定処理が返したアーカイブ種別に応じたアーカイブオブジェクトが構築されること
	@Test
	public void testOpen_Success() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		try (var archive = am.open(TestData.FOLDER_ARCHIVE)) {
			assertEquals(FolderArchive.class, archive.getClass());
		}
		try (var archive = am.open(TestData.ZIP_ARCHIVE)) {
			assertEquals(SzjbArchive.Zip.class, archive.getClass());
		}
		try (var archive = am.open(TestData.RAR4_ARCHIVE)) {
			assertEquals(SzjbArchive.Rar.class, archive.getClass());
		}
		try (var archive = am.open(TestData.RAR5_ARCHIVE)) {
			assertEquals(SzjbArchive.Rar.class, archive.getClass());
		}
		try (var archive = am.open(TestData.SEVEN_ZIP_ARCHIVE)) {
			assertEquals(SzjbArchive.SevenZip.class, archive.getClass());
		}
	}

	// open(Path)
	// IllegalStateException アーカイブマネージャが初期化されていない
	@Test
	public void testOpen_NotInitialized() throws Exception {
		var am = ArchiveManager.getInstance();
		assertThrows(IllegalStateException.class, () -> am.open(TestData.FOLDER_ARCHIVE));
	}

	// open(Path)
	// NullPointerException pathがnull
	@Test
	public void testOpen_NullPath() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertThrows(NullPointerException.class, () -> am.open(null));
	}

	// open(Path)
	// NoSuchFileException 指定パスのアーカイブが見つからない
	@Test
	public void testOpen_NotFound() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertThrows(NoSuchFileException.class, () -> am.open(TestData.LOCATION.resolve("_NOT_FOUND_.zip")));
	}

	// open(Path)
	// IOException アーカイブ種別判定中に任意の例外がスローされた
	@Test
	public void testOpen_ThrownInJudgement() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize(p -> { throw new RuntimeException("TEST"); });
		var cause = assertThrows(IOException.class, () -> am.open(TestData.FOLDER_ARCHIVE)).getCause();
		assertEquals(RuntimeException.class, cause.getClass());
		assertEquals("TEST", cause.getMessage());
	}

	// open(Path)
	// IOException アーカイブ種別判定結果がUNKNOWN
	@Test
	public void testOpen_UnknownArchiveType() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertThrows(IOException.class, () -> am.open(TestData.LOCATION.resolve("_UNKNOWN_")));
	}

	// open(Path)
	// IOException アーカイブ種別判定結果がnull
	@Test
	public void testOpen_NullArchiveType() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize(p -> null);
		assertThrows(IOException.class, () -> am.open(TestData.FOLDER_ARCHIVE));
	}

	// open(Path)
	// IOException 指定パスのフォルダ・ファイルの読み取り権限がない
	@Test
	public void testOpen_Deny() throws Exception {
		// 読み取り権限のないファイルの作成が困難なため実施保留
	}

	// open(Path)
	// IOException 指定パスのフォルダ・ファイルがアーカイブとして認識できない
	@Test
	public void testOpen_BrokenArchive() throws Exception {
		var am = ArchiveManager.getInstance();
		am.initialize();
		assertThrows(IOException.class, () -> am.open(TestData.BROKEN_ARCHIVE));
	}
}
