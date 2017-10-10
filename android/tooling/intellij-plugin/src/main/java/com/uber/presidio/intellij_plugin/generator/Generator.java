package com.uber.presidio.intellij_plugin.generator;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import org.apache.commons.lang.text.StrSubstitutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Base class for generating riblet classes.
 *
 * <p>Templates are tokenized using a ${token_name} syntax. {@code package_name} and {@code
 * riblet_name} token values are provided by the base generate - however subclasses can add custom
 * paramaters when needed using {@link Generator#getTemplateValuesMap()}.
 */
public abstract class Generator {

  private static final String TEMPLATE_TOKEN_PACKAGE_NAME = "package_name";
  private static final String TEMPLATE_TOKEN_RIBLET_NAME = "riblet_name";
  private static final String TEMPLATE_TOKEN_RIBLET_NAME_TO_LOWER = "riblet_name_to_lower";

  private final String packageName;
  private final String ribletName;
  private final String templateString;
  private final Map<String, String> templateValuesMap;

  /**
   * @param packageName riblet package name.
   * @param ribletName riblet name.
   * @param templateName template to be used by this generate.
   */
  public Generator(String packageName, String ribletName, String templateName) {
    this.packageName = packageName;
    this.ribletName = ribletName;

    templateValuesMap = new HashMap<String, String>();
    templateValuesMap.put(TEMPLATE_TOKEN_PACKAGE_NAME, packageName);
    templateValuesMap.put(TEMPLATE_TOKEN_RIBLET_NAME, ribletName);
    templateValuesMap.put(TEMPLATE_TOKEN_RIBLET_NAME_TO_LOWER, ribletName.toLowerCase());
    try {
      String[] resources = getResourceListing(this.getClass(), "partials/");
      for (String resourceName : resources) {
        if (resourceName == null || resourceName.length() == 0) {
          continue;
        }
        InputStream resourceAsStream =
            Generator.class.getResourceAsStream("/partials/" + resourceName);
        String resourceContents =
            Preconditions.checkNotNull(
                CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8)));
        templateValuesMap.put(String.format("partial: %s", resourceName), resourceContents);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    try {
      // Need to use getResourceAsStream() since we may be reading resources from inside a jar.
      // Class.getResource() doesn't work in this scenario.
      InputStream resourceAsStream1 =
          Generator.class.getResourceAsStream("/templates/" + templateName);
      templateString =
          Preconditions.checkNotNull(
              CharStreams.toString(new InputStreamReader(resourceAsStream1, Charsets.UTF_8)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** @return the class name for the generated file. */
  public abstract String getClassName();

  /** @return the package name for the generated file. */
  public final String getPackageName() {
    return packageName;
  }

  /** @return the riblet name for the generator. */
  public final String getRibletName() {
    return ribletName;
  }

  /** @return the template values map, to add more template paramters. */
  protected final Map<String, String> getTemplateValuesMap() {
    return templateValuesMap;
  }

  /** @return the source for the generated file. */
  public final String generate() {
    StrSubstitutor substitutor = new StrSubstitutor(templateValuesMap);
    String newFile = substitutor.replace(templateString);
    System.out.println(newFile);
    return newFile;
  }

  /**
   * List directory contents for a resource folder. Not recursive. This is basically a brute-force
   * implementation. Works for regular files and also JARs. Found at
   * http://stackoverflow.com/questions/6247144/how-to-load-a-folder-from-a-jar.
   *
   * @param clazz Any java class that lives in the same place as the resources you want.
   * @param path Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException
   * @throws IOException
   */
  String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
    URL dirURL = clazz.getClassLoader().getResource(path);
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      // A file path: easy enough
      return new File(dirURL.toURI()).list();
    }

    if (dirURL == null) {
      // In case of a jar file, we can't actually find a directory. Have to assume the same jar as
      // clazz.
      String me = clazz.getName().replace(".", "/") + ".class";
      dirURL = clazz.getClassLoader().getResource(me);
    }

    if (dirURL.getProtocol().equals("jar")) {
      // Strip out only the JAR file.
      String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));

      // Gives ALL entries in jar and avoids duplicates.
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
      Enumeration<JarEntry> entries = jar.entries();
      Set<String> result = new HashSet<String>();
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(path)) { // filter according to the path
          String entry = name.substring(path.length());
          int checkSubdir = entry.indexOf("/");
          if (checkSubdir >= 0) {
            // if it is a subdirectory, we just return the directory name
            entry = entry.substring(0, checkSubdir);
          }
          result.add(entry);
        }
      }
      return result.toArray(new String[result.size()]);
    }
    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }
}
