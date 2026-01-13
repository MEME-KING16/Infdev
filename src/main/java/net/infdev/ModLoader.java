package net.infdev;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ModLoader {

    private static int modsLoaded = 0;
    
    public static void runMods(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Directory not found or is not a directory: " + directoryPath);
            return;
        }

        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("No mods found in directory: " + directoryPath);
            return;
        }

        List<URL> urls = new ArrayList<>();
        try {
            for (File file : jarFiles) {
                urls.add(file.toURI().toURL());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

        for (File file : jarFiles) {
            try (JarFile jarFile = new JarFile(file)) {
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    String mainClassName = attributes.getValue(Attributes.Name.MAIN_CLASS);
                    if (mainClassName != null) {
                        System.out.println("Found mod " + file.getName() + ": " + mainClassName);
                        modsLoaded++;
                        
                        Class<?> mainClass = classLoader.loadClass(mainClassName);
                        
                        Method mainMethod = mainClass.getMethod("main", String[].class);
                        
                        System.out.println("Running main method for " + mainClassName);
                        mainMethod.invoke(null, (Object) new String[]{}); 
                        System.out.println("Finished running main method for " + mainClassName + "\n");

                    } else {
                        System.out.println("No Main-Class attribute found in Manifest of " + file.getName() + "\n");
                    }
                } else {
                    System.out.println("No Manifest file found in " + file.getName() + "\n");
                }
            } catch (Exception e) {
                System.err.println("Error loading mod file " + file.getName() + ": " + e.getMessage() + "\n");
                // e.printStackTrace();
            }
        }

        try {
            if (classLoader != null) {
                classLoader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getModsLoaded() {
        return modsLoaded;
    }
}