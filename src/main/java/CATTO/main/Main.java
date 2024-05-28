package CATTO.main;



import CATTO.code.analyzer.CodeAnalyzer;
import CATTO.config.ConfigWrapper;
import CATTO.config.Configurator;
import CATTO.test.runner.Runner;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.BasicConfigurator;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.selector.TestSelector;
import CATTO.test.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main {

    public static void main (String[] args) throws InvalidTargetPaths, NoTestFoundedException, IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        //no error
        int exit_code = 0;


        BasicConfigurator.configure();

        ConfigWrapper ini = new ConfigWrapper(args[0]);
        Configurator configurator = ini.getCONFIG();
        String tempFolder = configurator.getTempFolderPath();

        //discover .jar in lib folder
        List<String> libraryNames = new ArrayList<>();
        for (String path : configurator.getDependencies()) {
            if(new File(path).isDirectory()){
                List<File> file = (List<File>) FileUtils.listFiles(new File(path), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                for (File f : file) {

                    libraryNames.addAll(listf(f.getAbsolutePath()));
                }
            }else{
                libraryNames.add(new File(path).getAbsolutePath());
            }
        }

        //instantiate two version project object
        PreviousProject p = new PreviousProject(libraryNames.toArray(new String[0]), Paths.get(args[0], tempFolder).toString());
        NewProject p1 = new NewProject(libraryNames.toArray(new String[0]), configurator.getOutputPath().toArray(new String[0]));
        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(p1, p);
        codeAnalyzer.analyze();
        TestSelector rta = new TestSelector(p1,codeAnalyzer.getDifferentMethods() ,codeAnalyzer.getDifferentTest(),codeAnalyzer.getNewMethods() ,codeAnalyzer.getDifferentObject());
        Set<Test> selectedTest = rta.selectTest();

        //no test found
        if(selectedTest.isEmpty())
            exit_code = 2;


        for (Test t : selectedTest ){
            TestExecutionSummary testExecutionSummary = Runner.run(t, configurator.getDependencies().toArray(new String[0]), configurator.getOutputPath());
            if (testExecutionSummary.getTestsFailedCount() > 0){
                //failures in test
                exit_code = -1;
            }
        }




        System.exit(exit_code);


    }

    public static List<String> listf(String directoryName) {
        File directory = new File(directoryName);
        List<String> files = new ArrayList<>();

        // Get all files from a directory.
        if (directory.isFile()) {
            if (directory.getName().endsWith(".jar"))
                files.add(directory.getAbsolutePath());
        }
        else {
            File[] fList = directory.listFiles();
            if (fList != null)
                for (File file : fList) {
                    if (file.isFile()) {
                        if(file.getName().endsWith(".jar"))
                            files.add(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        files.addAll(listf(file.getAbsolutePath()));
                    }
                }

        }
        return files;
    }

}