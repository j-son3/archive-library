package com.lmt.lib.archive;

/**
 * エントリ列挙時のコールバックインターフェイスです。
 *
 * @author J-SON3
 */
@FunctionalInterface
public interface EntryCallback {
	/**
	 * 1個のエントリが列挙される度に呼び出されます。
	 * <p>列挙される順序は {@link Archive#enumEntries(EntryCallback)} の仕様に準拠します。
	 * アーカイブ種別やアーカイブの構成内容が異なれば、統一的な順序にはならないことに留意する必要があります。
	 * エントリの並び順に依存しないようにアプリケーションを設計するようにしてください。</p>
	 * <p>コールバックの引数によって現在列挙中のエントリ番号(インデックス値+1)とアーカイブのエントリ総数が通知されます。
	 * これを利用し列挙の進捗状況を知ることができますが、全アーカイブ種別でエントリ総数が通知されるわけではありません。
	 * 列挙が完了するまでエントリ総数が分からないアーカイブではエントリ総数には常に0が設定されます。</p>
	 * <p>列挙操作を中断したい場合はコールバックの戻り値で false を返すことで中断することができます。</p>
	 * @param entry 列挙されたエントリ情報
	 * @param current 列挙されたエントリのエントリ番号(1から開始)
	 * @param count アーカイブのエントリ総数。エントリ数が不明なアーカイブ種別では0。
	 * @return エントリの列挙を続行する場合はtrue、中断する場合はfalse
	 */
	boolean call(ArchiveEntry entry, int current, int count);
}
