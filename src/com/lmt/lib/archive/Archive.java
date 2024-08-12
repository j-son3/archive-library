package com.lmt.lib.archive;

import static com.lmt.lib.archive.Assertion.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * 1個のフォルダまたはアーカイブファイルを表すクラスです。
 *
 * <p>当クラスはフォルダやアーカイブファイルの形式を意識することなく共通のI/Fでコンテンツにアクセスする手段を提供します。
 * このクラスで提供する機能はコンテンツの読み込みに特化しています。従ってアーカイブ内容の更新を行うことはできません。</p>
 *
 * <p>アーカイブは {@link ArchiveManager} を通じてオープンします。具体的なオープン方法については
 * {@link ArchiveManager#open(Path)} を参照してください。
 *
 * @author J-SON3
 */
public abstract class Archive implements Closeable {
	/** インデックス値によるエントリ参照が可能であることを表すビット値 */
	protected static final int CAPS_INDEX = 0x01;
	/** パスによるエントリ参照が可能であることを表すビット値 */
	protected static final int CAPS_PATH = 0x02;
	/** 全ての手段でエントリ参照が可能であることを表す値 */
	protected static final int CAPS_ALL = CAPS_INDEX | CAPS_PATH;

	/** アーカイブ種別 */
	private ArchiveType mArchiveType;
	/** アーカイブのパス */
	private Path mPath;
	/** エントリ列挙が実行中かどうか */
	private boolean mIsRunningEnum;
	/** アーカイブがオープン中かどうか */
	private boolean mIsOpen;

	/**
	 * 新しいアーカイブオブジェクトを構築します。
	 * <p>当コンストラクタは継承先クラスから呼び出されます。</p>
	 * @param archiveType アーカイブ種別
	 * @param path アーカイブパス
	 * @exception NullPointerException archiveTypeがnull
	 * @exception NullPointerException pathがnull
	 */
	protected Archive(ArchiveType archiveType, Path path) {
		assertArgNotNull(archiveType, "archiveType");
		assertArgNotNull(path, "path");
		mArchiveType = archiveType;
		mPath = path.toAbsolutePath();
		mIsRunningEnum = false;
		mIsOpen = true;
	}

	/**
	 * アーカイブ種別を取得します。
	 * <p>当メソッドはアーカイブのオープン状態に関わらず使用できます。</p>
	 * @return アーカイブ種別
	 */
	public ArchiveType getArchiveType() {
		return mArchiveType;
	}

	/**
	 * アーカイブパスを取得します。
	 * <p>当メソッドはアーカイブのオープン状態に関わらず使用できます。</p>
	 * <p>返されるパスは絶対パスとなります。相対パス指定でアーカイブをオープンしても絶対パスに変換されます。</p>
	 * @return アーカイブパス
	 */
	public Path getPath() {
		return mPath;
	}

	/**
	 * アーカイブが開かれているかどうかを返します。
	 * @return アーカイブが開かれている場合true
	 */
	public boolean isOpen() {
		return mIsOpen;
	}

	/**
	 * 全てのエントリ情報がキャッシュされているかどうかを返します。
	 * <p>当メソッドは {@link #cacheEntries()} または {@link #enumEntries(EntryCallback)}
	 * による全エントリの列挙によりエントリ情報がメモリ上にキャッシュされたかどうかを調べることができます。</p>
	 * @return 全てのエントリ情報がキャッシュされていればtrue
	 * @exception IllegalStateException アーカイブがオープンされていない
	 */
	public boolean isCached() {
		assertIsOpen();
		return (onGetCapability() & CAPS_ALL) == CAPS_ALL;
	}

	/**
	 * 指定したコンテンツ(ファイル)を開き入力ストリームを返します。
	 * <p>返された入力ストリームを使用することでコンテンツのデータを取り出すことができます。</p>
	 * <p>アーカイブの種別によってはコンテンツを一旦メモリ上に展開したうえで入力ストリームを返すことがあります。
	 * 実行環境の使用可能メモリ容量が極端に少ないとメモリ不足エラーが発生する可能性があること留意してください。</p>
	 * <p>当メソッドはアーカイブ内エントリのインデックス値でコンテンツを指定します。当メソッドが使用可能かは
	 * {@link #canUseIndex()} で調べてください。<p>
	 * @param index オープンするコンテンツのインデックス値
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException インデックス値によるコンテンツアクセスが不可
	 * @exception IndexOutOfBoundsException インデックス値が0未満または {@link #getEntryCount()} 以上
	 * @exception NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public InputStream openContent(int index) throws IOException {
		assertIsOpen();
		assertArgIndex(index);
		return onOpenContentByIndex(index);
	}

	/**
	 * 指定したコンテンツ(ファイル)を開き入力ストリームを返します。
	 * <p>返された入力ストリームを使用することでコンテンツのデータを取り出すことができます。</p>
	 * <p>アーカイブの種別によってはコンテンツを一旦メモリ上に展開したうえで入力ストリームを返すことがあります。
	 * 実行環境の使用可能メモリ容量が極端に少ないとメモリ不足エラーが発生する可能性があること留意してください。</p>
	 * <p>当メソッドはアーカイブ内エントリのパスでコンテンツを指定します。パスの英字の大小を区別するかは、
	 * アーカイブの種別、実行環境のOSにより異なりますので当ライブラリ利用者は極力英字の大小を区別して扱ってください。
	 * また、当メソッドが使用可能かは {@link #canUsePath()} で調べてください。<p>
	 * @param path オープンするコンテンツのパス
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException パスによるコンテンツアクセスが不可
	 * @exception NullPointerException pathがnull
	 * @exception NoSuchFileException 指定したパスのエントリが見つからない
	 * @exception NoSuchFileException 指定したパスのエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public InputStream openContent(Path path) throws IOException {
		assertIsOpen();
		assertArgPath(path);
		return onOpenContentByPath(path);
	}

	/**
	 * 指定したコンテンツ(ファイル)を開き入力ストリームを返します。
	 * <p>返された入力ストリームを使用することでコンテンツのデータを取り出すことができます。</p>
	 * <p>アーカイブの種別によってはコンテンツを一旦メモリ上に展開したうえで入力ストリームを返すことがあります。
	 * 実行環境の使用可能メモリ容量が極端に少ないとメモリ不足エラーが発生する可能性があること留意してください。</p>
	 * <p>当メソッドはコンテンツエントリを指定します。他インスタンスが生成したエントリは指定できません。<p>
	 * @param entry オープンするコンテンツのエントリ
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception NullPointerException entryがnull
	 * @exception IllegalArgumentException 他インスタンスが生成したエントリを指定した
	 * @exception NoSuchFileException 指定したエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public InputStream openContent(ArchiveEntry entry) throws IOException {
		assertIsOpen();
		assertArgEntry(entry);
		return onOpenContentByEntry(entry);
	}

	/**
	 * 指定したコンテンツ(ファイル)を解凍しコンテンツ全体のバイトデータを返します。
	 * <p>コンテンツ全体のバイトデータを読み込みたい場合、当メソッドを使用するとほとんどのケースで
	 * {@link #openContent(int)} が返す入力ストリームからバイトデータを読み込むよりも処理効率が良くなります。</p>
	 * <p>当メソッドはアーカイブ内エントリのインデックス値でコンテンツを指定します。当メソッドが使用可能かは
	 * {@link #canUseIndex()} で調べてください。<p>
	 * @param index 解凍するコンテンツのインデックス値
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException インデックス値によるコンテンツアクセスが不可
	 * @exception IndexOutOfBoundsException インデックス値が0未満または {@link #getEntryCount()} 以上
	 * @exception NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public byte[] readAllBytes(int index) throws IOException {
		assertIsOpen();
		assertArgIndex(index);
		return onReadAllBytesByIndex(index);
	}

	/**
	 * 指定したコンテンツ(ファイル)を解凍しコンテンツ全体のバイトデータを返します。
	 * <p>コンテンツ全体のバイトデータを読み込みたい場合、当メソッドを使用するとほとんどのケースで
	 * {@link #openContent(int)} が返す入力ストリームからバイトデータを読み込むよりも処理効率が良くなります。</p>
	 * <p>当メソッドはアーカイブ内エントリのパスでコンテンツを指定します。パスの英字の大小を区別するかは、
	 * アーカイブの種別、実行環境のOSにより異なりますので当ライブラリ利用者は極力英字の大小を区別して扱ってください。
	 * また、当メソッドが使用可能かは {@link #canUsePath()} で調べてください。<p>
	 * @param path 解凍するコンテンツのパス
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException パスによるコンテンツアクセスが不可
	 * @exception NullPointerException pathがnull
	 * @exception NoSuchFileException 指定したパスのエントリが見つからない
	 * @exception NoSuchFileException 指定したパスのエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public byte[] readAllBytes(Path path) throws IOException {
		assertIsOpen();
		assertArgPath(path);
		return onReadAllBytesByPath(path);
	}

	/**
	 * 指定したコンテンツ(ファイル)を解凍しコンテンツ全体のバイトデータを返します。
	 * <p>コンテンツ全体のバイトデータを読み込みたい場合、当メソッドを使用するとほとんどのケースで
	 * {@link #openContent(int)} が返す入力ストリームからバイトデータを読み込むよりも処理効率が良くなります。</p>
	 * <p>当メソッドはコンテンツエントリを指定します。他インスタンスが生成したエントリは指定できません。<p>
	 * @param entry 解凍するコンテンツのエントリ
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception NullPointerException entryがnull
	 * @exception IllegalArgumentException 他インスタンスが生成したエントリを指定した
	 * @exception NoSuchFileException 指定したエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	public byte[] readAllBytes(ArchiveEntry entry) throws IOException {
		assertIsOpen();
		assertArgEntry(entry);
		return onReadAllBytesByEntry(entry);
	}

	/**
	 * アーカイブを閉じます。
	 * <p>既に閉じられたアーカイブに対して当メソッドを呼び出しても何も行いません。</p>
	 * @exception IOException クローズ中にエラーが発生した
	 */
	@Override
	public void close() throws IOException {
		if (isOpen()) {
			mIsOpen = false;
			onCloseArchive();
		}
	}

	/**
	 * アーカイブ内の場所(フォルダ)・コンテンツ(ファイル)を含む全てのエントリを列挙します。
	 * <p>エントリが列挙される順序はアーカイブごとに異なりますが、それ以外は全アーカイブで同等の機能を提供します。
	 * 従って、当メソッドはアーカイブの種別に左右されることなくほぼ公平な性能を発揮します。
	 * (ただし、アーカイブごとの処理速度は異なります)</p>
	 * <p>アーカイブからエントリが読み取られるごとにそのエントリの情報がコールバック通知されます。
	 * アプリケーションはコールバック内で必要な処理を行ってください。列挙を中断したい場合、コールバックの戻り値として
	 * false を返すと列挙が停止されアプリケーション側に処理が戻ります。</p>
	 * <p>当メソッドでアーカイブ内の全てのエントリが列挙されるとアーカイブオブジェクト内でエントリ情報がキャッシュされ、
	 * それ以降はインデックス・パスを引数にする全てのメソッドが使用可能になることが保証されます。つまり
	 * {@link #canUseIndex()} と {@link #canUsePath()} の両方が true を返すようになります。</p>
	 * <p>尚、当メソッドの実行中は同オブジェクトで連続して同じメソッドを呼び出すことはできません。</p>
	 * @param callback エントリが列挙される度に呼び出されるコールバック
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException 列挙中に当メソッドが呼び出された
	 * @exception NullPointerException callbackがnull
	 * @exception IOException エントリの読み取り中にエラー発生
	 * @see #canUseIndex()
	 * @see #canUsePath()
	 * @see #cacheEntries()
	 */
	public void enumEntries(EntryCallback callback) throws IOException {
		assertIsOpen();
		assertState(!mIsRunningEnum, "Now is running enumerate entries");
		assertArgNotNull(callback, "callback");
		try {
			mIsRunningEnum = true;
			onEnumEntries(callback);
		} finally {
			mIsRunningEnum = false;
		}
	}

	/**
	 * アーカイブ内の全てのエントリ情報を読み込み、キャッシュします。
	 * <p>当メソッドは {@link #enumEntries(EntryCallback)} を呼び出して全てのエントリを列挙しキャッシュします。
	 * これによりインデックス・パスを引数にする全てのメソッドが使用可能になることが保証されます。
	 * これは以下のコードと等価であり、当メソッドはこの処理を簡略化するためのラッパーメソッドです。</p>
	 * <pre>
	 * archive.enumEntries((e, c, n) -> true);</pre>
	 * <p>エントリ情報のキャッシュ後、アーカイブ内のフォルダ・ファイル構成が更新されてもキャッシュ情報は更新されません。
	 * キャッシュに更新された情報を反映するには再度当メソッドを実行しなければなりません。</p>
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException 列挙中に当メソッドが呼び出された
	 * @exception IOException エントリの読み取り中にエラー発生
	 * @see #enumEntries()
	 * @see #canUseIndex()
	 * @see #canUsePath()
	 */
	public void cacheEntries() throws IOException {
		enumEntries((e, c, n) -> true);
	}

	/**
	 * 指定したエントリの詳細情報を取得します。
	 * <p>当メソッドはアーカイブ内エントリのインデックス値でエントリを指定します。当メソッドが使用可能かは
	 * {@link #canUseIndex()} で調べてください。<p>
	 * @param index エントリのインデックス値
	 * @return エントリの詳細情報。エントリ情報の読み取りエラー時はnull。
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException インデックス値によるアクセスが不可
	 * @exception IndexOutOfBoundsException インデックス値が0未満または {@link #getEntryCount()} 以上
	 */
	public ArchiveEntry getEntry(int index) {
		assertIsOpen();
		assertArgIndex(index);
		return onGetEntryByIndex(index);
	}

	/**
	 * 指定したエントリの詳細情報を取得します。
	 * <p>当メソッドはアーカイブ内エントリのパスでエントリを指定します。パスの英字の大小を区別するかは、
	 * アーカイブの種別、実行環境のOSにより異なりますので当ライブラリ利用者は極力英字の大小を区別して扱ってください。
	 * また、当メソッドが使用可能かは {@link #canUsePath()} で調べてください。<p>
	 * @param path エントリのパス
	 * @return エントリの詳細情報。エントリ情報の読み取りエラー、または指定パスのエントリが存在しない場合はnull。
	 * @exception IllegalStateException アーカイブがオープンされていない
	 * @exception IllegalStateException パスによるコンテンツアクセスが不可
	 * @exception NullPointerException pathがnull
	 */
	public ArchiveEntry getEntry(Path path) {
		assertIsOpen();
		assertArgPath(path);
		return onGetEntryByPath(path);
	}

	/**
	 * アーカイブに含まれるエントリ数を取得します。
	 * <p>当メソッドが返すエントリ数は全ての種類のエントリを含む合計数です。ファイル数ではないことに注意してください。
	 * また、アーカイブのオープン直後に正しいエントリ数が返るかどうかはアーカイブの種別により異なります。
	 * 当メソッドが正しいエントリ数を返すことを保証したい場合は {@link #cacheEntries()} を実行してください。</p>
	 * <p>エントリ情報をキャッシュするまで実際のエントリ数が分からない種類のアーカイブ (例：{@link ArchiveType#FOLDER})
	 * では、エントリ情報がキャッシュされるまでの間は0を返します。</p>
	 * @return アーカイブに含まれるエントリ数
	 * @exception IllegalStateException アーカイブがオープンされていない
	 */
	public int getEntryCount() {
		assertIsOpen();
		return onGetEntryCount();
	}

	/**
	 * インデックス値によるエントリへのアクセスが可能かどうかを返します。
	 * @return インデックス値によるエントリへのアクセスが可能であればtrue
	 * @exception IllegalStateException アーカイブがオープンされていない
	 */
	public boolean canUseIndex() {
		assertIsOpen();
		return (onGetCapability() & CAPS_INDEX) != 0;
	}

	/**
	 * パスによるエントリへのアクセスが可能かどうかを返します。
	 * @return パスによるエントリへのアクセスが可能であればtrue
	 * @exception IllegalStateException アーカイブがオープンされていない
	 */
	public boolean canUsePath() {
		assertIsOpen();
		return (onGetCapability() & CAPS_PATH) != 0;
	}

	/**
	 * アーカイブがオープンされようとする時に呼び出されます。
	 * <p>当メソッドはコンストラクタから呼び出されることを想定しています。</p>
	 * @param path アーカイブパス
	 * @exception NoSuchFileException pathにアーカイブが未存在、または期待する種別(ファイル・フォルダ)ではない
	 * @exception IOException アーカイブのオープンエラー(例：未知のファイル形式、読み取り権限なし)
	 */
	protected abstract void onOpenArchive(Path path) throws IOException;

	/**
	 * アーカイブがクローズされようとする時に呼び出されます。
	 * @throws IOException アーカイブのクローズエラー(例：クローズ時に当該ファイルへのアクセス不可)
	 */
	protected abstract void onCloseArchive() throws IOException;

	/**
	 * エントリ指定によりコンテンツがオープンされようとする時に呼び出されます。
	 * @param entry オープンしようとするコンテンツのエントリ
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception IOException 入力エラー発生時
	 */
	protected abstract InputStream onOpenContentByEntry(ArchiveEntry entry) throws IOException;

	/**
	 * インデックス値指定によりコンテンツがオープンされようとする時に呼び出されます。
	 * @param index オープンしようとするコンテンツのインデックス値
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	protected abstract InputStream onOpenContentByIndex(int index) throws IOException;

	/**
	 * パス指定によりコンテンツがオープンされようとする時に呼び出されます。
	 * @param path オープンしようとするコンテンツのパス
	 * @return コンテンツにアクセスする入力ストリーム
	 * @exception NoSuchFileException 指定したパスのエントリが見つからない
	 * @exception NoSuchFileException 指定したパスのエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	protected abstract InputStream onOpenContentByPath(Path path) throws IOException;

	/**
	 * エントリ指定によりコンテンツ全体の解凍後バイトデータを読み取ろうとする時に呼び出されます。
	 * @param entry 解凍しようとするコンテンツのエントリ
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception IOException 入力エラー発生時
	 */
	protected abstract byte[] onReadAllBytesByEntry(ArchiveEntry entry) throws IOException;

	/**
	 * インデックス値指定によりコンテンツ全体の解凍後バイトデータを読み取ろうとする時に呼び出されます。
	 * @param index 解凍しようとするコンテンツのインデックス値
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception NoSuchFileException 指定したインデックス値のエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	protected abstract byte[] onReadAllBytesByIndex(int index) throws IOException;

	/**
	 * パス指定によりコンテンツ全体の解凍後バイトデータを読み取ろうとする時に呼び出されます。
	 * @param index 解凍しようとするコンテンツのインデックス値
	 * @return コンテンツ全体の解凍後バイトデータ
	 * @exception NoSuchFileException 指定したパスのエントリが見つからない
	 * @exception NoSuchFileException 指定したパスのエントリがコンテンツではない
	 * @exception IOException その他入力エラー発生時
	 */
	protected abstract byte[] onReadAllBytesByPath(Path path) throws IOException;

	/**
	 * インデックス値によりエントリの詳細情報を取得しようとするときに呼び出されます。
	 * @param index エントリのインデックス値
	 * @return エントリの詳細情報。エントリ情報の読み取りエラー時はnull。
	 */
	protected abstract ArchiveEntry onGetEntryByIndex(int index);

	/**
	 * パスによりエントリの詳細情報を取得しようとするときに呼び出されます。
	 * @param path エントリのパス
	 * @return エントリの詳細情報。エントリ情報の読み取りエラー、または指定パスのエントリが存在しない場合はnull。
	 */
	protected abstract ArchiveEntry onGetEntryByPath(Path path);

	/**
	 * アーカイブ内に含まれる全てのエントリ情報を列挙しようとする時に呼び出されます。
	 * <p>全てのエントリが列挙された場合、全てのエントリ情報をオブジェクト内でキャッシュします。</p>
	 * @param callback エントリが列挙される度に呼び出されるコールバック
	 * @exception IOException エントリの読み取り中にエラー発生
	 */
	protected abstract void onEnumEntries(EntryCallback callback) throws IOException;

	/**
	 * アーカイブ内に含まれるエントリ数を取得しようとする時に呼び出されます。
	 * <p>エントリ数が不明な状態で呼び出された場合は0を返してください。</p>
	 * @return アーカイブ内に含まれるエントリ数
	 */
	protected abstract int onGetEntryCount();

	/**
	 * アーカイブの現在の能力を参照しようとする時に呼び出されます。
	 * <p>当メソッドは呼び出し時のオブジェクトの能力を返します。能力値はアーカイブの利用状況により変化します。</p>
	 * @return アーカイブの現在の能力を表すビット値の集合
	 * @see #CAPS_INDEX
	 * @see #CAPS_PATH
	 */
	protected abstract int onGetCapability();

	/**
	 * オープン状態かどうかのアサーション。
	 * @exception IllegalStateException オープン状態ではない
	 */
	private void assertIsOpen() {
		assertState(mIsOpen, "Archive is not open: %s", this);
	}

	/**
	 * 指定されたエントリ情報のアサーション。
	 * @param entry エントリ情報
	 * @exception NullPointerException entryがnull
	 * @exception IllegalArgumentException エントリのオーナーが異なる
	 * @exception NoSuchFileException エントリがコンテンツではない
	 */
	private void assertArgEntry(ArchiveEntry entry) throws IOException {
		assertArgNotNull(entry, "entry");
		assertArg(entry.owner == this, "Illegal entry owner");
		if (!entry.isContent()) {
			throw new NoSuchFileException(entry.getPath().toString(), null, "This entry is not content");
		}
	}

	/**
	 * インデックス値のアサーション。
	 * @param index インデックス値
	 * @exception IllegalStateException インデックス値によるコンテンツアクセスが不可
	 * @exception IndexOutOfBoundsException インデックス値が0未満または {@link #getEntryCount()} 以上
	 */
	private void assertArgIndex(int index) {
		assertState(canUseIndex(), "In this class, can not use index now");
		assertArgIndexRange(index, onGetEntryCount(), "index");
	}

	/**
	 * パスのアサーション。
	 * @param path パス
	 * @exception IllegalStateException パスによるコンテンツアクセスが不可
	 * @exception NullPointerException pathがnull
	 */
	private void assertArgPath(Object path) {
		assertState(canUsePath(), "In this class, can not use path now");
		assertArgNotNull(path , "path");
	}
}
