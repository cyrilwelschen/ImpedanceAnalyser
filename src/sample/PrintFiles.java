package sample;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static java.nio.file.FileVisitResult.*;

public class PrintFiles extends SimpleFileVisitor<Path> {

    public List<Path> pathListToReturn;
    private Path MYBASE;

    PrintFiles(List<Path> pathList, Path workingDirectory) {
        this.pathListToReturn = pathList;
        this.MYBASE = workingDirectory;
    }

    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile() && file.toString().endsWith(".csv")) {
            File fileFile = file.toFile();
            //this.pathListToReturn.add(this.MYBASE.relativize(file));
            if (!(fileFile.isHidden())) {
                this.pathListToReturn.add(file);
            }
        }
        return CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        //this.pathListToReturn.add(this.MYBASE.relativize(dir));
        this.pathListToReturn.add(dir);
        return CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file,
                                           IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
}
