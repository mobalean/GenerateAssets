/**
 * GenerateAssets
 *
 * This class is used to create 'assets' for an 'asset pipeline', similar to
 * the asset pipeline implemented by Ruby on Rails.
 *
 * It does the following:
 *
 * 1. Delete old assets from destination folder (public/assets).
 * 2. Create new asset subfolders, if necessary.
 * 3. Process image files: calculate MD5 hashes, move to public/assets/img
 * 4. Concatenate Javascript files, minimize, fingerprint and move to public/assets/js
 * 5. Concatenate css files, minimize, fingerprint and move to public/assets/css
 * 6. Write out public/assets/assets.txt.
 *
 * Requirements: Java 1.7
 *
 * References:
 *
 * https://sites.google.com/site/matthewjoneswebsite/java/md5-hash-of-an-image
 * Matthew Jones (Matt Jones)
 * http://stackoverflow.com/questions/14676407/list-all-files-in-the-folder-and-also-sub-folders
 *
 * @author  Cleve Lendon (cleve@mobalean.com), Mobalean LLC, 2015
 *
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;

public class GenerateAssets {


   static String sourceFolder = "resources/assets";
   static String destinationFolder = "resources/public/assets";
   static String assetsFile = "resources/public/assets/assets.txt";


   static ArrayList<File> fileList = new ArrayList<File>();

   // filenameMap maps original file names to fingerprinted filenames.
   // For example:
   //     "img/image.jpg" --> "img/image-85d91d1c49f0e76c9f449248936a0f85.jpg"
   static Map<String, String> filenameMap = new HashMap();


   //---------------------------------------------------------
   public static void main(String[] args) {
      //Fingerprint.createAllAssets();
      createAllAssets();
   }
   //---------------------------------------------------------


   /**
    * createAllAssets
    *
    * This method deletes all the old assets from the public asset folder,
    * and creates new assets from the files in the resource asset folder.
    */
   public static void createAllAssets() {

      String sourceFolderFullPath = new File(sourceFolder).getAbsolutePath();

      // Delete old assets.
      getFileList(destinationFolder, fileList);
      System.out.println("Delete old assets.");
      for (File file : fileList) { file.delete(); }
      fileList.clear();

      // Create asset subfolders.
      getFileList(sourceFolder, fileList);
      createAssetFolders(fileList);
      fileList.clear();

      // Process image files.
      System.out.println("Create image assets.");
      getFileList(sourceFolder + "/img/", fileList);
      createImageAssets(fileList, filenameMap);
      fileList.clear();

      // Concatenate and fingerprint javascript.
      System.out.println("Concatenate and minify javascript assets.");
      getFileList(sourceFolder + "/js/", fileList);
      String jsFilename = "/js/assets";   // include subfolder, no ext
      createJavascriptAsset(fileList, jsFilename, filenameMap);
      fileList.clear();

      // Concatenate and fingerprint css.
      System.out.println("Concatenate and minify css assets.");
      getFileList(sourceFolder + "/css/", fileList);
      String cssFilename = "/css/assets";   // include subfolder, no ext
      createCssAsset(fileList, cssFilename, filenameMap);
      fileList.clear();

      System.out.println("Write assets file.");
      writeAssetsFile(assetsFile, filenameMap);

   }  // createAllAssets




   /*
    * writeAssetsFile
    *
    * Write out the filnameMap to the file 'assets.txt' in public/assets.
    * This files maps original filenames to fingerprinted filenames.
    *
    * The filename map collects the mappings of old to new filenames.
    *
    * @param  filename
    * @param  map - old filenames to new
    */
   static void writeAssetsFile(String filename, Map<String, String> filenameMap) {
      PrintWriter pw = openOutFile(filename, "UTF8");
      if (pw == null) return;
      pw.write("{\n");
      for (String key : filenameMap.keySet()) {
         pw.write("\"" + key + "\" \"" + filenameMap.get(key) + "\"\n");
      }
      pw.write("}\n");
      pw.close();
   }  // writeAssetsFile



   /*
    * createJavascriptAsset
    *
    * This method takes an array of javascript files and concatenates them into one file.
    * An MD5 hash is calculated, and the file is copied to the public asset folder with
    * a new name, for example:
    *
    *   public/assets/js/asset-240f985d91d1c49f0e78c9f44936a685.js
    *
    * The filename map receives the old filename / new filename entry.
    *
    * @param  array of js files
    * @parma  name of js asset file (without extension)
    * @param  map - old filenames to new
    */
   static void createJavascriptAsset(ArrayList<File> files,
                                     String jsFilename,
                                     Map<String, String> filenameMap) {

      String contents = concatMini(files);
      String hash     = getMD5(contents);
      String srcFn    = jsFilename + ".js";
      String destFn   = jsFilename + "-" + hash + ".js";
      PrintWriter pw  = openOutFile(destinationFolder + File.separator + destFn, "UTF8");
      pw.write(contents);
      pw.close();
      filenameMap.put(srcFn, destFn);

   }  // createJavascriptAsset


   /*
    * createCssAsset
    *
    * This method takes an array of css files and concatenates them into one file.
    * An MD5 hash is calculated, and the file is copied to the public asset folder
    * with a new name, for example:
    *
    *   public/assets/css/asset-240f985d91d1c49f0e78c9f44936a685.css
    *
    * The filename map receives the old filename / new filename entry.
    *
    * @param  array of css files
    * @parma  name of css asset file (without extension)
    * @param  map - old filenames to new
    */
   static void createCssAsset(ArrayList<File> files, String cssFilename,
                              Map<String, String> filenameMap) {

      String contents = concatMini(files);
      String hash     = getMD5(contents);
      String srcFn    = cssFilename + ".css";
      String destFn   = cssFilename + "-" + hash + ".css";
      PrintWriter pw  = openOutFile(destinationFolder + File.separator + destFn, "UTF8");
      pw.write(contents);
      pw.close();
      filenameMap.put(srcFn, destFn);

   }  // createCssAsset


   /*
    * createImageAssets
    *
    * This method takes an array of image asset files, calculates the MD5 hashes,
    * and copies the files to the public asset folder with fingerprinted names. Eg:
    *   public/assets/img/image-85d91d1c49f0e76c9f449248936a0f85.jpg
    *
    * The filename map collects the mappings of old to new filenames.
    *
    * @param  array of files
    * @param  map - old filenames to new
    */
   static void createImageAssets(ArrayList<File> files, Map<String, String> filenameMap) {

      String sourceFolderFullPath = new File(sourceFolder).getAbsolutePath();

      try {

         for (File file : files) {

            String srcFilename   = file.getName();
            String subfolder     = subfolderName(file, sourceFolderFullPath);
            String destFilename = fingerprintFilename(file);
            //System.out.println(subfolder + srcFilename + "  -->  " + subfolder + destFilename);
            filenameMap.put(subfolder + srcFilename, subfolder + destFilename);
            String destFolder = changePath(file, sourceFolder, destinationFolder);
            File destFile = new File(destFolder + File.separator + destFilename);
            Files.copy(file.toPath(), destFile.toPath());
         }

      }
      catch (IOException iox) {
         System.err.println("createImageAssets - failure.\n" + iox.toString());
      }
   }  // createImageAssets



   /*
    * createAssetFolders
    *
    * Create folders for destination assets, if the folders do not already exist.
    *
    * @param  array of files
    */
   static void createAssetFolders(ArrayList<File> files) {

      Set<String>  destFolderNames = new HashSet<String>();
      String sourceFolderFullPath = new File(sourceFolder).getAbsolutePath();

      for (File file : files) {
         String subfolder  = subfolderName(file, sourceFolderFullPath);
         destFolderNames.add(destinationFolder + subfolder);
      }

      for (String folderName : destFolderNames) {
        File folder = new File(folderName);
        if (!folder.exists()) {
           System.out.println("Create folder: " + folderName);
           folder.mkdir();
        }
      }

   }  // createAssetFolders



   /*
    * subfolderName
    *
    * This method gets the subfolder name of a file, relative to the given base path.
    *
    * For example, relative to the base:
    *     "/Users/user/www-clojure/simplesite/resources/assets/"
    * the file at:
    *     /Users/user/www-clojure/simplesite/resources/assets/img/klivo.jpg
    * has a subfolder name of:
    *     "img/"
    *
    * @param  file
    * @param  base path
    * @return subfolder name
    */
   static String subfolderName(File file, String basePath) {
      String srcFilename    = file.getName();
      String fullPath   = file.getAbsolutePath();
      return fullPath.substring(basePath.length(),
                                fullPath.length() - srcFilename.length());
   }


   /*
    * getFileList
    *
    * Get a list of files from the given folder and all subfolders.
    * Ignore filenames which start with period (.).
    *
    * @param  folderName
    * @param  collected file names
    */
   static void getFileList(String folderName, ArrayList<File> files) {

      String filename;
      File folder = new File(folderName);
      File[] listOfFiles = folder.listFiles();

      for (File file : listOfFiles) {
         if (file.isFile()) {
            filename = file.getName();
            if (filename.startsWith(".")) continue;
            files.add(file);
         }
         else
         if (file.isDirectory()) getFileList(file.getAbsolutePath(), files);
      }

   }  // getFileList


   /*
    * fingerprintFilename
    *
    * Calculates a file's MD5 hash and returns a file name with the hash included. Eg:
    *     picture-001.jpg
    * becomes
    *     picture-001-85d91d1c49f0e76c9f449248936a0f85.jpg
    *
    * @param  file object
    * @return fingerprinted name of file
    */
   static String fingerprintFilename(File file) {
      String md5Hash = getMD5(file);
      String name    = file.getName();
      int index = name.lastIndexOf(".");
      String name2 = name.substring(0, index);
      String ext = name.substring(index);
      return name2 + "-" + md5Hash + ext;
   }


   /*
    * changePath
    *
    * This method takes the path of a file, and replaces the source string
    * with the destination string. For example:
    *
    *    "resources/assets/"
    * is replaced with
    *    "resources/public/assets/"
    */
   static String changePath(File file, String source, String dest) {
      String path = file.getParent();
      return path.replaceAll(source, dest);
   }




   /*
    * getMD5 - Gets the MD5 hash for the given file.
    *
    * @param  file
    * @return hexadecimal hash (85d91d1c49f0e76c9f449248936a0f85)
    */
   static String getMD5(File file) {

      if (!file.exists()) {
         System.err.println("GetMD5: Unknown file: " + file.getName());
         return "???";
      }

      int MAX = 500000;
      byte[] imageBuffer = new byte[MAX];

      try {
         BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
         int count = in.read(imageBuffer, 0, MAX);
      }
      catch (IOException iox) {
         System.err.println("getMD5: IO error: " + file.getName());
         return "???";
      }

      byte[] hash = null;
      try {
         MessageDigest md = MessageDigest.getInstance("MD5");
         md.update(imageBuffer);
         hash = md.digest();
      }
      catch (NoSuchAlgorithmException nsax) {
         System.err.println("getMD5: No Such Algorithm.");
         return "???";
      }

      StringBuilder sb = new StringBuilder();
      for (byte b : hash) { sb.append(String.format("%02x", b)); }
      return sb.toString();

   }  // getMD5()



   /*
    * getMD5 - Gets the MD5 hash for the given string.
    *
    * @param  string
    * @return hexadecimal hash (85d91d1c49f0e76c9f449248936a0f85)
    */
   static String getMD5(String s) {

      byte[] buf = s.getBytes(StandardCharsets.UTF_8);
      byte[] hash = null;

      try {
         MessageDigest md = MessageDigest.getInstance("MD5");
         md.update(buf);
         hash = md.digest();
      }
      catch (NoSuchAlgorithmException nsax) {
         System.err.println("getMD5: No Such Algorithm. (2)");
         return "???";
      }

      StringBuilder sb = new StringBuilder();
      for (byte b : hash) { sb.append(String.format("%02x", b)); }
      return sb.toString();

   }  // getMD5()




   /*
    * openOutFile
    *
    * Opens a file for text output.
    *
    * @param  filename
    * @param  encoding (UTF8 ktp.)
    * @return Printwriter
    */
   static PrintWriter openOutFile(String filename, String encoding) {
      try {
         File file = new File(filename);
         if (file.exists()) file.delete();
         return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
      }
      catch (Exception ex) {
         System.out.println("Cannot open output file " + filename + " .\n" + ex.toString());
         return null;
      }
   }


   /*
    * fileToString
    *
    * Gets the entire contents of a file as a string.
    *
    * @param  filename
    * @return contents (string)
    */
   static String fileToString(String filename) {
      String s;
      try {
         s = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
      }
      catch (IOException iox) {
         System.out.println("Cannot read " + filename + " .\n" + iox.toString());
         return "???\n";
      }
      return s;
   }


   /*
    * minify
    *
    * Minify a javascript source file. Remove comments, leading spaces and blank lines.
    *
    * Trying to remove white spaces around + and = etc. is difficult and risky.
    * This solution seems adequate.
    *
    * Comment about YUI compressor:
    *   "C-style comments starting with /*! are preserved. This is useful with
    *   comments containing copyright/license information."
    * This method supports this convention.
    *
    * @param  string
    * @return minified string
    */
   static String minify(String s) {
      // Remove /* */
      String s1 = s.replaceAll("/\\*[^!].*\\*/", "");
      // Remove //
      // Warning: '//' can appear in a regex (eg: Fb=/^\/\//).
      // Check for white space in front.
      String s2 = s1.replaceAll("\\s//.*?\n","\n");
      String s3 = s2.replaceAll("[\n\r][ \t]+","\n");   // Remove leading spaces
      String s4 = s3.replaceAll("[\n\r]+", "\n");   // Remove extra blank lines
      return s4;
   }



   /*
    * concatMini
    *
    * Concatinate and minify the list of files:
    *
    * @param   list of files
    * @return  concatinated file as string
    */
   static String concatMini(ArrayList<File> files) {
      StringBuilder sb = new StringBuilder("");
      for (File file : files) {
         String fn = file.getAbsolutePath();
         String content = fileToString(fn);
         String minified = minify(content);
         sb.append(minified);
      }
      return sb.toString();
   }  // concatMini


}  // GenerateAssets


