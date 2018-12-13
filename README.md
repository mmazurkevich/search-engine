# In-memory search engine

This project was created for quick indexation and simple search of documents in the file system. Library concurrently index documents in the given folder.
It's stored indexed documents in the memory.  That's why it's important to consume less memory without performance loss. Under the hood project implements [Radix tree](https://en.wikipedia.org/wiki/Radix_tree) for effective storing indexed lexems. Users can extend this index by using it's own tokenizer by implementing `Tokenizer` interface and put it during `SearchEngine` class instantiation:
```
Tokenizer tokenizer = new WhitespaceTokenizer();
SearchEngine searchEngine = new SearchEngine(tokenizer);
```

For testing existing library project contains `app` module which provides an opportunity for indexing document or folder. **NOTE: during selection folder for indexation on MacOS
be aware that field `File:`  in finder is empty. It's a problem of default bihaviour of standart finder.**
To start an application simply execute the following command in the folder of the project: 
```
./gradlew run
```
If you want to run executable jar: 
```
./gradlew clean jar
java -jar app/build/libs/app-1.0-SNAPSHOT.jar
```
You can index some texts in the test resource folder `/engine/src/test/resources/shakespeare`. Ordinary indexation of 500MB of documents takes near 3min (3.1 GHz Intel Core i7, 16 GB 1867 MHz DDR3).  
## Future plans:
1. Implement partial locks for tree structure and allow to user branch locks, on branch which will be modified.
2. Implement more complex search handler which use `&&` `||` for query union.
3. Implement cache manager for handling similar queries to reduce queries count to the index (`SearchManager` call `CacheManager` if contains hit return the results. On the othe side do query to the index and cache results. Also should be subscribed to the changes in the index at `DocumentIndexManager` and reset the cache).
4. Implement notification logic from the `DocumentIndexManager` to get notifications about index changes.
