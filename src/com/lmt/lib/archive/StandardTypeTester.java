package com.lmt.lib.archive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * 標準的なアーカイブ種別判定処理の実装です。
 *
 * <p>当クラスは、指定されたパスのファイル・フォルダの種類、およびファイルの拡張子からアーカイブ種別を判定します。</p>
 *
 * @author J-SON3
 */
public class StandardTypeTester implements Function<Path, ArchiveType> {
	/**
	 * 指定パスの内容からアーカイブ種別を判定します。
	 * <p>最初に指定パスがディレクトリかどうかを判定し、ディレクトリであれば {@link ArchiveType#FOLDER} を返します。
	 * 次にパスが示すファイルの拡張子を調べ、拡張子を持たないファイルの場合は {@link ArchiveType#UNKNOWN} とします。
	 * 拡張子が"zip"であれば {@link ArchiveType#ZIP} 、"rar"であれば {@link ArchiveType#RAR}
	 * 、"7z"であれば {@link ArchiveType#SEVEN_ZIP} と判定します。それ以外は {@link ArchiveType#UNKNOWN} とします。</p>
	 * <p>拡張子の英字の大小は区別しません。また、ディレクトリ以外の判定結果はファイルの存在有無の影響を受けません。</p>
	 * @param path アーカイブ種別判定対象のパス
	 * @return アーカイブ種別
	 */
	@Override
	public ArchiveType apply(Path path) {
		if (Files.isDirectory(path)) {
			return ArchiveType.FOLDER;
		}

		var fileName = path.getFileName().toString();
		var dotPos = fileName.lastIndexOf('.');
		if (dotPos == -1) {
			return ArchiveType.UNKNOWN;
		}

		var extension = fileName.substring(dotPos);
		if (extension.equalsIgnoreCase(".zip")) {
			return ArchiveType.ZIP;
		} else if (extension.equalsIgnoreCase(".7z")) {
			return ArchiveType.SEVEN_ZIP;
		} else if (extension.equalsIgnoreCase(".rar")) {
			return ArchiveType.RAR;
		} else {
			return ArchiveType.UNKNOWN;
		}
	}
}
