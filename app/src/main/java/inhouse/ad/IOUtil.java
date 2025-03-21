package inhouse.ad;

import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {

    public static void copyStream(InputStream is, OutputStream os) {
        final int buffer_size = 4096;
        try {
            byte[] bytes = new byte[buffer_size];
            for (int count=0,prog=0;count!=-1;) {
                count = is.read(bytes);
                if(count != -1) {
                    os.write(bytes, 0, count);
                    prog=prog+count;
                }
            }
            os.flush();
            is.close();
            os.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
