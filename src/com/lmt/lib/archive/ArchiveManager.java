package com.lmt.lib.archive;

import static com.lmt.lib.archive.Assertion.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.Function;

import com.lmt.lib.archive.impl.FolderArchive;
import com.lmt.lib.archive.impl.SzjbArchive;

/**
 * アーカイブライブラリのマネージャクラスです。
 *
 * <p>当クラスではライブラリの初期化を行い、ライブラリの基本的な動作仕様を決定します。
 * 当クラスの機能を使用することで、サポートするアーカイブをオープンしたり、アーカイブ種別を特定することができます。</p>
 *
 * <p>現バージョンにてサポートされているアーカイブ形式は以下の通りです。</p>
 * <ul>
 * <li>{@link FolderArchive}</li>
 * <li>{@link SzjbArchive.Zip}</li>
 * <li>{@link SzjbArchive.Rar}</li>
 * <li>{@link SzjbArchive.SevenZip}</li>
 * </ul>
 *
 * @author J-SON3
 */
public class ArchiveManager {
	/** クラスのインスタンス */
	private static ArchiveManager sInstance = null;
	/** 初期化済みフラグ */
	private boolean mIsInitialized = false;
	/** アーカイブ形式判定 */
	private Function<Path, ArchiveType> mTypeTester = null;

	/** コンストラクタ */
	private ArchiveManager() {
		// Do nothing
	}

	/**
	 * アーカイブマネージャのインスタンスを取得します。
	 * @return このオブジェクトのインスタンス
	 */
	public static ArchiveManager getInstance() {
		if (sInstance == null) {
			sInstance = new ArchiveManager();
		}
		return sInstance;
	}

	/**
	 * アーカイブマネージャを初期化します。
	 * <p>初期化することで、アーカイブのオープンが可能になります。
	 * 一度初期化すると初期化のし直しはできなくなるので注意してください。</p>
	 * @param typeTester アーカイブ種別判定処理
	 * @exception IllegalStateException アーカイブマネージャが初期化済み
	 * @exception NullPointerException typeJudgementがnull
	 */
	public void initialize(Function<Path, ArchiveType> typeTester) {
		assertNotInitialized();
		assertArgNotNull(typeTester, "typeJudgement");
		mIsInitialized = true;
		mTypeTester = typeTester;
	}

	/**
	 * アーカイブマネージャを初期化します。
	 * <p>当メソッドはアーカイブ種別判定処理として {@link StandardTypeTester} を使用して初期化を行います。
	 * 具体的な初期化処理の内容は {@link #initialize(Function)} を参照してください。</p>
	 * @exception IllegalStateException アーカイブマネージャが初期化済み
	 */
	public void initialize() {
		initialize(new StandardTypeTester());
	}

	/**
	 * アーカイブマネージャが初期化済みかどうかを取得します。
	 * @return 初期化済みであればtrue
	 */
	public boolean isInitialized() {
		return mIsInitialized;
	}

	/**
	 * 指定パスが示すフォルダ・ファイルのアーカイブ種別を判定します。
	 * <p>アーカイブ種別判定処理内で例外がスローされた場合、その例外がそのままスローされます。</p>
	 * @param path 判定対象のフォルダ・ファイルパス
	 * @return パスが示すフォルダ・ファイルのアーカイブ種別
	 * @exception NullPointerException pathがnull
	 * @exception IllegalStateException アーカイブマネージャが初期化されていない
	 */
	public ArchiveType getArchiveType(Path path) {
		assertInitialized();
		assertArgNotNull(path, "path");
		return mTypeTester.apply(path);
	}

	/**
	 * アーカイブをオープンします。
	 * <p>指定したパスのフォルダ・ファイルをアーカイブ種別判定処理に入力し、アーカイブ種別を特定します。
	 * アーカイブ種別を特定するためにフォルダ・ファイルをオープンしても差し支えありませんが、
	 * 判定を完了させる前に必ずクローズしてください。クローズしないと後の処理で例外がスローされる可能性があります。</p>
	 * @param path オープンするアーカイブのパス
	 * @return オープンされたアーカイブオブジェクト
	 * @exception IllegalStateException アーカイブマネージャが初期化されていない
	 * @exception NullPointerException pathがnull
	 * @exception NoSuchFileException 指定パスのアーカイブが見つからない
	 * @exception IOException アーカイブ種別判定中に任意の例外がスローされた
	 * @exception IOException アーカイブ種別判定結果が {@link ArchiveType#UNKNOWN} またはnull
	 * @exception IOException 指定パスのフォルダ・ファイルの読み取り権限がない
	 * @exception IOException 指定パスのフォルダ・ファイルがアーカイブとして認識できない
	 */
	public Archive open(Path path) throws IOException {
		assertInitialized();
		assertArgNotNull(path, "path");

		// アーカイブ形式を判定する
		var archiveType = ArchiveType.UNKNOWN;
		try {
			archiveType = mTypeTester.apply(path);
		} catch (Exception e) {
			throw new IOException("Occurs exception during judge archive type", e);
		}

		// アーカイブ形式の判定結果をチェックする
		if ((archiveType == null) || archiveType.isUnknown()) {
			throw new IOException("Can't judgement archive type");
		}

		// 判定されたアーカイブ形式に基づいてアーカイブオブジェクトを生成する
		return archiveType.open(path);
	}

	/**
	 * アーカイブマネージャが初期化済みであることを確認するアサーション。
	 * @exception IllegalStateException アーカイブマネージャが初期化されていない
	 */
	private void assertInitialized() {
		assertState(mIsInitialized, "ArchiveManager is not initialized");
	}

	/**
	 * アーカイブマネージャが未初期化であることを確認するアサーション。
	 * @exception IllegalStateException アーカイブマネージャが初期化済み
	 */
	private void assertNotInitialized() {
		assertState(!mIsInitialized, "ArchiveManager is already initialized");
	}
}
