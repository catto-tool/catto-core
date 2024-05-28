package CATTO.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows programs to modify the classpath during runtime.
 */
public class ClassPathUpdater {
    /** Used to find the method signature. */
    private static final Class[] PARAMETERS = new Class[]{ URL.class };

    /** Class containing the private addURL method. */
    private static final Class<?> CLASS_LOADER = URLClassLoader.class;

    private static final Logger LOGGER = Logger.getLogger(ClassPathUpdater.class);


    private ClassPathUpdater(){

    }

    /**
     * Adds a new paths to the classloader. If the given string points to a file,
     * then that file's parent file (i.e., directory) is used as the
     * directory to add to the classpath. If the given string represents a
     * directory, then the directory is directly added to the classpath.
     *
     * @param paths paths to add at Classpath
     */
    public static List<Class> add(List<String> paths, ClassLoader classLoader)
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        List<Class> loadedClass = new ArrayList<>();
        for (String path : paths) {
            loadedClass.addAll(add(new File(path + File.separator), null, classLoader));
        }
        return loadedClass;
    }

    /**
     * Adds a new path to the classloader. If the given file object is
     * a file, then its parent file (i.e., directory) is used as the directory
     * to add to the classpath. If the given string represents a directory,
     * then the directory it represents is added.
     *
     * @param direcotory The directory (or enclosing directory if a file) to add to the
     *                   classpath.
     * @param pakage
     */
    public static List<Class> add(File direcotory, File pakage, ClassLoader classLoader)
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        List<Class> loadedClasses = new ArrayList<>();

        if(pakage == null){
            pakage = direcotory;
        }

        File[] packages = pakage.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File pkg: packages){
            File[] classes = pkg.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.toString().endsWith(".class");
                }
            });

            for(File clazz : classes){
                String className = "";

                if(!pakage.getAbsolutePath().equals(direcotory.getAbsolutePath())){
                    className = pakage.getAbsolutePath().replace(direcotory.getAbsolutePath(), "").concat(".");
                    if(className.startsWith(File.separator)){
                        className = className.replace(File.separator, "");
                    }

                }

                className = className.concat(pkg.getName());
                className = className.concat(".").concat(clazz.getName().replace(".class", ""));
                className = className.replace(File.separator, ".");
                Class loadedClass = add1(className, direcotory, classLoader);
                if (loadedClass != null){
                    loadedClasses.add(loadedClass);
                }
            }

            loadedClasses.addAll(add(direcotory, pkg, classLoader));

        }
        return loadedClasses;

    }

    /**
     * Adds a new path to the classloader. The class must point to a directory,
     * not a file.
     *
     * @param f The path to include when searching the classpath.
     */
    public static Class add1( String className, File directory, ClassLoader classLoader)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, MalformedURLException {
        //Method method = CLASS_LOADER.getDeclaredMethod( "addURL", PARAMETERS );
        //method.setAccessible( true );
        //method.invoke( getClassLoader(), url);
        try {
            // Creating an instance of URLClassloader using the above URL and parent classloader
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{directory.toURI().toURL()}, classLoader);
           return loader.loadClass(className);
        }catch (ClassNotFoundException | NoClassDefFoundError e){
           System.out.println("Impossible to load class:" + className);
        }

        return null;


    }

    private static URLClassLoader getClassLoader() {
        return (URLClassLoader)ClassLoader.getSystemClassLoader();
    }

    /**
     * Add dinamycally a list of jar to the library path
     * @param jarFiles one or more string contains the path of the jar to add
     */
    public static void addJar(String... jarFiles ) throws NoSuchMethodException {
        // Get the ClassLoader class
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Class<?> clazz = cl.getClass();

        // Get the protected addURL method from the parent URLClassLoader class
        Method method = null;

        method = clazz.getSuperclass().getDeclaredMethod("addURL", URL.class);

        for(String s : jarFiles){
            File jar = new File(s);
        // Run projected addURL method to add JAR to classpath
        method.setAccessible(true);
            try {
                method.invoke(cl, jar.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
                LOGGER.error(e);
            }
        }
    }

}