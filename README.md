# Archive Library
Now printing...

## 事前準備
ライブラリを使用するにあたり、以下のソフトウェアが必要となります。

### Java Development Kit (JDK)
Archive LibraryはJavaで記述されていますのでビルドにはJDKが必要になります。ライブラリ本体はJava11で記述されています。JDKは[こちら(例)](https://www.oracle.com/jp/java/technologies/downloads/)から入手してください。

### Apache Maven (ビルドツール)
Archive LibraryのビルドにはMavenを使用します。Mavenは[こちら](https://maven.apache.org/download.cgi)から入手できます。尚、インストール・設定の手順についてはインターネット等で検索してください。

## ビルド方法
ビルドはコマンドプロンプト等から行います。<br>
このドキュメントが格納されたディレクトリから以下のコマンドを実行し、Mavenのローカルリポジトリにライブラリをインストールしてください。ビルドが成功すると他プロジェクトからライブラリを参照できるようになります。

```
mvn clean install
```

## 使用方法
### 他のMavenプロジェクトから使用する
ライブラリを他のMavenプロジェクトから使用したい場合は、当該プロジェクトのpom.xmlの&lt;dependencies&gt;に以下を追加してください。

```
<dependency>
    <groupId>com.lmt</groupId>
    <artifactId>archive-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

### ライブラリのリファレンス(Javadoc)
最新版のリファレンスは以下を参照してください。<br>
準備中...

## 変更履歴
[CHANGELOG.md](https://github.com/j-son3/archive-library/blob/main/CHANGELOG.md)を参照してください。
