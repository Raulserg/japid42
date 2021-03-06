package cn.bran.japid.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.template.JapidRenderer;

public class DirUtil {
	public static final String OF = ", ";
	public static final String LINE_MARKER = "// line ";
	static final String DOT_SUB = "_d_";

	private static final String[] ALL_EXTS = new String[] { ".java", ".html", ".js", ".txt", ".css", ".json", ".xml" };
	private static final String[] TEMPLATE_EXTS = new String[] { ".html", ".js", ".txt", ".css", ".xml", ".json" };

	public static Set<File> findOrphanJava(File src, File target) {
		if (target == null)
			target = src;
		String[] allSrc = getAllFileNames(src, ALL_EXTS);
		Set<String> javas = new HashSet<String>();
		Set<String> srcFiles = new HashSet<String>();

		for (String s : allSrc) {
			if (s.endsWith(".java")) {
				javas.add(s);
			} else /* if (s.endsWith(".html")) */{
				srcFiles.add(mapSrcToJava(s));
			}
		}

		javas.removeAll(srcFiles);
		Set<File> re = new HashSet<File>();
		for (String j : javas) {
			re.add(new File(j));
		}
		return re;
	}

	/**
	 * 
	 */
	public static String[] getAllFileNames(File dir, String[] exts) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, exts);
		String[] ret = new String[files.size()];
		return files.toArray(ret);
	}

	public static String[] getAllTemplateHtmlFiles(File dir) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, new String[] { ".html" });
		// should filter out bad named files
		String[] ret = new String[files.size()];
		return files.toArray(ret);
	}

	public static String[] getAllTemplateFileNames(File dir) {
		List<String> files = new ArrayList<String>();
		getAllFileNames("", dir, files, TEMPLATE_EXTS);
		// should filter out bad named files
		String[] ret = new String[files.size()];
		return files.toArray(ret);
	}

	public static String[] getAllTemplateFileNames(String... dirs) {
		List<String> re = new ArrayList<String>();

		for (String dir : dirs) {
			File dirF = new File(dir);
			if (dirF.exists()) {
				List<String> files = new ArrayList<String>();
				getAllFileNames("", dirF, files, TEMPLATE_EXTS);
				re.addAll(files);
			}
		}
		// should filter out bad named files
		String[] ret = new String[re.size()];
		return re.toArray(ret);
	}

	/**
	 * collect all files with one of the extensions from the directory.
	 * 
	 * @param dir
	 * @param exts
	 * @param fs
	 * @return the files match. Note the files path starts with the source dir.
	 */
	public static Set<File> getAllFiles(File dir, String[] exts, Set<File> fs) {
		Set<File> scanFiles = scanFiles(dir, exts, fs);
		return scanFiles;
	}

	/**
	 * @param dir
	 * @param exts
	 * @param fs
	 * @return
	 */
	private static Set<File> scanFiles(File dir, String[] exts, Set<File> fs) {
		File[] flist = dir.listFiles();
		for (File f : flist) {
			if (f.isDirectory())
				getAllFiles(f, exts, fs);
			else {
				if (match(f, exts))
					fs.add(f);
			}
		}
		return fs;
	}

	/**
	 * retrieve all the files with specified extensions recursively in a
	 * directory and its sub-directory
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param dir
	 * @param exts
	 * @return
	 */
	public static Set<File> scanFiles(File dir, String[] exts) {
		Set<File> files = new HashSet<File>();
		for (File f : dir.listFiles()) {
			if (f.isDirectory())
				files.addAll(scanFiles(f, exts));
			else {
				if (match(f, exts))
					files.add(f);
			}
		}
		return files;
	}

	public static Set<File> getAllTemplateFiles(File root) {
		File dir = new File(root, "japidviews"); // is this requirement really
													// necessary?
		if (!dir.exists())
			throw new RuntimeException(dir.getPath() + " does not exist.");
		return scanFiles(dir, TEMPLATE_EXTS);
	}

	private static void getAllFileNames(String leadingPath, File dir, List<String> files, String[] exts) {
		if (!dir.exists())
			return;
		// throw new RuntimeException("directory exists? " + dir.getPath());

		File[] flist = dir.listFiles();
		for (File f : flist) {
			if (f.isDirectory())
				getAllFileNames(leadingPath + f.getName() + File.separatorChar, f, files, exts);
			else {
				if (match(f, exts))
					files.add(leadingPath + f.getName());
			}
		}
	}

	static boolean match(File f, String[] exts) {
		for (String ext : exts) {
			if (f.getName().endsWith(ext))
				if (fileNameIsValidClassName(f))
					return true;
		}
		return false;
	}

	// private static boolean firstTimeDetectingChanges = true;
//	 private static final ThreadLocal<Set<File>> versionCheckedDirs = new ThreadLocal<Set<File>>() {
//             @Override protected Set<File> initialValue() {
//            	 return new HashSet<File>();
//             }
//	 };
			 
	static private Set<File> versionCheckedDirs = new HashSet<File>();
	public static String curVersion;
	
	public static List<File> findChangedSrcFiles(File srcDir) {
		// if (target == null)
		// target = src;
		// String srcPath = src.getPath();
		Set<File> allSrc = new HashSet<File>();
		allSrc = getAllFiles(srcDir, ALL_EXTS, allSrc);

		// <name, last modified time>
		Map<String, Long> javaFiles = new HashMap<String, Long>();
		Map<String, Long> scriptFiles = new HashMap<String, Long>();

		long now = System.currentTimeMillis();
		boolean hasFutureTimestamp = false;

		for (File f : allSrc) {
			String path = f.getPath();
			long modi = f.lastModified();
			// System.out.println("file: " + path + ":" + modi);
			if (path.endsWith(".java")) {
				if (modi > now) {
					// for some reason(e.g., copies from other file system), the last modified time is in the future
					// let's reset it.
					hasFutureTimestamp = true;
					f.setLastModified(now - 1);
				}
				javaFiles.put(path, modi);
			} else {
				// validate file name to filter out dubious files such as
				// temporary files
				if (fileNameIsValidClassName(f)) {
					if (modi > now) {
						// for some reason(e.g., copies from other file system), the last modified time is in the future
						// let's reset it.
						hasFutureTimestamp = true;
						f.setLastModified(now);
					}
					scriptFiles.put(path, modi);
				}
			}
		}

		if (hasFutureTimestamp) {
			JapidFlags.warn("Some of the Japid files have a timestamp in the future. It could have been caused by out-of-synch system time.");
		}

		List<File> changedScripts = new ArrayList<File>();
		
		boolean japidVersionChecked = versionChecked(srcDir);
		if (!japidVersionChecked)
			JapidFlags.debug("to version-check the japid scripts in: " + srcDir + ". Current version is: " + curVersion);

		for (String script : scriptFiles.keySet()) {
			String javaFileName = mapSrcToJava(script);
			File javaFile = new File(javaFileName);
			Long javaTimestamp = javaFiles.get(javaFileName);

			if (javaTimestamp == null) {
				// how can this happen?
				// javaFile.delete();
				changedScripts.add(new File(script));
			} else {
				Long srcStamp = scriptFiles.get(script);
				if (srcStamp.compareTo(javaTimestamp) > 0) {
					// javaFile.delete();
					changedScripts.add(new File(script));
				} else {
					// if (firstTimeDetectingChanges) {
					if (!japidVersionChecked) {
						// check the japid version that the java code was
						// generated by
						if (javaFile.exists()) {
							String firstLine = JapidRenderer.readFirstLine(javaFile);
							if (firstLine.startsWith(AbstractTemplateClassMetaData.VERSION_HEADER)) {
								firstLine = firstLine.substring(AbstractTemplateClassMetaData.VERSION_HEADER.length())
										.trim();
								
								if (!curVersion.equals(firstLine)) {
									JapidFlags.debug("japid version mismatch. to refresh " + javaFileName);
									// javaFile.delete();
									changedScripts.add(new File(script));
								}
								else {
//									JapidFlags.debug("japid versions match for " + javaFileName);
								}
							} else {
								JapidFlags.debug("japid version mismatch. to refresh " + javaFileName);
								// javaFile.delete();
								changedScripts.add(new File(script));
							}
						}
					}
				}
			}
		}
//		versionCheckedDirs.get().add(srcDir);
		versionCheckedDirs.add(srcDir);
		// firstTimeDetectingChanges = false;
		return changedScripts;
	}

	private static boolean versionChecked(File srcDir) {
		return versionCheckedDirs.contains(srcDir);
//		return versionCheckedDirs.get().contains(srcDir);
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param f
	 * @return
	 */
	private static boolean fileNameIsValidClassName(File f) {
		String fname = f.getName();
		if (fname.startsWith("."))
			return false;
		fname = fname.substring(0, fname.lastIndexOf(".")).replace('.', '_');
		return isClassname(fname);
	}

	/**
	 * map template source file name to the generated java file name
	 * 
	 * @param k
	 * @return
	 */
	public static String mapSrcToJava(String k) {
		if (k.endsWith(".java"))
			return k;
		if (k.endsWith(".txt")) {
			return getRoot(k) + "_txt" + ".java";
		} else if (k.endsWith(".xml")) {
			return getRoot(k) + "_xml" + ".java";
		} else if (k.endsWith(".json")) {
			return getRoot(k) + "_json" + ".java";
		} else if (k.endsWith(".css")) {
			return getRoot(k) + "_css" + ".java";
		} else if (k.endsWith(".js")) {
			return getRoot(k) + "_js" + ".java";
		} else { // including html

			return getRoot(k) + ".java";
		}
	}

	/**
	 * 
	 * map java source file name to the template file name
	 * 
	 * @param k
	 * @return
	 */
	public static String mapJavaToSrc(String k) {
		return rawConvert(k);// .replaceAll(DOT_SUB, ".");
	}

	private static String rawConvert(String k) {
		if (k.endsWith(".java"))
			k = k.substring(0, k.lastIndexOf(".java"));

		if (k.endsWith("_txt")) {
			return k.substring(0, k.lastIndexOf("_txt")) + ".txt";
		} else if (k.endsWith("_xml")) {
			return k.substring(0, k.lastIndexOf("_xml")) + ".xml";
		} else if (k.endsWith("_json")) {
			return k.substring(0, k.lastIndexOf("_json")) + ".json";
		} else if (k.endsWith("_css")) {
			return k.substring(0, k.lastIndexOf("_css")) + ".css";
		} else if (k.endsWith("_js")) {
			return k.substring(0, k.lastIndexOf("_js")) + ".js";
		} else { // including html
			return k + ".html";
		}
	}

	private static String getRoot(String k) {
		for (String ext : TEMPLATE_EXTS) {
			if (k.endsWith(ext)) {
				String sub = k.substring(0, k.lastIndexOf(ext));
				// String first = "";
				// if (sub.startsWith(".")) { // keep the first dot, which means
				// current dir
				// first = ".";
				// sub = sub.substring(1);
				// }
				// return first + sub.replaceAll("\\.", DOT_SUB);
				return sub;
			}
		}
		return k;
	}

	public static void writeStringToFile(File file, String content) throws IOException {
		Writer fw = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append(content);
		bw.close();
	}

	public static void touch(File newer) throws IOException {
		writeStringToFile(newer, "");
	}

	public static boolean containsTemplateFiles(String root, String dirName) {
		String sep = File.separator;
		String japidViews = root + sep + JAPIDVIEWS_ROOT + sep;
		String dir = japidViews + dirName;
		return containsTemplatesInDir(dir);
	}

	public static boolean containsTemplatesInDir(String dirName) {
		File dir = new File(dirName);

		if (dir.exists()) {
			String[] temps = getAllFileNames(dir, ALL_EXTS);
			if (temps.length > 0)
				return true;
			else
				return false;
		} else
			return false;
	}

	// public static boolean hasTags(String root) {
	// String dirName = DirUtil.TAGSDIR;
	// return containsTemplateFiles(root, dirName);
	// }

	// public static boolean hasJavaTags(String root) {
	// String dirName = DirUtil.JAVATAGS;
	// return containsTemplateFiles(root, dirName);
	// }
	//
	// public static boolean hasLayouts(String root) {
	// String dirName = DirUtil.LAYOUTDIR;
	// return containsTemplateFiles(root, dirName);
	// }
	//
	public static boolean isClassname(String classname) {
		if (classname == null || classname.length() == 0)
			return false;

		CharacterIterator iter = new StringCharacterIterator(classname);
		// Check first character (there should at least be one character for
		// each part) ...
		char c = iter.first();
		if (c == CharacterIterator.DONE)
			return false;
		if (!Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c))
			return false;
		c = iter.next();
		// Check the remaining characters, if there are any ...
		while (c != CharacterIterator.DONE) {
			if (!Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c))
				return false;
			c = iter.next();
		}
		return true;
	}

	// public static final String JAVATAGS = "_javatags";
	// public static final String LAYOUTDIR = "_layouts";
	// public static final String TAGSDIR = "_tags";
	public static final String JAPIDVIEWS_ROOT = "japidviews";

	// public static List<String> scanJavaTags(String root) {
	// String sep = File.separator;
	// String japidViews = root + sep + JAPIDVIEWS_ROOT + sep;
	// File javatags = new File(japidViews + JAVATAGS);
	// if (!javatags.exists()) {
	// boolean mkdirs = javatags.mkdirs();
	// assert mkdirs == true;
	// JapidFlags.log("created: " + japidViews + JAVATAGS);
	// }
	//
	// File[] javafiles = javatags.listFiles(new FilenameFilter() {
	// @Override
	// public boolean accept(File dir, String name) {
	// if (name.endsWith(".java"))
	// return true;
	// return false;
	// }
	// });
	//
	// List<String> files = new ArrayList<String>();
	// for (File f : javafiles) {
	// String fname = f.getName();
	// files.add(JAPIDVIEWS_ROOT + "." + JAVATAGS + "." + fname.substring(0,
	// fname.lastIndexOf(".java")));
	// }
	// return files;
	// }

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param f
	 *            the java file
	 * @return the original template file
	 */
	public static File mapJavatoSrc(File f) {
		File parent = f.getParentFile();
		String fname = mapJavaToSrc(f.getName());
		return new File(parent, fname);
	}

	/**
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param sourceCode
	 * @param lineNum
	 *            1-based line number in Java file
	 * @return 1-based line number in the original source template
	 */
	public static int mapJavaLineToSrcLine(String sourceCode, int lineNum) {
		String[] codeLines = sourceCode.split("\n");
		String line = codeLines[lineNum - 1];

		int lineMarker = line.lastIndexOf(LINE_MARKER);
		if (lineMarker < 1) {
			return -1;
		}
		String linenum = line.substring(lineMarker + LINE_MARKER.length()).trim();
		int indexOf = linenum.indexOf(OF);
		if (indexOf > 0) {
			linenum = linenum.substring(0, indexOf);
		}
		return Integer.parseInt(linenum);
	}

	/**
	 * 
	 */
	static final String ERRORS = "_errors";
	/**
	 * 
	 */
	static final String NOTIFIERS = "_notifiers";

	/**
	 * a utility method.
	 * 
	 * @param srcDir
	 * @param cf
	 * @throws IOException
	 */
	public static String getRelativePath(File child, File parent) throws IOException {
		String curPath = parent.getCanonicalPath();
		String childPath = child.getCanonicalPath();
		assert (childPath.startsWith(curPath));
		String srcRelative = childPath.substring(curPath.length());
		if (srcRelative.startsWith(File.separator)) {
			srcRelative = srcRelative.substring(File.separator.length());
		}
		return srcRelative;
	}

	public static String readFileAsString(String filePath) throws Exception {
		return readFileAsString(new File(filePath));
	}

	public static String readFileAsString(File file) throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		byte[] buffer = new byte[(int) file.length()];
		BufferedInputStream f = new BufferedInputStream(new FileInputStream(file));
		f.read(buffer);
		f.close();
		return new String(buffer, "UTF-8");
	}

	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[4096];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	public static void copyStreamClose(InputStream in, OutputStream out) throws IOException {
		copyStream(in, out);
		in.close();
		out.close();
	}

	/**
	 * a/b.html -> a.b a/b.xml -> a.b_xml
	 * 
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param japidScriptFileName
	 * @return
	 */
	public static String deriveClassName(String japidScriptFileName) {
		// remove the leading slash
		if (japidScriptFileName.startsWith("/") || japidScriptFileName.startsWith("\\"))
			japidScriptFileName = japidScriptFileName.substring(1);
		if (japidScriptFileName.startsWith("."))
			japidScriptFileName = japidScriptFileName.substring(1);
		japidScriptFileName = mapSrcToJava(japidScriptFileName);
		japidScriptFileName = japidScriptFileName.replace('/', '.').replace('\\', '.');
		if (japidScriptFileName.endsWith(".java")) {
			japidScriptFileName = japidScriptFileName.substring(0, japidScriptFileName.length() - 5);
		}
		return japidScriptFileName;
	}

	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param templateRoots
	 * @return
	 */
	public static Set<File> getAllTemplateFiles(String[] templateRoots) {
		Set<File> re = new HashSet<File>();
		for (String r : templateRoots) {
			File root = new File(r);
			if (root.exists())
				re.addAll(getAllTemplateFiles(root));
		}
		return re;
	}

	/**
	 * get all the java files in a dir with the "java" removed
	 * 
	 * @return
	 */
	public static String[] getAllJavaFilesInDir(File root) {
		// from source files only
		String[] allFiles = getAllFileNames(root, new String[] { ".java" });
		for (int i=0; i< allFiles.length; i++) {
			allFiles[i] = allFiles[i].replace(".java", "");
		}
		return allFiles;
	}

	public static File writeToFile(String jsrc, String realTargetFile) throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		File f = new File(realTargetFile);
		if (!f.exists()) {
			String parent = f.getParent();
			new File(parent).mkdirs();
		}
		BufferedOutputStream bf = new BufferedOutputStream(new FileOutputStream(f));
		bf.write(jsrc.getBytes("UTF-8"));
		bf.close();
		return f;
	}

}
