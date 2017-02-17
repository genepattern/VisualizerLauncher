package org.genepattern.desktop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by nazaire on 4/6/16.
 */
public class Util
{
    public static File downloadFile(URL url, File dir, String filename) throws IOException
    {
        return downloadFile(url, dir, filename, null);
    }

    /**
     * Download a URL to a local file and return a File object for it.
     *
     * @param url, The url to download.
     * @param dir, The directory to download the URL to.
     * @param filename, The filename to download the URL to.
     */
    public static File downloadFile(URL url, File dir, String filename, String authString) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Authorization", authString);

            is = conn.getInputStream();
            dir.mkdirs();
            file = new File(dir, filename);
            fos = new FileOutputStream(file);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                }
            }
        }
        return file;
    }
}
