package com.test.streamingserver;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * used with permission http://upshots.org/android/android-file-utility-class
 *
 * Created by Mike Dunn on 8/11/15.
 *
 * Java 7 has the Files class with helper methods, and apache commons has FileUtils, but doesn't come stand-alone
 *
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/FileUtils.java
 * https://android.googlesource.com/platform/tools/tradefederation/+/dfd83b4c73cdb2ac0c2459f90b6caed8642cf684/src/com/android/tradefed/util/FileUtil.java
 * https://github.com/Trinea/android-common/blob/master/src/cn/trinea/android/common/util/FileUtils.java
 * http://grepcode.com/file/repo1.maven.org/maven2/commons-io/commons-io/1.4/org/apache/commons/io/FileUtils.java
 */
public class FileUtils {

  private static final String CANNOT_CREATE_DIRECTORY_EXCEPTION_MESSAGE = "Could not create directory %s";

  private static final String COMPRESSION_ENCODING = "ISO-8859-1";
  private static final int COMPRESSION_BUFFER_SIZE = 32;

  private static final String FILE_SCHEME_PROTOCOL = "file://";

  /**
   * Write a String to a File, with option to append instead of overwrite.
   *
   * @param content The input {@link String}.
   * @param file    The destination file to write to.
   * @param append  Option to append.
   * @return File that was written to.
   * @throws IOException the io exception
   */
  public static File writeToFile(String content, File file, boolean append) throws IOException {
    return writeToFile(streamFromString(content), file, append);
  }

  /**
   * Write a String to a File
   *
   * @param content The input {@link String}.
   * @param file    The destination file to write to.
   * @return File that was written to.
   * @throws IOException the io exception
   */
  public static File writeToFile(String content, File file) throws IOException {
    return writeToFile(content, file, false);
  }

  /**
   * Write to file file.
   *
   * @param content the content
   * @param path    the path
   * @param append  the append
   * @return the file
   * @throws IOException the io exception
   */
  public static File writeToFile(String content, String path, boolean append) throws IOException {
    return writeToFile(content, new File(path), append);
  }

  /**
   * Write to file file.
   *
   * @param content the content
   * @param path    the path
   * @return the file
   * @throws IOException the io exception
   */
  public static File writeToFile(String content, String path) throws IOException {
    return writeToFile(content, new File(path), false);
  }

  /**
   * Write to file file.
   *
   * @param inputStream the input stream
   * @param path        the path
   * @param append      the append
   * @return the file
   * @throws IOException the io exception
   */
  public static File writeToFile(InputStream inputStream, String path, boolean append) throws IOException {
    return writeToFile(inputStream, new File(path), append);
  }

  /**
   * Write to file file.
   *
   * @param inputStream the input stream
   * @param path        the path
   * @return the file
   * @throws IOException the io exception
   */
  public static File writeToFile(InputStream inputStream, String path) throws IOException {
    return writeToFile(inputStream, new File(path), false);
  }

  /**
   * Write to file file.
   *
   * @param inputStream the input stream
   * @param file        the file
   * @return the file
   * @throws IOException the io exception
   */
  public static File writeToFile(InputStream inputStream, File file) throws IOException {
    return writeToFile(inputStream, file, false);
  }

  /**
   * FileWriter API is a little more simple, but according to https://docs.oracle
   * .com/javase/7/docs/api/java/io/FileWriter.html, it's intended for _character_ files. I imagine it probably works
   * on "binary" files (e.g., videos), but that description seems to imply that's not a contract.  Copying streams
   * should be reliable, very backwards compatible and agnostic about things like encoding.
   *
   * @param inputStream the input stream
   * @param file        the file
   * @param append      the append
   * @return The file written to
   * @throws IOException the io exception
   */
  public static File writeToFile(InputStream inputStream, File file, boolean append) throws IOException {
    if (!file.exists()) {
      // file.getParentFile() is null, presumably because of android specific file system settings
      getParent(file).mkdirs();
    }
    copyStreams(inputStream, new FileOutputStream(file, append));
    return file;
  }

  /**
   * A helper method that copies a file's contents to a local file
   *
   * @param source      the original file to be copied
   * @param destination the destination file
   * @param overwrite   true if the operation should write over an existing file
   * @return the file
   * @throws IOException if failed to copy file
   */
  public static File copyFile(File source, File destination, boolean overwrite) throws IOException {
    if (!overwrite && destination.exists()) {
      return destination;
    }
    return writeToFile(new FileInputStream(source), destination);
  }

  /**
   * A helper method that copies a file's contents to a local file.
   * This signature overwrites an existing copy.
   *
   * @param source      the original file to be copied
   * @param destination the destination file
   * @return the file
   * @throws IOException if failed to copy file
   */
  public static File copyFile(File source, File destination) throws IOException {
    return copyFile(source, destination, true);
  }

  /**
   * A helper method that copies a file's contents to a local file.
   *
   * @param sourcePath      the source path
   * @param destinationPath the destination path
   * @param overwrite       true if the operation should write over an existing file
   * @return the file
   * @throws IOException the io exception
   */
  public static File copyFile(String sourcePath, String destinationPath, boolean overwrite) throws IOException {
    return copyFile(new File(sourcePath), new File(destinationPath), overwrite);
  }

  /**
   * A helper method that copies a file's contents to a local file.
   * This signature overwrites an existing copy.
   *
   * @param sourcePath      the source path
   * @param destinationPath the destination path
   * @return the file
   * @throws IOException the io exception
   */
  public static File copyFile(String sourcePath, String destinationPath) throws IOException {
    return copyFile(sourcePath, destinationPath, true);
  }

  /**
   * Move file file.
   *
   * @param source      the source
   * @param destination the destination
   * @return the file
   * @throws IOException the io exception
   */
// from https://github.com/Trinea/android-common/blob/master/src/cn/trinea/android/common/util/FileUtils.java
  public static File moveFile(File source, File destination) throws IOException {
    boolean rename = source.renameTo(destination);
    if (!rename) {
      copyFile(source, destination);
      source.delete();
    }
    return destination;
  }

  /**
   * Move file file.
   *
   * @param sourcePath      the source path
   * @param destinationPath the destination path
   * @return the file
   * @throws IOException the io exception
   */
  public static File moveFile(String sourcePath, String destinationPath) throws IOException {
    return moveFile(new File(sourcePath), new File(destinationPath));
  }


  /**
   * Recursively copy folder contents.
   * <p/>
   * Only supports copying of files and directories - symlinks are not copied.
   *
   * @param source      the folder that contains the files to copy
   * @param destination the destination folder
   * @param overwrite   true if existing files should be copied over
   * @return the file
   * @throws IOException the io exception
   */
  public static File copyDirectory(File source, File destination, boolean overwrite) throws IOException {
    File[] children = source.listFiles();
    destination.mkdirs();
    if (children != null) {
      for (File sourceChild : children) {
        File destinationChild = new File(destination, sourceChild.getName());
        if (sourceChild.isDirectory()) {
          if (!destinationChild.exists() && !destinationChild.mkdirs()) {
            String exceptionMessage = String.format(Locale.US, CANNOT_CREATE_DIRECTORY_EXCEPTION_MESSAGE, destinationChild.getAbsolutePath());
            throw new IOException(exceptionMessage);
          }
          copyDirectory(sourceChild, destinationChild, overwrite);
        } else if (sourceChild.isFile()) {
          copyFile(sourceChild, destinationChild, overwrite);
        }
      }
    }
    return destination;
  }

  /**
   * Recursively copy folder contents.
   * Only supports copying of files and directories - symlinks are not copied.
   * This signature will overwrite existing files.
   *
   * @param source      the folder that contains the files to copy
   * @param destination the destination folder
   * @return the file
   * @throws IOException the io exception
   */
  public static File copyDirectory(File source, File destination) throws IOException {
    return copyDirectory(source, destination, true);
  }

  /**
   * Move the contents of a directory.
   *
   * File.renameTo is platform-dependant, and might (or might not) overwrite existing files,
   * for this reason if @param overwrite is false, don't even bother - skip directly to the #copyDirectory operation
   *
   * @param source      the source
   * @param destination the destination
   * @param overwrite   true if existing files should be overwritten
   * @return the file
   * @throws IOException the io exception
   */
  public static File moveDirectory(File source, File destination, boolean overwrite) throws IOException {
    boolean moved = !overwrite && source.renameTo(destination);
    if (!moved) {
      copyDirectory(source, destination);
      deleteDirectory(source);
    }
    return destination;
  }

  /**
   * Move the contents of a directory.
   *
   * @param source      the source
   * @param destination the destination
   * @return the file
   * @throws IOException the io exception
   */
  public static File moveDirectory(File source, File destination) throws IOException {
    return moveDirectory(source, destination, true);
  }

  /**
   * Delete contents boolean.
   *
   * @param file the file
   * @return the boolean
   */
  public static boolean deleteDirectory(File file) {
    boolean success = true;
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File child : files) {
        success &= deleteDirectory(child);
      }
    }
    return file.delete() && success;
  }

  /**
   * Copies contents of origStream to destStream.
   * <p/>
   * Recommended to provide a buffered stream for input and output
   *
   * @param inputStream  the {@link InputStream}
   * @param outputStream the {@link OutputStream}
   * @param shouldClose  the should close
   * @throws IOException the io exception
   */
  public static void copyStreams(InputStream inputStream, OutputStream outputStream, boolean shouldClose) throws
      IOException {
    if (!(inputStream instanceof BufferedInputStream)) {
      inputStream = new BufferedInputStream(inputStream);
    }
    if (!(outputStream instanceof BufferedOutputStream)) {
      outputStream = new BufferedOutputStream(outputStream);
    }
    try {
      int data = inputStream.read();
      while (data != -1) {
        outputStream.write(data);
        data = inputStream.read();
      }
    } finally {
      if (shouldClose) {
        closeStream(inputStream);
        closeStream(outputStream);
      }
    }
  }

  /**
   * Copy streams.
   *
   * @param inputStream  the input stream
   * @param outputStream the output stream
   * @throws IOException the io exception
   */
  public static void copyStreams(InputStream inputStream, OutputStream outputStream) throws
      IOException {
    copyStreams(inputStream, outputStream, true);
  }

  /**
   * Gets files.
   *
   * @param target the target
   * @param out    the out
   * @return the files
   * @throws IOException the io exception
   */
  public static Collection<File> getFiles(File target, Collection<File> out) throws IOException {
    if (target.isDirectory()) {
      for (File child : target.listFiles()) {
        getFiles(child, out);
      }
    } else if (target.isFile()) {
      out.add(target);
    }
    return out;
  }

  /**
   * Gets files.
   *
   * @param target the target
   * @return the files
   * @throws IOException the io exception
   */
  public static Collection<File> getFiles(File target) throws IOException {
    return getFiles(target, new HashSet<File>());
  }

  /**
   * Read file string.
   *
   * @param file the file
   * @return the string
   * @throws IOException the io exception
   */
  public static String readFile(File file) throws IOException {
    return stringFromStream(new FileInputStream(file));
  }

  /**
   * Read file string.
   *
   * @param path the path
   * @return the string
   * @throws IOException the io exception
   */
  public static String readFile(String path) throws IOException {
    return readFile(new File(path));
  }

  /**
   * Read file from assets string.
   *
   * @param context the context
   * @param path    the path
   * @return the string
   * @throws IOException the io exception
   */
  public static String readFileFromAssets(Context context, String path) throws IOException {
    return stringFromStream(context.getAssets().open(path));
  }

  /**
   * Read file from raw resource string.
   *
   * @param context    the context
   * @param resourceId the resource id
   * @return the string
   * @throws IOException the io exception
   */
  public static String readFileFromRawResource(Context context, int resourceId) throws IOException {
    return stringFromStream(context.getResources().openRawResource(resourceId));
  }

  /**
   * Gets path separator.
   *
   * @return the path separator
   */
  public static String getPathSeparator() {
    return File.separator;
  }

  /**
   * Gets line separator.
   *
   * @return the line separator
   */
  public static String getLineSeparator() {
    return System.getProperty("line.separator");
  }

  /**
   * String from stream string.
   *
   * @param bufferedReader the buffered reader
   * @return the string
   * @throws IOException the io exception
   */
  public static String stringFromStream(BufferedReader bufferedReader) throws IOException {
    try {
      int character = bufferedReader.read();
      StringBuilder builder = new StringBuilder();
      while (character != -1) {
        builder.append((char) character);
        character = bufferedReader.read();
      }
      return builder.toString();
    } finally {
      closeStream(bufferedReader);
    }
  }

  /**
   * String from stream string.
   *
   * @param inputStream the input stream
   * @return the string
   * @throws IOException the io exception
   */
  public static String stringFromStream(InputStream inputStream) throws IOException {
    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    return stringFromStream(inputStreamReader);
  }

  /**
   * String from stream string.
   *
   * @param inputStreamReader the input stream reader
   * @return the string
   * @throws IOException the io exception
   */
  public static String stringFromStream(InputStreamReader inputStreamReader) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    return stringFromStream(bufferedReader);
  }

  /**
   * Stream from string input stream.
   *
   * @param content the content
   * @return the input stream
   */
  public static InputStream streamFromString(String content) {
    byte[] bytes = content.getBytes();
    return new ByteArrayInputStream(bytes);
  }


  private static void closeStream(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        // bleh
      }
    }
  }

  /**
   * Gets parent.
   *
   * @param file the file
   * @return the parent
   */
  public static File getParent(File file) {
    return new File(getParentPath(file));
  }

  // START android.os.FileUtils

  /**
   * Test if a file lives under the given directory, either as a direct child
   * or a distant grandchild.
   * <p>
   * Both files <em>must</em> have been resolved using
   * {@link File#getCanonicalFile()} to avoid symlink or path traversal
   * attacks.
   *
   * @param directory the directory
   * @param file      the file
   * @return the boolean
   */
  public static boolean contains(File directory, File file) {
    if (directory == null || file == null) {
      return false;
    }
    String directoryAbsolutePath = directory.getAbsolutePath();
    String fileAbsolutePath = file.getAbsolutePath();
    if (directoryAbsolutePath.equals(fileAbsolutePath)) {
      return true;
    }
    directoryAbsolutePath = getNormalizedDirectoryPath(directoryAbsolutePath);
    return fileAbsolutePath.startsWith(directoryAbsolutePath);
  }


  // https://android.googlesource.com/platform/tools/tradefederation/+/dfd83b4c73cdb2ac0c2459f90b6caed8642cf684/src/com/android/tradefed/util/FileUtil.java
  // tradefed is an android testing framework, not sure how it fits into history, but their fileutils class has some
  // nice stuff

  /**
   * Compare file contents boolean.
   *
   * @param file1 the file 1
   * @param file2 the file 2
   * @return the boolean
   * @throws IOException the io exception
   */
  public static boolean compareFiles(File file1, File file2) throws IOException {
    BufferedInputStream stream1 = null;
    BufferedInputStream stream2 = null;
    try {
      stream1 = new BufferedInputStream(new FileInputStream(file1));
      stream2 = new BufferedInputStream(new FileInputStream(file2));
      boolean eof = false;
      while (!eof) {
        int byte1 = stream1.read();
        int byte2 = stream2.read();
        if (byte1 != byte2) {
          return false;
        }
        eof = byte1 == -1;
      }
      return true;
    } finally {
      closeStream(stream1);
      closeStream(stream2);
    }
  }

  /**
   * Gets size.
   *
   * @param file the file
   * @return the size
   */
  public static long getSize(File file) {
    long size = 0;
    if (file.isFile()) {
      size += file.length();
    } else if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        size += getSize(child);
      }
    }
    return size;
  }

  // String paths

  /**
   * Gets the extension for given file name.
   *
   * @param fileName the file name
   * @return the extension or empty String if file has no extension
   */
  public static String getExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    return (index == -1) ? null : fileName.substring(index + 1);
  }

  /**
   * Gets extension.
   *
   * @param file the file
   * @return the extension
   */
  public static String getExtension(File file) {
    return getExtension(file.getName());
  }

  /**
   * Gets the base name, without extension, of given file name.
   * <p/>
   * e.g. getBaseName("file.txt") will return "file"
   *
   * @param fileName the file name
   * @return the base name
   */
  public static String getBaseName(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index == -1) {
      return fileName;
    } else {
      return fileName.substring(0, index);
    }
  }

  /**
   * Gets base name.
   *
   * @param file the file
   * @return the base name
   */
  public static String getBaseName(File file) {
    return getBaseName(file.getName());
  }

  /**
   * Gets parent path.
   *
   * @param path the path
   * @return the parent path
   */
  public static String getParentPath(String path) {
    return path.substring(0, path.lastIndexOf(getPathSeparator()));
  }

  /**
   * Gets parent path.
   *
   * @param file the file
   * @return the parent path
   */
  public static String getParentPath(File file) {
    return getParentPath(file.getAbsolutePath());
  }

  /**
   * Gets path from components.
   *
   * @param components the components
   * @return the path from components
   */
  public static String getPathFromComponents(String... components) {
    return getPathFromComponents(false, components);
  }

  /**
   * Gets path from components.
   *
   * @param isDirectory the is directory
   * @param components  the components
   * @return the path from components
   */
  public static String getPathFromComponents(boolean isDirectory, String... components) {
    String delimiter = getPathSeparator();
    StringBuilder stringBuilder = new StringBuilder();
    for (String component : components) {
      stringBuilder.append(component);
      stringBuilder.append(delimiter);
    }
    if (!isDirectory && stringBuilder.length() > 0) {
      stringBuilder.setLength(stringBuilder.length() - 1);
    }
    return stringBuilder.toString();
  }

  /**
   * Gets file from components.
   *
   * @param components the components
   * @return the file from components
   */
  public static File getFileFromComponents(String... components) {
    return getFileFromComponents(false, components);
  }

  /**
   * Gets file from components.
   *
   * @param isDirectory the is directory
   * @param components  the components
   * @return the file from components
   */
  public static File getFileFromComponents(boolean isDirectory, String... components) {
    return new File(getPathFromComponents(isDirectory, components));
  }

  /**
   * Gets normalized directory path.
   *
   * @param path the path
   * @return the normalized directory path
   */
  public static String getNormalizedDirectoryPath(String path) {
    String separator = getPathSeparator();
    return path.endsWith(separator) ? path : (path + separator);
  }

  /**
   * Returns a string path of a File, excluding some ancestor.
   *
   * E.g.
   * File file = new File("path/to/some/file.dat");
   * File directory = new File("path/to");
   * getPathWithoutDirectory(file, directory) would yield "some/file.dat"
   *
   * @param file
   * @param directory
   * @return
   */
  public static String getPathWithoutDirectory(File file, File directory) {
    String filePath = file.getAbsolutePath();
    String directoryPath = directory.getAbsolutePath();
    if (filePath.startsWith(directoryPath)) {
      filePath = filePath.substring(directoryPath.length());
      if (filePath.startsWith("/")) {
        filePath = filePath.substring(1);
      }
    }
    return filePath;
  }

  /**
   * Get a file scheme from a path, e.g., /data/user/9/com.safariflow.queue/files/content/user-01/book-02
   * becomes file:///data/user/9/com.safariflow.queue/files/content/user-01/book-02
   *
   * @param path
   * @return The file scheme.
   */
  public static String getFileSchemeFromPath(String path) {
    return FILE_SCHEME_PROTOCOL + path;
  }

  // compression

  /**
   * Compress string.
   *
   * @param uncompressed the uncompressed
   * @return the string
   */
  public static String compress(String uncompressed) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
      gzipOutputStream.write(uncompressed.getBytes());
      gzipOutputStream.close();
      return byteArrayOutputStream.toString(COMPRESSION_ENCODING);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @param compressed
   * @return
   */
  public static String uncompress(String compressed) {
    try {
      byte[] compressedBytes = compressed.getBytes(COMPRESSION_ENCODING);
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
      GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream, COMPRESSION_BUFFER_SIZE);
      return stringFromStream(gzipInputStream);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // SD Card

  /**
   * Gets confirmed removable sd card directory.
   *
   * @param context the context
   * @return the confirmed removable sd card directory
   */
  public static File getConfirmedRemovableSDCardDirectory(Context context) {
    if (Environment.isExternalStorageRemovable()) {
      return Environment.getExternalStorageDirectory();
    }

    File[] directories = ContextCompat.getExternalFilesDirs(context, null);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      for (File directory : directories) {
        // directory will be null for an unmounted SD card
        if (directory != null && Environment.isExternalStorageRemovable(directory)) {
          return directory;
        }
      }
    }

    return null;
  }

  /**
   * Get possible removable sd card directories file [ ].
   *
   * The returned array might contain null values if a directory is unavailable.
   *
   * @param context the context
   * @return the file [ ]
   */
  public static File[] getPossibleRemovableSDCardDirectories(Context context) {
    File confirmedLocation = getConfirmedRemovableSDCardDirectory(context);
    if (confirmedLocation != null) {
      return new File[]{confirmedLocation};
    }
    return ContextCompat.getExternalFilesDirs(context, null);
  }

  // zip

  /**
   * Utility method to extract entire contents of zip file into given directory
   *
   * @param zipFile     the {@link ZipFile} to extract
   * @param destination the local dir to extract file to
   * @throws IOException if failed to extract file
   */
  public static void extractZip(ZipFile zipFile, File destination) throws IOException {
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      File file = new File(destination, entry.getName());
      if (entry.isDirectory()) {
        file.mkdirs();
      } else {
        writeToFile(zipFile.getInputStream(entry), file);
      }
    }
  }

  /**
   * Utility method to create a temporary zip file containing the given directory and
   * all its contents.
   *
   * @param file        the directory to zip
   * @param destination the destination
   * @return a temporary zip {@link File} containing directory contents
   * @throws IOException if failed to create zip file
   */
  public static File createZip(File file, File destination) throws IOException {
    ZipOutputStream zipOutputStream = null;
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(destination);
      zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream));
      addToZip(zipOutputStream, file, file.getName());
    } finally {
      closeStream(zipOutputStream);
    }
    return destination;
  }

  /**
   * Create zip file.
   *
   * @param file the file
   * @return the file
   * @throws IOException the io exception
   */
  public static File createZip(File file) throws IOException {
    File zip = new File(file.getParent() + getPathSeparator() + file.getName() + ".zip");
    return createZip(file, zip);
  }

  /**
   * Recursively adds given file and its contents to ZipOutputStream
   *
   * @param zipOutputStream the {@link ZipOutputStream}
   * @param file            the {@link File} to add to the stream
   * @param pathInZip       the path to the file as it should appear in the zip
   * @throws IOException if failed to add file to zip
   */
  private static void addToZip(ZipOutputStream zipOutputStream, File file, String pathInZip) throws IOException {
    if (file.isDirectory()) {
      pathInZip = getNormalizedDirectoryPath(pathInZip);
    }
    ZipEntry zipEntry = new ZipEntry(pathInZip);
    zipOutputStream.putNextEntry(zipEntry);
    if (file.isFile()) {
      copyStreams(new FileInputStream(file), zipOutputStream, false);
    }
    zipOutputStream.closeEntry();
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File child : files) {
        addToZip(zipOutputStream, child, pathInZip + child.getName());
      }
    }
  }

  /**
   * Removes file type extension from a path
   *
   * @param filename
   * @return
   */
  public static String filenameWithoutExtension(String filename) {
    if (filename == null || filename.length() == 0) {
      return filename;
    }
    int index = filename.lastIndexOf('.');
    if (index != -1) {
      int separatorIndex = filename.lastIndexOf(File.separator);
      if (separatorIndex < index) {
        return filename.substring(0, index);
      }
    }
    return filename;
  }

}
