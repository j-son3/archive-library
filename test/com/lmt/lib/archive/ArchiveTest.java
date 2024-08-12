package com.lmt.lib.archive;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class ArchiveTest {
	protected abstract ArchiveType expectedArchiveType();
	protected abstract Path expectedPath();

	// 各テストケースではこのアーカイブでテストすること
	private Archive mArchive = null;

	private static class DummyArchive extends Archive {
		static final DummyArchive INSTANCE = new DummyArchive();
		DummyArchive() { super(ArchiveType.UNKNOWN, Path.of("")); }
		@Override protected void onOpenArchive(Path path) throws IOException {}
		@Override protected void onCloseArchive() throws IOException {}
		@Override protected InputStream onOpenContentByEntry(ArchiveEntry entry) throws IOException { return null; }
		@Override protected InputStream onOpenContentByIndex(int index) throws IOException { return null; }
		@Override protected InputStream onOpenContentByPath(Path path) throws IOException { return null; }
		@Override protected byte[] onReadAllBytesByEntry(ArchiveEntry entry) throws IOException { return null; }
		@Override protected byte[] onReadAllBytesByIndex(int index) throws IOException { return null; }
		@Override protected byte[] onReadAllBytesByPath(Path path) throws IOException { return null; }
		@Override protected ArchiveEntry onGetEntryByIndex(int index) { return null; }
		@Override protected ArchiveEntry onGetEntryByPath(Path path) { return null; }
		@Override protected void onEnumEntries(EntryCallback callback) throws IOException {}
		@Override protected int onGetEntryCount() { return 0; }
		@Override protected int onGetCapability() { return 0; }
	}

	private static class DummyEntry extends ArchiveEntry {
		static final DummyEntry INSTANCE = new DummyEntry();
		DummyEntry() {
			this.owner = DummyArchive.INSTANCE;
			this.index = 0;
			this.isContent = true;
			this.isLocation = false;
			this.path = TestData.CONTENT_ASCII_TXT_PATH;
			this.size = 1;
			this.lastModified = 0;
		}
	}

	// 当テストクラスのテスト開始前にArchiveManagerを初期化しておく
	@BeforeClass
	public static void setupClass() {
		ArchiveManager.getInstance().initialize();
	}

	// 当テストクラスのテスト終了後にArchiveManagerを未初期化状態に戻しておく
	@AfterClass
	public static void tearDownClass() throws Exception {
		var am = ArchiveManager.getInstance();
		Tests.setf(am, "mIsInitialized", false);
		Tests.setf(am, "mTypeTester", null);
	}

	// 各テストケース開始前にアーカイブを開く
	@Before
	public void setup() throws Exception {
		mArchive = ArchiveManager.getInstance().open(expectedPath());
	}

	// 各テストケース終了後にアーカイブを閉じる
	@After
	public void tearDown() throws Exception {
		var archive = mArchive;
		if (archive != null) {
			mArchive = null;
			archive.close();
		}
	}

	// getArchiveType()
	// 開いたアーカイブのアーカイブ種別が取得できること
	@Test
	public void testGetArchiveType() {
		assertEquals(expectedArchiveType(), mArchive.getArchiveType());
	}

	// getPath()
	// 開いたアーカイブの絶対パスが取得され、オープン状態に限らず呼び出し可能であること
	@Test
	public void testGetPath() throws Exception {
		var absolutePath = expectedPath().toAbsolutePath();
		assertEquals(absolutePath, mArchive.getPath());
		mArchive.close();
		assertEquals(absolutePath, mArchive.getPath());
	}

	// isOpen()
	// オープン状態に応じた値が取得できること
	@Test
	public void testIsOpen() throws Exception {
		assertTrue(mArchive.isOpen());
		mArchive.close();
		assertFalse(mArchive.isOpen());
	}

	// isCached()
	// オープン直後はfalseを返し、キャッシュを実行するとtrueが返ること
	@Test
	public void testIsCached_Normal() throws Exception {
		assertFalse(mArchive.isCached());
		mArchive.cacheEntries();
		assertTrue(mArchive.isCached());
	}

	// isCached()
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testIsCached_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.isCached());
	}

	// openContent(int)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testOpenContentByIndex_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			try (var stream = mArchive.openContent(getIndexByPath(c.getKey()))) {
				assertArrayEquals(c.getValue(), stream.readAllBytes());
			}
		}
	}

	// openContent(int)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testOpenContentByIndex_CacheBeforeAfter() throws Exception {
		// 本テストは最初からインデックス値によるコンテンツアクセスが可能なアーカイブでのみ試験する
		if (mArchive.canUseIndex()) {
			for (var c : TestData.ALL_CONTENTS.entrySet()) {
				try (var stream = mArchive.openContent(getIndexByPath(c.getKey()))) {
					assertArrayEquals(c.getValue(), stream.readAllBytes());
				}
			}
			mArchive.cacheEntries();
			for (var c : TestData.ALL_CONTENTS.entrySet()) {
				try (var stream = mArchive.openContent(getIndexByPath(c.getKey()))) {
					assertArrayEquals(c.getValue(), stream.readAllBytes());
				}
			}
		}
	}

	// openContent(int)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testOpenContentByIndex_NotOpen() throws Exception {
		var index = getIndexByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.openContent(index));
	}

	// openContent(int)
	// IllegalStateException インデックス値によるコンテンツアクセスが不可
	@Test
	public void testOpenContentByIndex_CantUseIndex() throws Exception {
		// 本テストはインデックス値によるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUseIndex()) {
			assertThrows(IllegalStateException.class, () -> mArchive.openContent(0));
		}
	}

	// openContent(int)
	// IndexOutOfBoundsException インデックス値が0未満またはエントリ総数以上
	@Test
	public void testOpenContentByIndex_IndexOutOfRange() throws Exception {
		var exc = IndexOutOfBoundsException.class;
		mArchive.cacheEntries();
		assertThrows(exc, () -> mArchive.openContent(-1));
		assertThrows(exc, () -> mArchive.openContent(mArchive.getEntryCount()));
	}

	// openContent(int)
	// NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	@Test
	public void testOpenContentByIndex_NotContent() throws Exception {
		var index = getIndexByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.openContent(index));
	}

	// openContent(Path)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testOpenContentByPath_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			try (var stream = mArchive.openContent(getPathByPath(c.getKey()))) {
				assertArrayEquals(c.getValue(), stream.readAllBytes());
			}
		}
	}

	// openContent(Path)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testOpenContentByPath_CacheBeforeAfter() throws Exception {
		// 本テストは最初からパスによるコンテンツアクセスが可能なアーカイブでのみ試験する
		if (mArchive.canUsePath()) {
			for (var c : TestData.ALL_CONTENTS.entrySet()) {
				try (var stream = mArchive.openContent(getPathByPath(c.getKey()))) {
					assertArrayEquals(c.getValue(), stream.readAllBytes());
				}
			}
			mArchive.cacheEntries();
			for (var c : TestData.ALL_CONTENTS.entrySet()) {
				try (var stream = mArchive.openContent(getPathByPath(c.getKey()))) {
					assertArrayEquals(c.getValue(), stream.readAllBytes());
				}
			}
		}
	}

	// openContent(Path)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testOpenContentByPath_NotOpen() throws Exception {
		var path = getPathByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.openContent(path));
	}

	// openContent(Path)
	// IllegalStateException パスによるコンテンツアクセスが不可
	@Test
	public void testOpenContentByPath_CantUsePath() throws Exception {
		// 本テストはパスによるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUsePath()) {
			var path = TestData.CONTENT_ASCII_TXT_PATH;
			assertThrows(IllegalStateException.class, () -> mArchive.openContent(path));
		}
	}

	// openContent(Path)
	// NullPointerException pathがnull
	@Test
	public void testOpenContentByPath_NullPath() throws Exception {
		mArchive.cacheEntries();
		assertThrows(NullPointerException.class, () -> mArchive.openContent((Path)null));
	}

	// openContent(Path)
	// NoSuchFileException 指定したパスのエントリが見つからない
	@Test
	public void testOpenContentByPath_NotFound() throws Exception {
		var path = getPathByPath(Path.of("not_found"));
		assertThrows(NoSuchFileException.class, () -> mArchive.openContent(path));
	}

	// openContent(Path)
	// NoSuchFileException 指定したパスのエントリがコンテンツではない
	@Test
	public void testOpenContentByPath_NotContent() throws Exception {
		var path = getPathByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.openContent(path));
	}

	// openContent(ArchiveEntry)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testOpenContentByEntry_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			try (var stream = mArchive.openContent(getEntryByPath(c.getKey()))) {
				assertArrayEquals(c.getValue(), stream.readAllBytes());
			}
		}
	}

	// openContent(ArchiveEntry)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testOpenContentByEntry_CacheBeforeAfter() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			try (var stream = mArchive.openContent(getEntryByPath(c.getKey()))) {
				assertArrayEquals(c.getValue(), stream.readAllBytes());
			}
		}
		mArchive.cacheEntries();
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			try (var stream = mArchive.openContent(getEntryByPath(c.getKey()))) {
				assertArrayEquals(c.getValue(), stream.readAllBytes());
			}
		}
	}

	// openContent(ArchiveEntry)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testOpenContentByEntry_NotOpen() throws Exception {
		var entry = getEntryByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.openContent(entry));
	}

	// openContent(ArchiveEntry)
	// NullPointerException entryがnull
	@Test
	public void testOpenContentByEntry_NullEntry() throws Exception {
		assertThrows(NullPointerException.class, () -> mArchive.openContent((ArchiveEntry)null));
	}

	// openContent(ArchiveEntry)
	// IllegalArgumentException 他インスタンスが生成したエントリを指定した
	@Test
	public void testOpenContentByEntry_BadOwner() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> mArchive.openContent(DummyEntry.INSTANCE));
	}

	// openContent(ArchiveEntry)
	// NoSuchFileException 指定したエントリがコンテンツではない
	@Test
	public void testOpenContentByEntry_NotContent() throws Exception {
		var entry = getEntryByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.openContent(entry));
	}

	// readAllBytes(int)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testReadAllBytesByIndex_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getIndexByPath(c.getKey())));
		}
	}

	// readAllBytes(int)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testReadAllBytesByIndex_CacheBeforeAfter() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getIndexByPath(c.getKey())));
		}
		mArchive.cacheEntries();
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getIndexByPath(c.getKey())));
		}
	}

	// readAllBytes(int)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testReadAllBytesByIndex_NotOpen() throws Exception {
		var index = getIndexByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.readAllBytes(index));
	}

	// readAllBytes(int)
	// IllegalStateException インデックス値によるコンテンツアクセスが不可
	@Test
	public void testReadAllBytesByIndex_CantUseIndex() throws Exception {
		// 本テストはインデックス値によるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUseIndex()) {
			assertThrows(IllegalStateException.class, () -> mArchive.readAllBytes(0));
		}
	}

	// readAllBytes(int)
	// IndexOutOfBoundsException インデックス値が0未満またはエントリ総数以上
	@Test
	public void testReadAllBytesByIndex_IndexOutOfRange() throws Exception {
		var exc = IndexOutOfBoundsException.class;
		mArchive.cacheEntries();
		assertThrows(exc, () -> mArchive.readAllBytes(-1));
		assertThrows(exc, () -> mArchive.readAllBytes(mArchive.getEntryCount()));
	}

	// readAllBytes(int)
	// NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	@Test
	public void testReadAllBytesByIndex_NotContent() throws Exception {
		var index = getIndexByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.readAllBytes(index));
	}

	// readAllBytes(Path)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testReadAllBytesByPath_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getPathByPath(c.getKey())));
		}
	}

	// readAllBytes(Path)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testReadAllBytesByPath_CacheBeforeAfter() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getPathByPath(c.getKey())));
		}
		mArchive.cacheEntries();
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getPathByPath(c.getKey())));
		}
	}

	// readAllBytes(Path)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testReadAllBytesByPath_NotOpen() throws Exception {
		var path = getPathByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.readAllBytes(path));
	}

	// readAllBytes(Path)
	// IllegalStateException パスによるコンテンツアクセスが不可
	@Test
	public void testReadAllBytesByPath_CantUsePath() throws Exception {
		// 本テストはパスによるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUsePath()) {
			var path = TestData.CONTENT_ASCII_TXT_PATH;
			assertThrows(IllegalStateException.class, () -> mArchive.readAllBytes(path));
		}
	}

	// readAllBytes(Path)
	// NullPointerException pathがnull
	@Test
	public void testReadAllBytesByPath_NullPath() throws Exception {
		mArchive.cacheEntries();
		assertThrows(NullPointerException.class, () -> mArchive.readAllBytes((Path)null));
	}

	// readAllBytes(Path)
	// NoSuchFileException 指定したパスのエントリが見つからない
	@Test
	public void testReadAllBytesByPath_NotFound() throws Exception {
		var path = getPathByPath(Path.of("not_found"));
		assertThrows(NoSuchFileException.class, () -> mArchive.readAllBytes(path));
	}

	// readAllBytes(Path)
	// NoSuchFileException 指定したパスのエントリがコンテンツではない
	@Test
	public void testReadAllBytesByPath_NotContent() throws Exception {
		var path = getPathByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.readAllBytes(path));
	}

	// readAllBytes(ArchiveEntry)
	// ストリームから解凍後のデータが正しく取り出せること
	@Test
	public void testReadAllBytesByEntry_Extract() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getEntryByPath(c.getKey())));
		}
	}

	// readAllBytes(ArchiveEntry)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testReadAllBytesByEntry_CacheBeforeAfter() throws Exception {
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getEntryByPath(c.getKey())));
		}
		mArchive.cacheEntries();
		for (var c : TestData.ALL_CONTENTS.entrySet()) {
			assertArrayEquals(c.getValue(), mArchive.readAllBytes(getEntryByPath(c.getKey())));
		}
	}

	// readAllBytes(ArchiveEntry)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testReadAllBytesByEntry_NotOpen() throws Exception {
		var entry = getEntryByPath(TestData.CONTENT_ASCII_TXT_PATH);
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.readAllBytes(entry));
	}

	// readAllBytes(ArchiveEntry)
	// NullPointerException entryがnull
	@Test
	public void testReadAllBytesByEntry_NullEntry() throws Exception {
		assertThrows(NullPointerException.class, () -> mArchive.readAllBytes((ArchiveEntry)null));
	}

	// readAllBytes(ArchiveEntry)
	// IllegalArgumentException 他インスタンスが生成したエントリを指定した
	@Test
	public void testReadAllBytesByEntry_BadOwner() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> mArchive.readAllBytes(DummyEntry.INSTANCE));
	}

	// readAllBytes(ArchiveEntry)
	// NoSuchFileException 指定したエントリがコンテンツではない
	@Test
	public void testReadAllBytesByEntry_NotContent() throws Exception {
		var entry = getEntryByPath(TestData.LOCATION_EN_PATH);
		assertThrows(NoSuchFileException.class, () -> mArchive.readAllBytes(entry));
	}

	// close()
	// メソッドを1回呼び出すとクローズ状態になること
	@Test
	public void testClose_Normal() throws Exception {
		mArchive.close();
		assertFalse(mArchive.isOpen());
	}

	// close()
	// メソッドを2回以上呼び出しても何も実行されず例外もスローされないこと
	@Test
	public void testClose_Call2Times() throws Exception {
		mArchive.close();
		mArchive.close();
	}

	// enumEntries(EntryCallback)
	// 全てのエントリが通知され、1エントリごとにコールバックが実行され、全エントリ列挙後にキャッシュ済み状態になること
	@Test
	public void testEnumEntries_Normal() throws Exception {
		var remaining = new HashSet<>(TestData.ALL_ENTRY_PATHS);
		var counter = new AtomicInteger(-1);
		mArchive.enumEntries((e, c, n) -> {
			var index = counter.incrementAndGet();
			assertTrue(remaining.remove(e.getPath()));
			assertEquals(index, e.getIndex());
			assertSame(mArchive, e.owner);
			assertEquals(index + 1, c);
			assertTrue((n == 0) || (n == mArchive.getEntryCount()));
			return true;
		});
		assertTrue(remaining.isEmpty());
		assertTrue(mArchive.canUseIndex());
		assertTrue(mArchive.canUsePath());
	}

	// enumEntries(EntryCallback)
	// コールバックでfalseを返すと列挙が中断され、未キャッシュ状態のままとなること
	@Test
	public void testEnumEntries_Abort() throws Exception {
		var orgCanUseIndex = mArchive.canUseIndex();
		var orgCanUsePath = mArchive.canUsePath();
		var counter = new AtomicInteger(0);
		mArchive.enumEntries((e, c, n) -> counter.incrementAndGet() < 3);
		assertEquals(orgCanUseIndex, mArchive.canUseIndex());
		assertEquals(orgCanUsePath, mArchive.canUsePath());
	}

	// enumEntries(EntryCallback)
	// 列挙中に例外がスローされても、次回列挙が実行可能であること
	@Test
	public void testEnumEntries_ThrownInCallback() throws Exception {
		var exc = RuntimeException.class;
		assertThrows(exc, () -> mArchive.enumEntries((e, c, n) -> { throw new RuntimeException("TEST"); }));
		mArchive.enumEntries((e, c, n) -> true);
		assertTrue(mArchive.canUseIndex());
		assertTrue(mArchive.canUsePath());
	}

	// enumEntries(EntryCallback)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testEnumEntries_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.enumEntries((e, c, n) -> true));
	}

	// enumEntries(EntryCallback)
	// IllegalStateException 列挙中に当メソッドが呼び出された
	@Test
	public void testEnumEntries_AlreadyRunning() throws Exception {
		mArchive.enumEntries((e, c, n) -> {
			assertThrows(IllegalStateException.class, () -> mArchive.enumEntries((e2, c2, n2) -> true));
			return false;
		});
	}

	// enumEntries(EntryCallback)
	// NullPointerException callbackがnull
	@Test
	public void testEnumEntries_NullCallback() throws Exception {
		assertThrows(NullPointerException.class, () -> mArchive.enumEntries(null));
	}

	// cacheEntries()
	// キャッシュが完了するとインデックス値、パスの両方が使用可能になること
	@Test
	public void testCacheEntries_Normal() throws Exception {
		mArchive.cacheEntries();
		assertTrue(mArchive.canUseIndex());
		assertTrue(mArchive.canUsePath());
	}

	// cacheEntries()
	// 2回実行しても例外がスローされず、インデックス値、パスの両方が使用可能であること
	@Test
	public void testCacheEntries_Call2Times() throws Exception {
		mArchive.cacheEntries();
		mArchive.cacheEntries();
		assertTrue(mArchive.canUseIndex());
		assertTrue(mArchive.canUsePath());
	}

	// cacheEntries()
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testCacheEntries_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.cacheEntries());
	}

	// cacheEntries()
	// IllegalStateException 列挙中に当メソッドが呼び出された
	@Test
	public void testCacheEntries_AlreadyRunning() throws Exception {
		mArchive.enumEntries((e, c, n) -> {
			assertThrows(IllegalStateException.class, () -> mArchive.cacheEntries());
			return false;
		});
	}

	// getEntry(int)
	// 指定したインデックス値のエントリを取得できること
	@Test
	public void testGetEntryByIndex_Normal() throws Exception {
		if (!mArchive.canUseIndex()) { mArchive.cacheEntries(); }
		assertAllEntriesByIndex();
	}

	// getEntry(int)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testGetEntryByIndex_CacheBeforeAfter() throws Exception {
		// 最初からインデックス値が使用可能な場合のみキャッシュ前チェックを実施する
		if (mArchive.canUseIndex()) {
			assertAllEntriesByIndex();
		}
		mArchive.cacheEntries();
		assertAllEntriesByIndex();
	}

	// getEntry(int)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testGetEntryByIndex_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.getEntry(0));
	}

	// getEntry(int)
	// IllegalStateException インデックス値によるアクセスが不可
	@Test
	public void testGetEntryByIndex_CantUseIndex() throws Exception {
		// 本テストはインデックス値によるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUseIndex()) {
			assertThrows(IllegalStateException.class, () -> mArchive.getEntry(0));
		}
	}

	// getEntry(int)
	// IndexOutOfBoundsException インデックス値が0未満またはエントリ総数以上
	@Test
	public void testGetEntryByIndex_IndexOutOfRange() throws Exception {
		if (!mArchive.canUseIndex()) { mArchive.cacheEntries(); }
		var exc = IndexOutOfBoundsException.class;
		assertThrows(exc, () -> mArchive.getEntry(-1));
		assertThrows(exc, () -> mArchive.getEntry(mArchive.getEntryCount()));
	}

	// getEntry(Path)
	// 指定したパスのエントリを取得できること
	@Test
	public void testGetEntryByPath_Normal() throws Exception {
		if (!mArchive.canUsePath()) { mArchive.cacheEntries(); }
		assertAllEntriesByPath();
	}

	// getEntry(Path)
	// エントリキャッシュ前後で動作仕様に変化がないこと
	@Test
	public void testGetEntryByPath_CacheBeforeAfter() throws Exception {
		// 最初からパスが使用可能な場合のみキャッシュ前チェックを実施する
		if (mArchive.canUsePath()) {
			assertAllEntriesByPath();
		}
		mArchive.cacheEntries();
		assertAllEntriesByPath();
	}

	// getEntry(Path)
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testGetEntryByPath_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.getEntry(TestData.CONTENT_ASCII_TXT_PATH));
	}

	// getEntry(Path)
	// IllegalStateException パスによるコンテンツアクセスが不可
	@Test
	public void testGetEntryByPath_CantUsePath() throws Exception {
		// 本テストはパスによるコンテンツアクセスが不可のアーカイブでのみ試験する
		if (!mArchive.canUsePath()) {
			assertThrows(IllegalStateException.class, () -> mArchive.getEntry(TestData.CONTENT_ASCII_TXT_PATH));
		}
	}

	// getEntry(Path)
	// NullPointerException pathがnull
	@Test
	public void testGetEntryByPath_NullPath() throws Exception {
		if (!mArchive.canUsePath()) { mArchive.cacheEntries(); }
		assertThrows(NullPointerException.class, () -> mArchive.getEntry(null));
	}

	// getEntryCount()
	// エントリキャッシュ前に正しいエントリ数が取得できなくても、キャッシュすると正しいエントリ数が返ること
	@Test
	public void testGetEntryCount_Normal() throws Exception {
		var entryCount = mArchive.getEntryCount();
		if (entryCount == 0) {
			// キャッシュ前に正しいエントリ数が取得できない場合のテストケース
			mArchive.cacheEntries();
			assertEquals(TestData.ALL_ENTRY_PATHS.size(), mArchive.getEntryCount());
		} else {
			// キャッシュ前から正しいエントリ数が取得できる場合のテストケース
			assertEquals(TestData.ALL_ENTRY_PATHS.size(), entryCount);
			mArchive.cacheEntries();
			assertEquals(TestData.ALL_ENTRY_PATHS.size(), mArchive.getEntryCount());
		}
	}

	// getEntryCount()
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testGetEntryCount_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.getEntryCount());
	}

	// canUseIndex()
	// キャッシュするとインデックス値が使用可になること
	@Test
	public void testCanUseIndex_Normal() throws Exception {
		mArchive.cacheEntries();
		assertTrue(mArchive.canUseIndex());
	}

	// canUseIndex()
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testCanUseIndex_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.canUseIndex());
	}

	// canUsePath()
	// キャッシュするとインデックス値が使用可になること
	@Test
	public void testCanUsePath_Normal() throws Exception {
		mArchive.cacheEntries();
		assertTrue(mArchive.canUsePath());
	}

	// canUsePath()
	// IllegalStateException アーカイブがオープンされていない
	@Test
	public void testCanUsePath_NotOpen() throws Exception {
		mArchive.close();
		assertThrows(IllegalStateException.class, () -> mArchive.canUsePath());
	}

	private int getIndexByPath(Path path) throws Exception {
		// エントリをキャッシュして全機能を使用できるようにする
		// インデックス値を使用する場合、全ての機能を使用可能にしないと一律正しいインデックス値を返せない。
		// そのためアーカイブ種別に関わらずキャッシュ済みの状態で試験を実施する必要がある
		mArchive.cacheEntries();

		// 全エントリの中から指定されたパスを持つエントリを探す
		var entry = mArchive.getEntry(path);
		if (entry == null) {
			var msg = String.format("BUG!! %s: No such entry in %s", path, expectedPath());
			fail(msg);
		}

		// 取得できたエントリからインデックス値を返す
		// キャッシュ済みのエントリからは有効なインデックス値が返却されなければならない
		var index = entry.getIndex();
		if ((index < 0) || (index >= mArchive.getEntryCount())) {
			var msg = String.format("BUG!! Index is returned from entry '%s'", path);
			fail(msg);
		}

		return index;
	}

	private Path getPathByPath(Path path) throws Exception {
		// パスが使用できないアーカイブではエントリをキャッシュして使用できるようにする
		if (!mArchive.canUsePath()) {
			mArchive.cacheEntries();
		}

		// 指定されたパスをそのまま返す
		// このメソッドの意義は、パス指定を最初からサポートしているアーカイブではキャッシュ処理を省略し、
		// サポートしていないアーカイブでのみエントリのキャッシュを行うことにある。
		return path;
	}

	private ArchiveEntry getEntryByPath(Path path) throws Exception {
		// パスに該当するエントリを検索する
		var entry = (ArchiveEntry)null;
		if (mArchive.canUseIndex()) {
			// インデックス値によるコンテンツアクセスが可能な場合、全エントリから該当パスのエントリを探す
			var entryCount = mArchive.getEntryCount();
			for (var i = 0; i < entryCount; i++) {
				var entryTmp = mArchive.getEntry(i);
				if (entryTmp.getPath().equals(path)) {
					entry = entryTmp;
					break;
				}
			}
		} else if (mArchive.canUsePath()) {
			// パスによるコンテンツアクセスが可能な場合、指定パスから該当パスのエントリを探す
			entry = mArchive.getEntry(path);
		} else {
			// 全ての能力を備えていない場合はアーカイブクラスの不具合
			var msg = String.format("BUG!! The archive has not all capabilities");
			fail(msg);
		}

		// 指定パスのエントリが見つからない場合は不具合
		if (entry == null) {
			var msg = String.format("BUG!! %s: No such entry in %s", path, expectedPath());
			fail(msg);
		}

		return entry;
	}

	private void assertAllEntriesByIndex() {
		var remaining = new HashSet<>(TestData.ALL_ENTRY_PATHS);
		var entryCount = mArchive.getEntryCount();
		for (var i = 0; i < entryCount; i++) {
			var entry = mArchive.getEntry(i);
			assertNotNull(entry);
			assertSame(mArchive, entry.owner);
			assertEquals(i, entry.getIndex());
			assertTrue(remaining.remove(entry.getPath()));
		}
		assertTrue(remaining.isEmpty());
	}

	private void assertAllEntriesByPath() {
		var paths = List.copyOf(TestData.ALL_ENTRY_PATHS);
		for (var path : paths) {
			var entry = mArchive.getEntry(path);
			assertNotNull(entry);
			assertSame(mArchive, entry.owner);
			//assertTrue(entry.getIndex() >= 0);  // 未キャッシュの場合、負の値になることがある(仕様)
			assertEquals(path, entry.getPath());
		}
	}
}
