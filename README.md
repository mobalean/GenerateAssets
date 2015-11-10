# Generate Assets

This class is used to create 'assets' for an 'asset pipeline', similar to the asset pipeline implemented by Ruby on Rails. It does the following:

1. Delete old assets from destination folder (public/assets).
2. Create new asset subfolders, if necessary.
3. Process image files: calculate MD5 hashes, fingerprint, move to public/assets/img
4. Concatenate Javascript files, minimize, fingerprint and move to public/assets/js
5. Concatenate css files, minimize, fingerprint and move to public/assets/css
6. Write out public/assets/assets.txt.

## Usage

javac GenerateAssets.java
java GenerateAssets

## License

Copyright © 2015 Mobalean LLC

## Requirements

Java 1.7

