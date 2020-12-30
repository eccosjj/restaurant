package restaurant.log;

import java.io.File;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.ErrorCode;

/**
 * This is a customized log4j appender, which will create a new file for every
 * run of the application.
 *
 * @author veera | https://veerasundar.com
 *
 */
public class MyFileAppender extends FileAppender {

    static final String newLogFolder = String.valueOf(System.currentTimeMillis());;

    @Override
    public boolean isAsSevereAsThreshold(Priority priority) {
        return this.getThreshold().equals(priority);
    }

    public void activateOptions() {
        if (fileName != null) {
            try {
                fileName = getNewLogFileName();
                setFile(fileName, fileAppend, bufferedIO, bufferSize);
            } catch (Exception e) {
                errorHandler.error("Error while activating log options", e, ErrorCode.FILE_OPEN_FAILURE);
            }
        }
    }

    // Use the time stamp as the folder of the new log files.
    private String getNewLogFileName() {
        if (fileName != null) {
            final File logFile = new File(fileName);
            final String fileName = logFile.getName();
            return logFile.getParent() + File.separator + newLogFolder + File.separator + fileName;
        }
        return String.valueOf(newLogFolder);
    }
}
