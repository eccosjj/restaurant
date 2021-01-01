package restaurant.log;

import java.io.File;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.ErrorCode;

/**
 * This is a customized log4j appender, which will only accept the Threshold log
 * in log4j.properties, and also generate new log file every time the
 * application is running
 *
 * @author Junjie Sun
 *
 */
public class MyFileAppender extends FileAppender {

    static final String newLogFolder = String.valueOf(System.currentTimeMillis());;

    /**
     * when the log is activated, it call the getNewLogFileName to generate new log
     * file every time the application is running
     */
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

    /**
     * generate new log file every time the application is running
     */
    private String getNewLogFileName() {
        if (fileName != null) {
            final File logFile = new File(fileName);
            final String fileName = logFile.getName();
            return logFile.getParent() + File.separator + newLogFolder + File.separator + fileName;
        }
        return String.valueOf(newLogFolder);
    }

    /**
     * only accept the Threshold log configuration in log4j.properties
     */
    @Override
    public boolean isAsSevereAsThreshold(Priority priority) {
        return this.getThreshold().equals(priority);
    }

}
